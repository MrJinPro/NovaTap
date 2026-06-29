package com.novaboost.novatap.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class NovaTapAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("NovaTapAccessibility", "Accessibility Service Connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun click(x: Float, y: Float, holdDurationMs: Long = 50): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        
        try {
            val strokePath = Path().apply {
                moveTo(x, y)
                // Microscopic offset (0.5 pixel) to ensure OS does not optimize the gesture out as empty.
                // This is crucial on Android 12+ and customized vendors (MIUI, OneUI, ColorOS).
                lineTo(x, y + 0.5f)
            }
            
            // Allow extreme speeds by coercing to 10ms minimum instead of 40ms
            val stroke = GestureDescription.StrokeDescription(strokePath, 0, holdDurationMs.coerceAtLeast(10))
            val gestureBuilder = GestureDescription.Builder().apply {
                addStroke(stroke)
            }
            
            return dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d("NovaTapAccessibility", "Gesture click at ($x, $y) completed successfully.")
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w("NovaTapAccessibility", "Gesture click at ($x, $y) was cancelled by the system.")
                }
            }, null)
        } catch (e: Exception) {
            Log.e("NovaTapAccessibility", "Click gesture failed: ${e.message}", e)
            return false
        }
    }

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        
        try {
            val strokePath = Path().apply {
                moveTo(startX, startY)
                // Ensure there is some physical movement length
                val actualEndX = if (startX == endX) endX + 0.5f else endX
                val actualEndY = if (startY == endY) endY + 0.5f else endY
                lineTo(actualEndX, actualEndY)
            }
            
            val stroke = GestureDescription.StrokeDescription(strokePath, 0, durationMs.coerceAtLeast(10))
            val gestureBuilder = GestureDescription.Builder().apply {
                addStroke(stroke)
            }
            
            return dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d("NovaTapAccessibility", "Gesture swipe completed.")
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w("NovaTapAccessibility", "Gesture swipe cancelled.")
                }
            }, null)
        } catch (e: Exception) {
            Log.e("NovaTapAccessibility", "Swipe gesture failed: ${e.message}", e)
            return false
        }
    }

    suspend fun clickSuspended(x: Float, y: Float, holdDurationMs: Long = 50): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val strokePath = Path().apply {
                    moveTo(x, y)
                    lineTo(x, y + 0.5f)
                }
                
                val stroke = GestureDescription.StrokeDescription(strokePath, 0, holdDurationMs.coerceAtLeast(10))
                val gestureBuilder = GestureDescription.Builder().apply {
                    addStroke(stroke)
                }
                
                val success = dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d("NovaTapAccessibility", "Suspended gesture click completed at ($x, $y).")
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w("NovaTapAccessibility", "Suspended gesture click cancelled at ($x, $y).")
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }, null)
                
                if (!success) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("NovaTapAccessibility", "Suspended click failed: ${e.message}", e)
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }
    }

    suspend fun swipeSuspended(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val strokePath = Path().apply {
                    moveTo(startX, startY)
                    val actualEndX = if (startX == endX) endX + 0.5f else endX
                    val actualEndY = if (startY == endY) endY + 0.5f else endY
                    lineTo(actualEndX, actualEndY)
                }
                
                val stroke = GestureDescription.StrokeDescription(strokePath, 0, durationMs.coerceAtLeast(10))
                val gestureBuilder = GestureDescription.Builder().apply {
                    addStroke(stroke)
                }
                
                val success = dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d("NovaTapAccessibility", "Suspended gesture swipe completed.")
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w("NovaTapAccessibility", "Suspended gesture swipe cancelled.")
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }, null)
                
                if (!success) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("NovaTapAccessibility", "Suspended swipe failed: ${e.message}", e)
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }
    }

    companion object {
        @Volatile
        var instance: NovaTapAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }
}
