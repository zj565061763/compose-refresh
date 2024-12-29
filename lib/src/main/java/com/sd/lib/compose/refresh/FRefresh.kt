package com.sd.lib.compose.refresh

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
  getRefreshThreshold: @Composable (IntSize) -> Float = { size ->
    when (state.refreshDirection) {
      RefreshDirection.Top, RefreshDirection.Bottom -> size.height
      RefreshDirection.Left, RefreshDirection.Right -> size.width
    }.toFloat()
  },
  indicator: @Composable () -> Unit = { DefaultRefreshIndicator(state = state) },
) {
  require(state is RefreshStateImpl)
  var containerSize by remember { mutableStateOf(IntSize.Zero) }

  val refreshThreshold = getRefreshThreshold(containerSize)
  state.setRefreshThreshold(refreshThreshold)

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

@Composable
fun rememberRefreshStateTop(
  isRefreshing: Boolean? = null,
  enabled: Boolean = true,
  onRefresh: () -> Unit,
): FRefreshState {
  return rememberRefreshState(
    refreshDirection = RefreshDirection.Top,
    isRefreshing = isRefreshing,
    enabled = enabled,
    onRefresh = onRefresh,
  )
}

@Composable
fun rememberRefreshStateBottom(
  isRefreshing: Boolean? = null,
  enabled: Boolean = true,
  onRefresh: () -> Unit,
): FRefreshState {
  return rememberRefreshState(
    refreshDirection = RefreshDirection.Bottom,
    isRefreshing = isRefreshing,
    enabled = enabled,
    onRefresh = onRefresh,
  )
}

@Composable
fun rememberRefreshStateLeft(
  isRefreshing: Boolean? = null,
  enabled: Boolean = true,
  onRefresh: () -> Unit,
): FRefreshState {
  return rememberRefreshState(
    refreshDirection = RefreshDirection.Left,
    isRefreshing = isRefreshing,
    enabled = enabled,
    onRefresh = onRefresh,
  )
}

@Composable
fun rememberRefreshStateRight(
  isRefreshing: Boolean? = null,
  enabled: Boolean = true,
  onRefresh: () -> Unit,
): FRefreshState {
  return rememberRefreshState(
    refreshDirection = RefreshDirection.Right,
    isRefreshing = isRefreshing,
    enabled = enabled,
    onRefresh = onRefresh,
  )
}

@Composable
fun rememberRefreshStateStart(
  isRefreshing: Boolean? = null,
  enabled: Boolean = true,
  onRefresh: () -> Unit,
): FRefreshState {
  return if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
    rememberRefreshStateLeft(
      isRefreshing = isRefreshing,
      enabled = enabled,
      onRefresh = onRefresh,
    )
  } else {
    rememberRefreshStateRight(
      isRefreshing = isRefreshing,
      enabled = enabled,
      onRefresh = onRefresh,
    )
  }
}

@Composable
fun rememberRefreshStateEnd(
  isRefreshing: Boolean? = null,
  enabled: Boolean = true,
  onRefresh: () -> Unit,
): FRefreshState {
  return if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
    rememberRefreshStateRight(
      isRefreshing = isRefreshing,
      enabled = enabled,
      onRefresh = onRefresh,
    )
  } else {
    rememberRefreshStateLeft(
      isRefreshing = isRefreshing,
      enabled = enabled,
      onRefresh = onRefresh,
    )
  }
}

@Composable
private fun rememberRefreshState(
  refreshDirection: RefreshDirection,
  isRefreshing: Boolean?,
  enabled: Boolean,
  onRefresh: () -> Unit,
): FRefreshState {
  return remember(refreshDirection) {
    RefreshStateImpl(refreshDirection)
  }.also { state ->
    state.setEnabled(enabled)
    state.setOnRefresh(onRefresh)
    if (isRefreshing != null) {
      LaunchedEffect(state, isRefreshing) {
        if (isRefreshing) {
          state.showRefresh()
        } else {
          state.hideRefresh()
        }
      }
    }
  }
}