package com.sd.lib.compose.refresh

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.math.absoluteValue

interface FRefreshState {
   /** 嵌套滚动对象，外部需要把此对象传给[Modifier.nestedScroll] */
   val nestedScrollConnection: NestedScrollConnection

   @get:FloatRange(from = 0.0)
   val progress: Float

   /** 当前互动状态 */
   val currentInteraction: RefreshInteraction

   /** 互动状态 */
   val interactionState: RefreshInteractionState

   /** 刷新方向 */
   val refreshDirection: RefreshDirection

   /**
    * 显示刷新状态
    */
   fun showRefresh()

   /**
    * 隐藏刷新状态
    */
   fun hideRefresh()

   /**
    * 设置可以触发刷新的距离
    */
   fun setRefreshThreshold(value: Float)

   /**
    * 注册隐藏刷新回调
    */
   fun registerHideRefreshing(callback: suspend () -> Unit)

   /**
    * 取消注册隐藏刷新回调
    */
   fun unregisterHideRefreshing(callback: suspend () -> Unit)
}

enum class RefreshDirection {
   Top, Bottom, Left, Right,
}

enum class RefreshInteraction {
   /** 原始 */
   None,

   /** 拖动 */
   Drag,

   /** 正在滑向刷新状态的位置 */
   FlingToRefresh,

   /** 刷新中 */
   Refreshing,

   /** 滑向原始的位置 */
   FlingToNone,
}

data class RefreshInteractionState(
   /** 当前互动状态 */
   val current: RefreshInteraction = RefreshInteraction.None,

   /** 上一个互动状态 */
   val previous: RefreshInteraction = RefreshInteraction.None,
)

