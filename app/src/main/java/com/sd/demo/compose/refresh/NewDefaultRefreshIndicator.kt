package com.sd.demo.compose.refresh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.refresh.FRefreshState

@Composable
fun NewDefaultRefreshIndicator(
    modifier: Modifier = Modifier,
    state: FRefreshState,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    size: Dp = 40.dp,
    spinnerSize: Dp = size.times(0.5f),
    padding: PaddingValues = PaddingValues(5.dp),
    shadow: Boolean = true,
) {
//    PaddingSizedBox(
//        modifier = modifier,
//        size = size,
//        padding = padding,
//    ) {
//        CircularBox(
//            backgroundColor = backgroundColor,
//            shadow = shadow,
//        ) {
//            NewGoogleRefreshIndicator(
//                isRefreshing = state.isRefreshing,
//                progress = { state.progress },
//                contentColor = contentColor,
//                spinnerSize = spinnerSize,
//                strokeWidth = strokeWidth,
//            )
//        }
//    }

    WrapperBox(
        modifier = modifier,
        size = size,
        padding = padding,
        backgroundColor = backgroundColor,
        shadow = shadow,
    ) {
        NewGoogleRefreshIndicator(
            isRefreshing = state.isRefreshing,
            progress = { state.progress },
            contentColor = contentColor,
            spinnerSize = spinnerSize,
            strokeWidth = strokeWidth,
        )
    }
}

@Composable
private fun WrapperBox(
    modifier: Modifier = Modifier,
    size: Dp,
    padding: PaddingValues,
    backgroundColor: Color,
    shadow: Boolean,
    content: @Composable () -> Unit,
) {
    val shadowColor = contentColorFor(backgroundColor)

    Box(
        modifier = modifier.padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(backgroundColor, CircleShape)
                .let {
                    if (shadow) {
                        it.drawBehind {
                            drawIntoCanvas { canvas ->
                                val paint = Paint()
                                with(paint.asFrameworkPaint()) {
                                    this.color = backgroundColor.toArgb()
                                    this.setShadowLayer(
                                        5.dp.toPx(),
                                        0f,
                                        0f,
                                        shadowColor
                                            .copy(0.2f)
                                            .toArgb(),
                                    )
                                }

                                val outline = CircleShape.createOutline(this.size, this.layoutDirection, this)
                                canvas.drawOutline(outline, paint)
                            }
                        }
                    } else {
                        it
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun CircularBox(
    backgroundColor: Color,
    shadow: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shadowColor = contentColorFor(backgroundColor)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor, CircleShape)
            .let {
                if (shadow) {
                    it.drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint()
                            with(paint.asFrameworkPaint()) {
                                this.color = backgroundColor.toArgb()
                                this.setShadowLayer(
                                    5.dp.toPx(),
                                    0f,
                                    0f,
                                    shadowColor
                                        .copy(0.2f)
                                        .toArgb(),
                                )
                            }

                            val outline = CircleShape.createOutline(size, layoutDirection, this)
                            canvas.drawOutline(outline, paint)
                        }
                    }
                } else {
                    it
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}