package com.sd.lib.compose.refresh

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.sd.lib.compose.refresh.indicator.DefaultRefreshIndicator

@SuppressLint("ModifierParameter")
@Composable
fun FRefreshContainer(
   state: FRefreshState,
   isRefreshing: Boolean?,
   modifier: Modifier = Modifier,
   indicator: @Composable (FRefreshState) -> Unit = { DefaultRefreshIndicator(state = state) },
) {
   check(state is RefreshStateImpl)

   var containerSize by remember { mutableStateOf<IntSize?>(null) }

   state.setContainerSize(containerSize)

   if (isRefreshing != null) {
      LaunchedEffect(isRefreshing) {
         if (isRefreshing) {
            state.showRefresh()
         } else {
            state.hideRefresh()
         }
      }
   }

   Box(
      modifier = modifier
         .onSizeChanged {
            containerSize = it
         }
         .graphicsLayer {
            when (state.refreshDirection) {
               RefreshDirection.Top -> translationY = state.offset - size.height
               RefreshDirection.Bottom -> translationY = state.offset + size.height
               RefreshDirection.Start -> translationX = state.offset - size.width
               RefreshDirection.End -> translationX = state.offset + size.width
            }
         },
      contentAlignment = Alignment.Center,
   ) {
      indicator(state)
   }
}

/**
 * 顶部刷新
 */
@Composable
fun rememberFRefreshStateTop(
   enabled: () -> Boolean = { true },
   onRefresh: () -> Unit,
): FRefreshState {
   return rememberFRefreshState(
      refreshDirection = RefreshDirection.Top,
      enabled = enabled,
      onRefresh = onRefresh,
   )
}

/**
 * 底部刷新
 */
@Composable
fun rememberFRefreshStateBottom(
   enabled: () -> Boolean = { true },
   onRefresh: () -> Unit,
): FRefreshState {
   return rememberFRefreshState(
      refreshDirection = RefreshDirection.Bottom,
      enabled = enabled,
      onRefresh = onRefresh,
   )
}

/**
 * 左侧刷新
 */
@Composable
fun rememberFRefreshStateLeft(
   enabled: () -> Boolean = { true },
   onRefresh: () -> Unit,
): FRefreshState {
   return rememberFRefreshState(
      refreshDirection = RefreshDirection.Start,
      enabled = enabled,
      onRefresh = onRefresh,
   )
}

/**
 * 右侧刷新
 */
@Composable
fun rememberFRefreshStateRight(
   enabled: () -> Boolean = { true },
   onRefresh: () -> Unit,
): FRefreshState {
   return rememberFRefreshState(
      refreshDirection = RefreshDirection.End,
      enabled = enabled,
      onRefresh = onRefresh,
   )
}

@Composable
private fun rememberFRefreshState(
   refreshDirection: RefreshDirection,
   enabled: () -> Boolean = { true },
   onRefresh: () -> Unit,
): FRefreshState {
   val coroutineScope = rememberCoroutineScope()
   return remember(refreshDirection, enabled, coroutineScope) {
      RefreshStateImpl(
         coroutineScope = coroutineScope,
         refreshDirection = refreshDirection,
         enabled = enabled,
      )
   }.apply {
      setRefreshCallback(onRefresh)
   }
}

internal inline fun logMsg(block: () -> String) {
   Log.i("FRefresh", block())
}