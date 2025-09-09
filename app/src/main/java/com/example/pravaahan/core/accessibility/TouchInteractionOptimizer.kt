package com.example.pravaahan.core.accessibility

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.pravaahan.core.logging.Logger
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Optimizes touch interactions for railway map navigation with accessibility support
 */
@Singleton
class TouchInteractionOptimizer @Inject constructor(
    private val logger: Logger,
    private val hapticManager: HapticFeedbackManager
) {
    companion object {
        private const val TAG = "TouchInteractionOptimizer"
        private const val MIN_TOUCH_TARGET_SIZE_DP = 48f // Material Design minimum
        private const val DOUBLE_TAP_TIMEOUT_MS = 300L
        private const val LONG_PRESS_TIMEOUT_MS = 500L
        private const val GESTURE_VELOCITY_THRESHOLD = 100f
        private const val ZOOM_SENSITIVITY_FACTOR = 0.8f
        private const val PAN_SENSITIVITY_FACTOR = 1.2f
    }
    
    /**
     * Enhanced gesture handling with accessibility optimizations
     */
    @Composable
    fun Modifier.optimizedMapGestures(
        onTap: (Offset) -> Unit = {},
        onDoubleTap: (Offset) -> Unit = {},
        onLongPress: (Offset) -> Unit = {},
        onPan: (Offset) -> Unit = {},
        onZoom: (Float, Offset) -> Unit = { _, _ -> },
        isAccessibilityEnabled: Boolean = false
    ): Modifier {
        val density = LocalDensity.current
        val hapticFeedback = LocalHapticFeedback.current
        
        var lastTapTime by remember { mutableStateOf(0L) }
        var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
        var isLongPressActive by remember { mutableStateOf(false) }
        
        val minTouchTargetPx = with(density) { MIN_TOUCH_TARGET_SIZE_DP.dp.toPx() }
        
        return this
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val currentTime = System.currentTimeMillis()
                        val timeDiff = currentTime - lastTapTime
                        val distance = calculateDistance(offset, lastTapPosition)
                        
                        if (timeDiff < DOUBLE_TAP_TIMEOUT_MS && distance < minTouchTargetPx) {
                            // Double tap detected
                            logger.debug(TAG, "Double tap detected at: $offset")
                            hapticManager.performLightImpact()
                            onDoubleTap(offset)
                        } else {
                            // Single tap
                            logger.debug(TAG, "Single tap at: $offset")
                            if (isAccessibilityEnabled) {
                                hapticManager.performLightImpact()
                            }
                            onTap(offset)
                        }
                        
                        lastTapTime = currentTime
                        lastTapPosition = offset
                    },
                    onLongPress = { offset ->
                        logger.debug(TAG, "Long press at: $offset")
                        hapticManager.performHeavyImpact()
                        isLongPressActive = true
                        onLongPress(offset)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        logger.debug(TAG, "Pan gesture started at: $offset")
                        if (isAccessibilityEnabled) {
                            hapticManager.performLightImpact()
                        }
                    },
                    onDrag = { _, dragAmount ->
                        val adjustedDrag = dragAmount * PAN_SENSITIVITY_FACTOR
                        onPan(adjustedDrag)
                    },
                    onDragEnd = {
                        logger.debug(TAG, "Pan gesture ended")
                        isLongPressActive = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (abs(zoom - 1f) > 0.01f) {
                        val adjustedZoom = 1f + (zoom - 1f) * ZOOM_SENSITIVITY_FACTOR
                        logger.debug(TAG, "Zoom gesture: $adjustedZoom")
                        
                        if (isAccessibilityEnabled && abs(zoom - 1f) > 0.1f) {
                            hapticManager.performMediumImpact()
                        }
                        
                        onZoom(adjustedZoom, pan)
                    }
                }
            }
    }
    
    /**
     * Enhanced train selection with larger touch targets for accessibility
     */
    @Composable
    fun Modifier.optimizedTrainSelection(
        onTrainSelected: (String) -> Unit,
        trainId: String,
        isAccessibilityEnabled: Boolean = false
    ): Modifier {
        val density = LocalDensity.current
        val minTouchTargetPx = with(density) { 
            if (isAccessibilityEnabled) 56.dp.toPx() else MIN_TOUCH_TARGET_SIZE_DP.dp.toPx()
        }
        
        return this.pointerInput(trainId) {
            detectTapGestures(
                onTap = { offset ->
                    logger.info(TAG, "Train $trainId selected via touch at: $offset")
                    hapticManager.performSelectionFeedback()
                    onTrainSelected(trainId)
                }
            )
        }
    }
    
    /**
     * Gesture recognition with velocity tracking
     */
    fun recognizeGesture(
        startPosition: Offset,
        endPosition: Offset,
        duration: Long
    ): GestureType {
        val distance = calculateDistance(startPosition, endPosition)
        val velocity = distance / duration * 1000 // pixels per second
        
        return when {
            distance < 10f -> GestureType.TAP
            velocity > GESTURE_VELOCITY_THRESHOLD -> GestureType.SWIPE
            duration > LONG_PRESS_TIMEOUT_MS -> GestureType.LONG_PRESS
            else -> GestureType.DRAG
        }
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(point1: Offset, point2: Offset): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Log gesture performance metrics
     */
    fun logGesturePerformance(gestureType: GestureType, processingTime: Long) {
        logger.debug(TAG, "${gestureType.name} gesture processed in ${processingTime}ms")
        
        if (processingTime > 16) { // More than one frame at 60fps
            logger.warn(TAG, "Slow gesture processing: ${gestureType.name} took ${processingTime}ms")
        }
    }
}

/**
 * Types of gestures for optimization
 */
enum class GestureType {
    TAP,
    DOUBLE_TAP,
    LONG_PRESS,
    DRAG,
    SWIPE,
    ZOOM,
    ROTATE
}