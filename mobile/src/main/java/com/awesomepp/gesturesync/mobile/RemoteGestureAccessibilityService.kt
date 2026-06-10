package com.awesomepp.gesturesync.mobile

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.awesomepp.gesturesync.shared.GestureSyncContract
import kotlin.math.min

class RemoteGestureAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    fun performTap() {
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels / 2f
        val y = metrics.heightPixels / 2f
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 80L))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun performSwipe(direction: String) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val verticalDistance = height * 0.35f
        val horizontalDistance = min(width * 0.35f, height * 0.25f)

        val coordinates = when (direction) {
            GestureSyncContract.DIRECTION_UP -> SwipeCoordinates(
                startX = centerX,
                startY = centerY + verticalDistance,
                endX = centerX,
                endY = centerY - verticalDistance
            )
            GestureSyncContract.DIRECTION_DOWN -> SwipeCoordinates(
                startX = centerX,
                startY = centerY - verticalDistance,
                endX = centerX,
                endY = centerY + verticalDistance
            )
            GestureSyncContract.DIRECTION_LEFT -> SwipeCoordinates(
                startX = centerX + horizontalDistance,
                startY = centerY,
                endX = centerX - horizontalDistance,
                endY = centerY
            )
            GestureSyncContract.DIRECTION_RIGHT -> SwipeCoordinates(
                startX = centerX - horizontalDistance,
                startY = centerY,
                endX = centerX + horizontalDistance,
                endY = centerY
            )
            else -> return
        }

        val path = Path().apply {
            moveTo(coordinates.startX, coordinates.startY)
            lineTo(coordinates.endX, coordinates.endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 250L))
            .build()

        dispatchGesture(gesture, null, null)
    }

    private data class SwipeCoordinates(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float
    )

    companion object {
        var instance: RemoteGestureAccessibilityService? = null
            private set
    }
}
