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
   indicator: @Composable (FRefreshState) -> Unit = { DefaultRefreshIndicator(state = state) },
) {
   var containerSize by remember { mutableStateOf(IntSize.Zero) }

   when (state.refreshDirection) {
      RefreshDirection.Top, RefreshDirection.Bottom -> containerSize.height
      RefreshDirection.Left, RefreshDirection.Right -> containerSize.width
   }.let {
      // TODO review logic
      state.setRefreshThreshold(it.toFloat())
   }

   Box(
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
            val progress = state.progress
            when (state.refreshDirection) {
               RefreshDirection.Top -> translationY = progress * size.height - size.height
               RefreshDirection.Left -> translationX = progress * size.width - size.width
               RefreshDirection.Bottom -> translationY = size.height - progress * size.height
               RefreshDirection.Right -> translationX = size.width - progress * size.width
            }
         },
      contentAlignment = Alignment.Center,
   ) {
      indicator(state)
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
         refreshDirection = refreshDirection,
         coroutineScope = coroutineScope,
      )
   }.apply {
      setEnabled(enabled)
      setRefreshCallback(onRefresh)
   }
}