package com.sd.lib.compose.refresh

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity

internal interface DirectionHandler {
   fun handlePreScroll(available: Offset): Offset
   fun handlePostScroll(available: Offset): Offset
   suspend fun handlePreFling(available: Velocity): Velocity

   fun isBack(available: Float): Boolean
}

internal fun DirectionHandler(
   refreshDirection: RefreshDirection,
   onPreScroll: (Float) -> Float?,
   onPostScroll: (Float) -> Float?,
   onPreFling: suspend (Float) -> Float?,
): DirectionHandler {
   return BaseDirectionHandler(
      refreshDirection = refreshDirection,
      onPreScroll = onPreScroll,
      onPostScroll = onPostScroll,
      onPreFling = onPreFling,
   )
}

private class BaseDirectionHandler(
   private val refreshDirection: RefreshDirection,
   private val onPreScroll: (Float) -> Float?,
   private val onPostScroll: (Float) -> Float?,
   private val onPreFling: suspend (Float) -> Float?,
) : DirectionHandler {
   override fun handlePreScroll(available: Offset): Offset {
      val unpackOffset = unpackOffset(available).also { if (it == 0f) return Offset.Zero }
      val consumed = onPreScroll(unpackOffset) ?: return Offset.Zero
      return packOffset(consumed)
   }

   override fun handlePostScroll(available: Offset): Offset {
      val unpackOffset = unpackOffset(available).also { if (it == 0f) return Offset.Zero }
      val consumed = onPostScroll(unpackOffset) ?: return Offset.Zero
      return packOffset(consumed)
   }

   override suspend fun handlePreFling(available: Velocity): Velocity {
      val consumed = onPreFling(unpackVelocity(available)) ?: return Velocity.Zero
      return packVelocity(consumed)
   }

   override fun isBack(available: Float): Boolean {
      return when (refreshDirection) {
         RefreshDirection.Top, RefreshDirection.Left -> available < 0
         RefreshDirection.Bottom, RefreshDirection.Right -> available > 0
      }
   }

   private fun unpackOffset(value: Offset): Float {
      return when (refreshDirection) {
         RefreshDirection.Top, RefreshDirection.Bottom -> value.y
         RefreshDirection.Left, RefreshDirection.Right -> value.x
      }
   }

   private fun packOffset(value: Float): Offset {
      return when (refreshDirection) {
         RefreshDirection.Top, RefreshDirection.Bottom -> Offset(0f, value)
         RefreshDirection.Left, RefreshDirection.Right -> Offset(value, 0f)
      }
   }

   private fun unpackVelocity(value: Velocity): Float {
      return when (refreshDirection) {
         RefreshDirection.Top, RefreshDirection.Bottom -> value.y
         RefreshDirection.Left, RefreshDirection.Right -> value.x
      }
   }

   private fun packVelocity(value: Float): Velocity {
      return when (refreshDirection) {
         RefreshDirection.Top, RefreshDirection.Bottom -> Velocity(0f, value)
         RefreshDirection.Left, RefreshDirection.Right -> Velocity(value, 0f)
      }
   }
}