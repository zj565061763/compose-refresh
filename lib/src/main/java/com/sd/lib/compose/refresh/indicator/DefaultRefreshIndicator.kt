package com.sd.lib.compose.refresh.indicator

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.refresh.FRefreshState
import com.sd.lib.compose.refresh.RefreshDirection
import com.sd.lib.compose.refresh.RefreshInteraction

/**
 * 默认的刷新指示器
 */
@Composable
fun DefaultRefreshIndicator(
    state: FRefreshState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    size: Dp = 40.dp,
    spinnerSize: Dp = size.times(0.5f),
    padding: PaddingValues = PaddingValues(5.dp),
    shadow: Boolean = true,
) {
    val animScale = remember(state) { Animatable(1f) }
    DisposableEffect(state) {
        val callback: suspend () -> Unit = {
            animScale.animateTo(0f)
        }
        state.registerHideRefreshing(callback)
        onDispose {
            state.unregisterHideRefreshing(callback)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.currentInteraction }
            .collect {
                if (it == RefreshInteraction.None) {
                    animScale.snapTo(1f)
                }
            }
    }

    GoogleRefreshIndicator(
        isRefreshing = state.isRefreshing,
        progress = state.progress.coerceIn(0f, 1f),
        modifier = modifier
            .graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
            },
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        strokeWidth = strokeWidth,
        size = size,
        spinnerSize = spinnerSize,
        padding = padding,
        shadow = shadow,
        rotationZ = when (state.refreshDirection) {
            RefreshDirection.Top -> 0f
            RefreshDirection.Right -> 90f
            RefreshDirection.Bottom -> 180f
            RefreshDirection.Left -> 270f
        },
    )
}
