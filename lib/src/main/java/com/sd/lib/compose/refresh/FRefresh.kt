package com.sd.lib.compose.refresh

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.sd.lib.compose.refresh.indicator.DefaultRefreshIndicator

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
               RefreshDirection.Left -> translationX = state.offset - size.width
               RefreshDirection.Right -> translationX = state.offset + size.width
            }
         },
      contentAlignment = Alignment.Center,
   ) {
      indicator(state)
   }
}

@Composable
fun rememberFRefreshStateTop(
   enabled: Boolean = true,
   onRefresh: () -> Unit,
): FRefreshState {
   return rememberFRefreshState(
      refreshDirection = RefreshDirection.Top,
      enabled = enabled,
      onRefresh = onRefresh,
   )
}

@Composable
fun rememberFRefreshStateBottom(
   enabled: Boolean = true,
   onRefresh: () -> Unit,
): FRefreshState {
   return rememberFRefreshState(
      refreshDirection = RefreshDirection.Bottom,
      enabled = enabled,
      onRefresh = onRefresh,
   )
}

@Composable
fun rememberFRefreshStateLeft(
   enabled: Boolean = true,
   onRefresh: () -> Unit,
): FRefreshState {
   return rememberFRefreshState(
      refreshDirection = RefreshDirection.Left,
      enabled = enabled,
      onRefresh = onRefresh,
   )
}

@Composable
fun rememberFRefreshStateRight(
   enabled: Boolean = true,
   onRefresh: () -> Unit,
): FRefreshState {
   return rememberFRefreshState(
      refreshDirection = RefreshDirection.Right,
      enabled = enabled,
      onRefresh = onRefresh,
   )
}

@Composable
fun rememberFRefreshStateStart(
   enabled: Boolean = true,
   onRefresh: () -> Unit,
): FRefreshState {
   return if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
      rememberFRefreshStateLeft(
         enabled = enabled,
         onRefresh = onRefresh,
      )
   } else {
      rememberFRefreshStateRight(
         enabled = enabled,
         onRefresh = onRefresh,
      )
   }
}

@Composable
fun rememberFRefreshStateEnd(
   enabled: Boolean = true,
   onRefresh: () -> Unit,
): FRefreshState {
   return if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
      rememberFRefreshStateRight(
         enabled = enabled,
         onRefresh = onRefresh,
      )
   } else {
      rememberFRefreshStateLeft(
         enabled = enabled,
         onRefresh = onRefresh,
      )
   }
}

@Composable
private fun rememberFRefreshState(
   refreshDirection: RefreshDirection,
   enabled: Boolean = true,
   onRefresh: () -> Unit,
): FRefreshState {
   val coroutineScope = rememberCoroutineScope()
   return remember(refreshDirection, coroutineScope) {
      RefreshStateImpl(
         coroutineScope = coroutineScope,
         refreshDirection = refreshDirection,
      )
   }.apply {
      setEnabled(enabled)
      setRefreshCallback(onRefresh)
   }
}