internal class RefreshStateImpl(
   override val refreshDirection: RefreshDirection,
   private val coroutineScope: CoroutineScope,
) : FRefreshState {
   private var _enabled = false
   private val _dispatcher = runCatching { Dispatchers.Main.immediate }.getOrDefault(Dispatchers.Main)

   override val progress: Float get() = _progressState
   override val currentInteraction: RefreshInteraction get() = _interactionState.current
   override val interactionState: RefreshInteractionState get() = _interactionState

   private val _anim = Animatable(0f)
   private var _offset = 0f
   private var _refreshThreshold = 0f

   private var _progressState by mutableFloatStateOf(0f)
   private var _interactionState by mutableStateOf(RefreshInteractionState())

   private var _onRefreshCallback: (() -> Unit)? = null
   private val _hideRefreshingCallbacks: MutableSet<suspend () -> Unit> = Collections.synchronizedSet(mutableSetOf())

   override fun showRefresh() {
      coroutineScope.launch(_dispatcher) {
         if (currentInteraction != RefreshInteraction.Refreshing) {
            animateToRefresh()
            setRefreshInteraction(RefreshInteraction.Refreshing)
         }
      }
   }

   override fun hideRefresh() {
      coroutineScope.launch(_dispatcher) {
         // TODO review cancellation logic
         if (currentInteraction == RefreshInteraction.Refreshing) {
            _hideRefreshingCallbacks.toTypedArray().forEach {
               it.invoke()
            }
         }
         animateToReset()
      }
   }

   override fun setRefreshThreshold(value: Float) {
      _refreshThreshold = value.coerceAtLeast(0f)
   }

   override fun registerHideRefreshing(callback: suspend () -> Unit) {
      _hideRefreshingCallbacks.add(callback)
   }

   override fun unregisterHideRefreshing(callback: suspend () -> Unit) {
      _hideRefreshingCallbacks.remove(callback)
   }

   internal fun setEnabled(enabled: Boolean) {
      _enabled = enabled
   }

   internal fun setRefreshCallback(callback: () -> Unit) {
      _onRefreshCallback = callback
   }

   private val _directionHandler = DirectionHandler(
      refreshDirection = refreshDirection,
      onPreScroll = { onPreScroll(it) },
      onPostScroll = { onPostScroll(it) },
      onPreFling = {
         withContext(_dispatcher) {
            onPreFling(it)
         }
      },
   )

   private fun onPreScroll(available: Float): Float? {
      if (currentInteraction == RefreshInteraction.Drag) {
         consumeAvailableOffset(available)
         return available
      }
      return null
   }

   private fun onPostScroll(available: Float): Float? {
      if (currentInteraction == RefreshInteraction.None) {
         val threshold = getThreshold() ?: return null
         val newOffset = calculateNewOffset(available, threshold)
         if (newOffset != 0f) {
            setRefreshInteraction(RefreshInteraction.Drag)
            _offset = newOffset
            _progressState = (newOffset / threshold).absoluteValue
            return available
         }
      }
      return null
   }

   private fun consumeAvailableOffset(available: Float): Float? {
      check(currentInteraction == RefreshInteraction.Drag)
      val threshold = getThreshold() ?: return null
      val newOffset = calculateNewOffset(available, threshold)

      val consumed = newOffset - _offset
      _offset = newOffset
      _progressState = (newOffset / threshold).absoluteValue

      if (newOffset == 0f) {
         setRefreshInteraction(RefreshInteraction.None)
      }
      return consumed
   }

   private suspend fun onPreFling(available: Float): Float? {
      if (_progressState >= 1f) {
         animateToRefresh()
         _onRefreshCallback?.invoke()
      } else {
         animateToReset()
      }
      // TODO review consumed
      return available
   }

   private suspend fun animateToRefresh() {
      if (_progressState != 1f) {
         setRefreshInteraction(RefreshInteraction.FlingToRefresh)
      }
      animateToProgress(1f)
   }

   private suspend fun animateToReset() {
      if (_progressState != 0f) {
         setRefreshInteraction(RefreshInteraction.FlingToNone)
      }
      animateToProgress(0f)
      setRefreshInteraction(RefreshInteraction.None)
   }

   private suspend fun animateToProgress(progress: Float) {
      _anim.snapTo(_progressState)
      _anim.animateTo(progress) { _progressState = value }
   }

   private fun setRefreshInteraction(current: RefreshInteraction) {
      val state = _interactionState
      if (state.current == current) return

      if (current == RefreshInteraction.None) {
         _offset = 0f
      }

      _interactionState = state.copy(
         previous = state.current,
         current = current,
      )
   }

   private fun getThreshold(): Float? {
      val threshold = _refreshThreshold
      if (threshold > 0) return threshold
      _progressState = 0f
      setRefreshInteraction(RefreshInteraction.None)
      return null
   }

   private fun calculateNewOffset(available: Float, threshold: Float): Float {
      val transform = transformAvailable(available, threshold)
      return (_offset + transform).let { offset ->
         when (refreshDirection) {
            RefreshDirection.Top, RefreshDirection.Left -> offset.coerceAtLeast(0f)
            RefreshDirection.Bottom, RefreshDirection.Right -> offset.coerceAtMost(0f)
         }
      }
   }

   private fun transformAvailable(
      available: Float,
      threshold: Float,
   ): Float {
      require(threshold > 0)
      val maxDragDistance = (threshold * 3f).takeIf { it.isFinite() } ?: Float.MAX_VALUE
      val currentProgress = (_offset / maxDragDistance).absoluteValue.coerceIn(0f, 1f)
      val multiplier = (1f - currentProgress).coerceIn(0f, 0.6f)
      return available * multiplier
   }

   override val nestedScrollConnection = object : NestedScrollConnection {
      override fun onPreScroll(
         available: Offset,
         source: NestedScrollSource,
      ): Offset {
         return if (source == NestedScrollSource.UserInput && handleScroll()) {
            _directionHandler.handlePreScroll(available)
         } else {
            Offset.Zero
         }
      }

      override fun onPostScroll(
         consumed: Offset,
         available: Offset,
         source: NestedScrollSource,
      ): Offset {
         return if (source == NestedScrollSource.UserInput && handleScroll()) {
            _directionHandler.handlePostScroll(available)
         } else {
            Offset.Zero
         }
      }

      override suspend fun onPreFling(available: Velocity): Velocity {
         return if (currentInteraction == RefreshInteraction.Drag) {
            _directionHandler.handlePreFling(available)
         } else {
            Velocity.Zero
         }
      }
   }

   private fun handleScroll(): Boolean {
      return _enabled && currentInteraction.let {
         it == RefreshInteraction.None || it == RefreshInteraction.Drag
      }
   }
}