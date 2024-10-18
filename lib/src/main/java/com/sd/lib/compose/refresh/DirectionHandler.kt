package com.sd.lib.compose.refresh

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity

internal interface DirectionHandler {
    fun handlePreScroll(available: Offset): Offset
    fun handlePostScroll(available: Offset): Offset
    suspend fun handlePreFling(available: Velocity): Velocity
}

internal fun DirectionHandler(
    refreshDirection: RefreshDirection,
    onScroll: (Float) -> Float?,
    onFling: suspend (Float) -> Float?,
): DirectionHandler {
    return DirectionHandlerImpl(
        refreshDirection = refreshDirection,
        onScroll = onScroll,
        onFling = onFling,
    )
}

private abstract class BaseDirectionHandler(
    protected val refreshDirection: RefreshDirection
) : DirectionHandler {
    final override fun handlePreScroll(available: Offset): Offset {
        val consumed = onPreScroll(unpackOffset(available)) ?: return Offset.Zero
        return packOffset(consumed)
    }

    final override fun handlePostScroll(available: Offset): Offset {
        val consumed = onPostScroll(unpackOffset(available)) ?: return Offset.Zero
        return packOffset(consumed)
    }

    final override suspend fun handlePreFling(available: Velocity): Velocity {
        val consumed = onPreFling(unpackVelocity(available)) ?: return Velocity.Zero
        return packVelocity(consumed)
    }

    private fun unpackOffset(value: Offset): Float {
        return when (refreshDirection) {
            RefreshDirection.Top, RefreshDirection.Bottom -> value.y
            RefreshDirection.Start, RefreshDirection.End -> value.x
        }
    }

    private fun packOffset(value: Float): Offset {
        return when (refreshDirection) {
            RefreshDirection.Top, RefreshDirection.Bottom -> Offset(0f, value)
            RefreshDirection.Start, RefreshDirection.End -> Offset(value, 0f)
        }
    }

    private fun unpackVelocity(value: Velocity): Float {
        return when (refreshDirection) {
            RefreshDirection.Top, RefreshDirection.Bottom -> value.y
            RefreshDirection.Start, RefreshDirection.End -> value.x
        }
    }

    private fun packVelocity(value: Float): Velocity {
        return when (refreshDirection) {
            RefreshDirection.Top, RefreshDirection.Bottom -> Velocity(0f, value)
            RefreshDirection.Start, RefreshDirection.End -> Velocity(value, 0f)
        }
    }

    abstract fun onPreScroll(available: Float): Float?
    abstract fun onPostScroll(available: Float): Float?
    abstract suspend fun onPreFling(available: Float): Float?
}

private class DirectionHandlerImpl(
    refreshDirection: RefreshDirection,
    private val onScroll: (Float) -> Float?,
    private val onFling: suspend (Float) -> Float?,
) : BaseDirectionHandler(refreshDirection = refreshDirection) {

    override fun onPreScroll(available: Float): Float? {
        if (isBack(available)) {
            return onScroll(available)
        }
        return null
    }

    override fun onPostScroll(available: Float): Float? {
        if (isOut(available)) {
            return onScroll(available)
        }
        return null
    }

    override suspend fun onPreFling(available: Float): Float? {
        return onFling(available)
    }

    private fun isOut(available: Float): Boolean {
        return when (refreshDirection) {
            RefreshDirection.Top, RefreshDirection.Start -> available > 0
            RefreshDirection.Bottom, RefreshDirection.End -> available < 0
        }
    }

    private fun isBack(available: Float): Boolean {
        return when (refreshDirection) {
            RefreshDirection.Top, RefreshDirection.Start -> available < 0
            RefreshDirection.Bottom, RefreshDirection.End -> available > 0
        }
    }
}