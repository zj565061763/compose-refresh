package com.sd.lib.compose.refresh

import android.annotation.SuppressLint
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
  check(state is RefreshStateImpl)
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
fun rememberFRefreshStateTop(
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
fun rememberFRefreshStateBottom(
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
fun rememberFRefreshStateLeft(
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
fun rememberFRefreshStateRight(
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
fun rememberFRefreshStateStart(
  isRefreshing: Boolean? = null,
  enabled: Boolean = true,
  onRefresh: () -> Unit,
): FRefreshState {
  return if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
    rememberFRefreshStateLeft(
      isRefreshing = isRefreshing,
      enabled = enabled,
      onRefresh = onRefresh,
    )
  } else {
    rememberFRefreshStateRight(
      isRefreshing = isRefreshing,
      enabled = enabled,
      onRefresh = onRefresh,
    )
  }
}

@Composable
fun rememberFRefreshStateEnd(
  isRefreshing: Boolean? = null,
  enabled: Boolean = true,
  onRefresh: () -> Unit,
): FRefreshState {
  return if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
    rememberFRefreshStateRight(
      isRefreshing = isRefreshing,
      enabled = enabled,
      onRefresh = onRefresh,
    )
  } else {
    rememberFRefreshStateLeft(
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
  }.apply {
    setEnabled(enabled)
    setOnRefresh(onRefresh)
    if (isRefreshing != null) {
      setRefreshing(isRefreshing)
    }
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