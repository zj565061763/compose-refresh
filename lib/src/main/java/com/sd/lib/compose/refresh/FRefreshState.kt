package com.sd.lib.compose.refresh

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.math.absoluteValue

interface FRefreshState {
    /** 刷新方向，该值不会改变 */
    val refreshDirection: RefreshDirection

    /** 是否刷新中[RefreshInteraction.Refreshing]或者即将刷新[RefreshInteraction.FlingToRefresh] */
    val isRefreshing: Boolean

    /** 当前互动状态 */
    val currentInteraction: RefreshInteraction

    /** 互动状态 */
    val interactionState: RefreshInteractionState

    /** 嵌套滚动对象，外部需要把此对象传给[Modifier.nestedScroll] */
    val nestedScrollConnection: NestedScrollConnection

    /** 当前偏移量 */
    val offset: Float

    /** 拖动距离 */
    @get:FloatRange(from = 0.0)
    val progress: Float

    /** 可以触发刷新的距离 */
    @get:FloatRange(from = 0.0)
    val refreshThreshold: Float

    /** 刷新中状态的距离 */
    @get:FloatRange(from = 0.0)
    val refreshingDistance: Float

    /** 是否已经达到可以触发刷新的距离 */
    val reachRefreshThreshold: Boolean

    /** 容器大小 */
    val containerSize: IntSize?

    /**
     * 显示刷新状态
     */
    fun showRefresh()

    /**
     * 隐藏刷新状态
     */
    fun hideRefresh()

    /**
     * 设置[refreshThreshold]
     */
    fun setRefreshThreshold(value: Float?)

    /**
     * 设置[refreshingDistance]
     */
    fun setRefreshingDistance(value: Float?)

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

    /** 刷新中 */
    Refreshing,

    /** 滑向刷新的位置 */
    FlingToRefresh,

    /** 滑向原始的位置 */
    FlingToNone,
}

data class RefreshInteractionState(
    /** 当前互动状态 */
    val current: RefreshInteraction = RefreshInteraction.None,

    /** 上一次互动状态 */
    val previous: RefreshInteraction = RefreshInteraction.None,
)

