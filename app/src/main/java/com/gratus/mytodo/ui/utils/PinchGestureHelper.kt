package com.gratus.mytodo.ui.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * Custom pointer input extension that detects cumulative pinch-to-zoom gestures.
 * Unlike standard [androidx.compose.foundation.gestures.detectTransformGestures] which only returns
 * the instantaneous zoom factor per frame, this helper aggregates the zoom changes over a single
 * continuous gesture sequence.
 */
suspend fun PointerInputScope.detectPinchZoom(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    awaitEachGesture {
        var accumulatedZoom = 1f
        // Wait for the first finger down to start the gesture cycle
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoom = event.calculateZoom()
                if (zoom != 1f) {
                    accumulatedZoom *= zoom
                    if (accumulatedZoom > 1.25f) {
                        onZoomIn()
                        accumulatedZoom = 1f // Reset to prevent double-firing in same gesture
                    } else if (accumulatedZoom < 0.75f) {
                        onZoomOut()
                        accumulatedZoom = 1f // Reset to prevent double-firing in same gesture
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}
