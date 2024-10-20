package com.sd.lib.compose.refresh

import android.annotation.SuppressLint
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.sd.lib.compose.refresh.indicator.DefaultRefreshIndicator

@Composable
fun FRefreshContainer(
   state: FRefreshState,
   modifier: Modifier = Modifier,
   getRefreshThreshold: @Composable (IntSize) -> Float = {
      when (state.refreshDirection) {
         RefreshDirection.Top, RefreshDirection.Bottom -> it.height
         RefreshDirection.Left, RefreshDirection.Right -> it.width
      }.toFloat()
   },
   indicator: @Composable () -> Unit = { DefaultRefreshIndicator(state = state) },
) {
   check(state is RefreshStateImpl)

   var containerSize by remember { mutableStateOf(IntSize.Zero) }
   state.setRefreshThreshold(getRefreshThreshold(containerSize))

   Box(
      contentAlignment = Alignment.Center,
      modifier = modifier
         .onSizeChanged {
            containerSize = it
         }
         .drawWithContent {
            if (state.currentInteraction != RefreshInteraction.None) {
               drawContent()
            }
         }
         .graphicsLayer {
            val distance = state.progress * state.refreshThreshold
            when (state.refreshDirection) {
               RefreshDirection.Top -> translationY = distance - size.height
               RefreshDirection.Left -> translationX = distance - size.width
               RefreshDirection.Bottom -> translationY = size.height - distance
               RefreshDirection.Right -> translationX = size.width - distance
            }
         },
   ) {
      indicator()
   }
}

@SuppressLint("ComposableNaming")
@Composable
fun FRefreshState.setRefreshing(isRefreshing: Boolean) {
   LaunchedEffect(isRefreshing) {
      if (isRefreshing) {
         showRefresh()
      } else {
         hideRefresh()
      }
   }
}

@Composable
fun rememberFRefreshStateTop(
   enabled: Boolean = true,
   onRefresh: () -> Unit,
): FRefreshState {
   return rememberRefreshState(
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
   return rememberRefreshState(
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
   return rememberRefreshState(
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
   return rememberRefreshState(
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
private fun rememberRefreshState(
   refreshDirection: RefreshDirection,
   enabled: Boolean = true,
   onRefresh: () -> Unit,
): FRefreshState {
   val coroutineScope = rememberCoroutineScope()
   return remember(refreshDirection, coroutineScope) {
      RefreshStateImpl(
         refreshDirection = refreshDirection,
         coroutineScope = coroutineScope,
      )
   }.apply {
      setEnabled(enabled)
      setRefreshCallback(onRefresh)
   }
}