internal class RefreshStateImpl(
    private val coroutineScope: CoroutineScope,
    override val refreshDirection: RefreshDirection,
    enabled: () -> Boolean,
) : FRefreshState {
    private val _dispatcher = Dispatchers.Main.immediate

    override val isRefreshing: Boolean by derivedStateOf { iRefreshing() }
    override val currentInteraction: RefreshInteraction by derivedStateOf { iGetCurrentInteraction() }
    override val interactionState: RefreshInteractionState get() = _interactionState

    override val offset: Float get() = _offsetState
    override val progress: Float by derivedStateOf { iGetProgress() }
    override val refreshThreshold: Float by derivedStateOf { iGetRefreshThreshold() }
    override val refreshingDistance: Float by derivedStateOf { iGetRefreshingDistance() }
    override val reachRefreshThreshold: Boolean by derivedStateOf { iReachRefreshThreshold() }
    override val containerSize: IntSize? get() = _containerSizeState

    /** 互动状态 */
    private var _interactionState by mutableStateOf(RefreshInteractionState())
    /** 当前偏移量 */
    private var _offsetState by mutableFloatStateOf(0f)
    /** 容器大小 */
    private var _containerSizeState by mutableStateOf<IntSize?>(null)
    /** 可以刷新的距离 */
    private var _refreshThresholdState by mutableStateOf<Float?>(null)
    /** 刷新中状态的距离 */
    private var _refreshingDistanceState by mutableStateOf<Float?>(null)

    private var _notifyCallbackJob: Job? = null
    private var _onRefreshCallback: (() -> Unit)? = null

    private var _internalOffset = 0f
    private val _animOffset = Animatable(0f)

    private val _hideRefreshingCallbacks: MutableSet<suspend () -> Unit> = Collections.synchronizedSet(mutableSetOf())

    private val _directionHandler = DirectionHandler(
        refreshDirection = refreshDirection,
        onScroll = { handleScroll(it) },
        onFling = {
            withContext(_dispatcher) {
                handleFling(it)
            }
        },
    )

    override fun showRefresh() {
        coroutineScope.launch(_dispatcher) {
            animateToRefreshing()
        }
    }

    override fun hideRefresh() {
        coroutineScope.launch(_dispatcher) {
            cancelNotifyCallbackJob()
            if (iRefreshing()) {
                _hideRefreshingCallbacks.toTypedArray().forEach {
                    it.invoke()
                }
            }
            animateToReset()
        }
    }

    override fun setRefreshThreshold(value: Float?) {
        _refreshThresholdState = value
    }

    override fun setRefreshingDistance(value: Float?) {
        _refreshingDistanceState = value
    }

    override fun registerHideRefreshing(callback: suspend () -> Unit) {
        _hideRefreshingCallbacks.add(callback)
    }

    override fun unregisterHideRefreshing(callback: suspend () -> Unit) {
        _hideRefreshingCallbacks.remove(callback)
    }

    internal fun setContainerSize(size: IntSize) {
        _containerSizeState = size
    }

    internal fun setRefreshCallback(callback: () -> Unit) {
        _onRefreshCallback = callback
    }

    private fun handleScroll(available: Float): Float? {
        val maxDragDistance = iGetMaxDragDistance()
        if (maxDragDistance <= 0) {
            updateOffset(0f)
            setRefreshInteraction(RefreshInteraction.None)
            return null
        }

        when (iGetCurrentInteraction()) {
            RefreshInteraction.None, RefreshInteraction.Drag -> {
                val offset = transformOffset(available, maxDragDistance)
                return updateOffset(_internalOffset + offset) { newOffset ->
                    when (newOffset) {
                        0f -> {
                            if (iGetCurrentInteraction() == RefreshInteraction.Drag) {
                                setRefreshInteraction(RefreshInteraction.None)
                            }
                        }
                        else -> {
                            if (iGetCurrentInteraction() == RefreshInteraction.None) {
                                setRefreshInteraction(RefreshInteraction.Drag)
                            }
                        }
                    }
                }.let { updated ->
                    if (updated) available else null
                }
            }
            else -> return null
        }
    }

    private fun transformOffset(
        available: Float,
        maxDragDistance: Float,
    ): Float {
        require(maxDragDistance > 0)
        val currentProgress = (_internalOffset / maxDragDistance).absoluteValue.coerceIn(0f, 1f)
        val multiplier = (1f - currentProgress).coerceIn(0f, 0.6f)
        return available * multiplier
    }

    private suspend fun handleFling(available: Float): Float? {
        if (iGetCurrentInteraction() == RefreshInteraction.Drag) {
            cancelNotifyCallbackJob()
            if (iReachRefreshThreshold()) {
                animateToRefreshing()
                _notifyCallbackJob = currentCoroutineContext()[Job]
                _onRefreshCallback?.invoke()
                delay(100)
            }
            animateToReset()
            return available
        }
        return null
    }

    private fun cancelNotifyCallbackJob() {
        _notifyCallbackJob?.cancel()
    }

    private suspend fun animateToRefreshing() {
        animateToOffset(iGetRefreshingOffset(), RefreshInteraction.Refreshing)
    }

    private suspend fun animateToReset() {
        animateToOffset(0f, RefreshInteraction.None)
    }

    private suspend fun animateToOffset(
        offset: Float,
        flingEnd: RefreshInteraction,
    ) {
        val flingInteraction = when (flingEnd) {
            RefreshInteraction.None -> RefreshInteraction.FlingToNone
            RefreshInteraction.Refreshing -> RefreshInteraction.FlingToRefresh
            else -> error("Illegal flingEnd:$flingEnd")
        }

        if (iGetCurrentInteraction() == flingEnd) {
            if (_animOffset.isRunning) {
                if (_animOffset.targetValue == offset) return
            } else {
                if (_internalOffset == offset) return
            }
        }

        currentCoroutineContext().ensureActive()
        setRefreshInteraction(flingInteraction)

        _animOffset.snapTo(_internalOffset)
        _animOffset.animateTo(offset) { updateOffset(value) }

        setRefreshInteraction(flingEnd)
    }

    private inline fun updateOffset(
        offset: Float,
        onChange: (Float) -> Unit = {},
    ): Boolean {
        val newOffset = when (refreshDirection) {
            RefreshDirection.Top, RefreshDirection.Left -> offset.coerceAtLeast(0f)
            RefreshDirection.Bottom, RefreshDirection.Right -> offset.coerceAtMost(0f)
        }

        return if (_internalOffset != newOffset) {
            _internalOffset = newOffset
            onChange(newOffset)
            _offsetState = newOffset
            true
        } else {
            false
        }
    }

    private fun setRefreshInteraction(current: RefreshInteraction) {
        val state = _interactionState
        if (state.current == current) return
        _interactionState = state.copy(
            previous = state.current,
            current = current,
        )
    }

    //-------------------- internal getter --------------------

    private fun iRefreshing(): Boolean {
        return _interactionState.current.let {
            it == RefreshInteraction.Refreshing
                    || it == RefreshInteraction.FlingToRefresh
        }
    }

    private fun iGetCurrentInteraction(): RefreshInteraction {
        return _interactionState.current
    }

    private fun iGetProgress(): Float {
        return iGetRefreshThreshold().let { threshold ->
            if (threshold > 0) {
                (_offsetState / threshold).absoluteValue.coerceAtLeast(0f)
            } else {
                0f
            }
        }
    }

    private fun iReachRefreshThreshold(): Boolean {
        return iGetRefreshThreshold().let { threshold ->
            threshold > 0 && _offsetState.absoluteValue >= threshold
        }
    }

    private fun iGetRefreshingOffset(): Float {
        val distance = iGetRefreshingDistance()
        return when (refreshDirection) {
            RefreshDirection.Top, RefreshDirection.Left -> distance
            RefreshDirection.Bottom, RefreshDirection.Right -> -distance
        }
    }

    private fun iGetMaxDragDistance(): Float {
        return maxOf(iGetRefreshThreshold(), iGetRefreshingDistance()) * 3
    }

    private fun iGetRefreshThreshold(): Float {
        return _refreshThresholdState ?: iGetContainerSize().toFloat()
    }

    private fun iGetRefreshingDistance(): Float {
        return _refreshingDistanceState ?: iGetContainerSize().toFloat()
    }

    private fun iGetContainerSize(): Int {
        return when (refreshDirection) {
            RefreshDirection.Top, RefreshDirection.Bottom -> _containerSizeState?.height ?: 0
            RefreshDirection.Left, RefreshDirection.Right -> _containerSizeState?.width ?: 0
        }
    }

    override val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            return when {
                !enabled() -> Offset.Zero
                source == NestedScrollSource.Drag -> _directionHandler.handlePreScroll(available)
                else -> Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            return when {
                !enabled() -> Offset.Zero
                source == NestedScrollSource.Drag -> _directionHandler.handlePostScroll(available)
                else -> Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            return _directionHandler.handlePreFling(available)
        }
    }

    init {
        coroutineScope.launch(_dispatcher) {
            snapshotFlow { refreshingDistance }
                .filter { it > 0 }
                .distinctUntilChanged()
                .collect {
                    if (iRefreshing()) {
                        animateToRefreshing()
                    }
                }
        }
    }
}