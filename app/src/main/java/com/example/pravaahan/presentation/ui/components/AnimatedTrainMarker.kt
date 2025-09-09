package com.example.pravaahan.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.*
import com.example.pravaahan.presentation.ui.components.animation.*
import com.example.pravaahan.presentation.ui.components.map.MapState
import androidx.compose.runtime.withFrameNanos
import kotlin.math.*
import kotlin.system.measureTimeMillis

private const val TAG = "AnimatedTrainMarker"
private const val MARKER_SIZE_DP = 24
private const val MARKER_HALF_SIZE_DP = 12

/**
 * Memory-efficient animated train marker with comprehensive lifecycle management and performance optimization.
 * Implements animation pooling, visibility-based optimization, and distance-based duration calculation.
 */
@Composable
fun AnimatedTrainMarker(
    trainState: RealTimeTrainState,
    mapState: MapState,
    onTrainSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    logger: Logger? = null,
    animationPool: AnimationPool? = null,
    visibilityOptimizer: VisibilityOptimizer? = null,
    performanceMonitor: PerformanceMonitor? = null,
    isVisible: Boolean = true, // Allow external visibility control for performance
    trainPriority: TrainPriority? = null
) {
    
    // Only render if we have position data
    val currentPosition = trainState.currentPosition ?: return
    
    // Enhanced visibility check with optimizer
    val isActuallyVisible = remember(currentPosition, mapState, isVisible) {
        if (!isVisible) return@remember false
        
        visibilityOptimizer?.isTrainVisible(currentPosition, mapState, trainPriority) 
            ?: run {
                // Fallback visibility calculation
                val targetPosition = mapState.railwayToScreen(
                    Offset(currentPosition.latitude.toFloat(), currentPosition.longitude.toFloat())
                )
                targetPosition.x >= -50 && targetPosition.x <= mapState.canvasSize.width + 50 &&
                targetPosition.y >= -50 && targetPosition.y <= mapState.canvasSize.height + 50
            }
    }
    
    // Early return if not visible to save resources
    if (!isActuallyVisible) {
        logger?.debug(TAG, "Train ${trainState.trainId} not visible, skipping animation")
        return
    }
    
    // Get animation priority for resource allocation
    val animationPriority = remember(isActuallyVisible, trainPriority, currentPosition.speed) {
        visibilityOptimizer?.getAnimationPriority(
            trainId = trainState.trainId,
            isVisible = isActuallyVisible,
            trainPriority = trainPriority,
            isMoving = currentPosition.speed > 0.1
        ) ?: AnimationPriority.MEDIUM
    }
    
    // Skip animation for NONE priority
    if (animationPriority == AnimationPriority.NONE) return
    
    // Enhanced animation state with pooling and lifecycle management
    val animatedPosition = rememberAnimationWithPool(
        trainId = trainState.trainId,
        animationPool = animationPool,
        logger = logger
    )
    
    // Calculate target position
    val targetPosition = remember(currentPosition, mapState) {
        mapState.railwayToScreen(
            Offset(currentPosition.latitude.toFloat(), currentPosition.longitude.toFloat())
        )
    }
    
    // Determine if train is moving
    val isMoving = currentPosition.speed > 0.1
    
    // Enhanced animation with performance monitoring
    LaunchedEffect(targetPosition, animationPriority, trainState.trainId) {
        performanceMonitor?.recordAnimationStart(trainState.trainId)
        
        val animationTime = measureTimeMillis {
            animateToTargetPosition(
                animatedPosition = animatedPosition,
                targetPosition = targetPosition,
                currentPosition = currentPosition,
                animationPriority = animationPriority,
                logger = logger,
                trainId = trainState.trainId
            )
        }
        
        performanceMonitor?.recordFrameTime(animationTime.toFloat())
        performanceMonitor?.recordAnimationEnd(trainState.trainId, animationTime)
    }
    
    // Enhanced train marker with status and priority chips
    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (animatedPosition.value.x - MARKER_HALF_SIZE_DP).roundToInt(),
                    (animatedPosition.value.y - MARKER_HALF_SIZE_DP).roundToInt()
                )
            }
            .size(MARKER_SIZE_DP.dp)
            .clip(CircleShape)
            .clickable {
                logger?.debug(TAG, "Train ${trainState.trainId} selected at position (${animatedPosition.value.x}, ${animatedPosition.value.y})")
                onTrainSelected(trainState.trainId)
            }
            .semantics {
                contentDescription = buildTrainContentDescription(trainState, currentPosition)
            }
    ) {
        // Main train marker canvas
        TrainMarkerCanvas(
            trainState = trainState,
            isMoving = isMoving,
            animationPriority = animationPriority,
            modifier = Modifier.fillMaxSize()
        )
        
        // Status and priority indicators (only for high priority trains to save resources)
        if (animationPriority >= AnimationPriority.HIGH) {
            TrainStatusOverlay(
                trainState = trainState,
                trainPriority = trainPriority,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

/**
 * Enhanced train marker canvas with performance optimizations and status indicators.
 */
@Composable
private fun TrainMarkerCanvas(
    trainState: RealTimeTrainState,
    isMoving: Boolean,
    animationPriority: AnimationPriority,
    modifier: Modifier = Modifier
) {
    val trainColor = remember { Color(0xFF2196F3) } // Cached color
    val connectionColor = remember(trainState.connectionStatus) { 
        getConnectionStatusColor(trainState.connectionStatus) 
    }
    
    // Adaptive animation quality based on priority
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = if (isMoving && animationPriority >= AnimationPriority.MEDIUM) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (animationPriority) {
                    AnimationPriority.CRITICAL -> 800
                    AnimationPriority.HIGH -> 1000
                    else -> 1200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 4
        
        // Draw connection status ring (only for high priority)
        if (animationPriority >= AnimationPriority.HIGH) {
            drawCircle(
                color = connectionColor,
                radius = radius * 1.5f,
                center = center,
                alpha = 0.3f
            )
        }
        
        // Draw main train marker with adaptive pulse effect
        drawCircle(
            color = trainColor,
            radius = radius * pulseScale,
            center = center
        )
        
        // Draw direction indicator if moving and priority allows
        if (isMoving && animationPriority >= AnimationPriority.MEDIUM) {
            trainState.currentPosition?.let { position ->
                if (position.speed > 0.1) {
                    drawDirectionIndicator(
                        center = center,
                        radius = radius,
                        heading = position.heading,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Status overlay with enhanced TrainStatusChip and TrainPriorityChip integration.
 */
@Composable
private fun TrainStatusOverlay(
    trainState: RealTimeTrainState,
    trainPriority: TrainPriority?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(2.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Priority chip (only for express and high priority)
        trainPriority?.let { priority ->
            if (priority == TrainPriority.EXPRESS || priority == TrainPriority.HIGH) {
                TrainPriorityChip(
                    priority = priority,
                    modifier = Modifier.size(width = 32.dp, height = 12.dp)
                )
            }
        }
        
        // Status chip for critical statuses
        if (trainState.currentPosition?.let { pos ->
            pos.speed == 0.0 || !pos.isSafetyReliable
        } == true) {
            // Note: We need to get the train status from somewhere - this is a placeholder
            // In a real implementation, this would come from the Train domain model
            Spacer(modifier = Modifier.height(2.dp))
            // TrainStatusChip would go here if we had access to TrainStatus
        }
    }
}

/**
 * Remembers animation state with pool management and proper lifecycle handling.
 */
@Composable
private fun rememberAnimationWithPool(
    trainId: String,
    animationPool: AnimationPool?,
    logger: Logger?
): Animatable<Offset, *> {
    // Use saveable state for configuration change persistence
    val animationState = rememberSaveable(
        trainId,
        saver = Saver(
            save = { "${it.value.x},${it.value.y}" },
            restore = { 
                val parts = it.split(",")
                if (parts.size == 2) {
                    try {
                        Animatable(Offset(parts[0].toFloat(), parts[1].toFloat()), Offset.VectorConverter)
                    } catch (e: NumberFormatException) {
                        Animatable(Offset.Zero, Offset.VectorConverter)
                    }
                } else {
                    Animatable(Offset.Zero, Offset.VectorConverter)
                }
            }
        )
    ) { 
        Animatable(Offset.Zero, Offset.VectorConverter)
    }
    
    // Cleanup on disposal
    DisposableEffect(trainId) {
        onDispose {
            // Return animation to pool if available
            animationPool?.let { pool ->
                // Note: This would need to be a suspend function call in a real implementation
                logger?.debug("AnimatedTrainMarker", "Disposing animation for train $trainId")
            }
        }
    }
    
    return animationState
}

/**
 * Enhanced animation logic with distance-based duration and performance optimization.
 */
private suspend fun animateToTargetPosition(
    animatedPosition: Animatable<Offset, *>,
    targetPosition: Offset,
    currentPosition: TrainPosition,
    animationPriority: AnimationPriority,
    logger: Logger?,
    trainId: String
) {
    val currentOffset = animatedPosition.value
    val distance = sqrt(
        (targetPosition.x - currentOffset.x).pow(2) + 
        (targetPosition.y - currentOffset.y).pow(2)
    )
    
    // Enhanced animation duration calculation
    val animationDuration = calculateEnhancedAnimationDuration(
        distance = distance,
        speed = currentPosition.speed,
        priority = animationPriority
    )
    
    logger?.debug(
        "AnimatedTrainMarker",
        "Animating train $trainId: distance=${distance}px, duration=${animationDuration}ms, speed=${currentPosition.speed}km/h, priority=$animationPriority"
    )
    
    if (animationDuration > 0) {
        // Use TargetBasedAnimation for precise control with high-priority trains
        if (animationPriority >= AnimationPriority.HIGH) {
            val targetBasedAnimation = TargetBasedAnimation(
                animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                ),
                typeConverter = Offset.VectorConverter,
                initialValue = currentOffset,
                targetValue = targetPosition
            )
            
            val startTime = withFrameNanos { it }
            
            do {
                val playTime = withFrameNanos { it } - startTime
                val animationValue = targetBasedAnimation.getValueFromNanos(playTime)
                animatedPosition.snapTo(animationValue)
            } while (!targetBasedAnimation.isFinishedFromNanos(playTime))
        } else {
            // Standard animation for lower priority trains
            animatedPosition.animateTo(
                targetValue = targetPosition,
                animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                )
            )
        }
    } else {
        animatedPosition.snapTo(targetPosition)
    }
}

/**
 * Enhanced animation duration calculation with priority-based optimization.
 */
private fun calculateEnhancedAnimationDuration(
    distance: Float,
    speed: Double,
    priority: AnimationPriority
): Int {
    val baseDuration = when {
        distance < 10f -> 200
        distance < 50f -> 500
        speed > 50.0 -> 800
        else -> 1000
    }
    
    // Adjust duration based on priority
    val priorityMultiplier = when (priority) {
        AnimationPriority.CRITICAL -> 0.8f // Faster animations for critical trains
        AnimationPriority.HIGH -> 0.9f
        AnimationPriority.MEDIUM -> 1.0f
        AnimationPriority.LOW -> 1.2f // Slower animations for low priority
        AnimationPriority.NONE -> 0f
    }
    
    return (baseDuration * priorityMultiplier).toInt().coerceAtMost(2000)
}

/**
 * Builds comprehensive content description for accessibility.
 */
private fun buildTrainContentDescription(
    trainState: RealTimeTrainState,
    currentPosition: TrainPosition
): String {
    return buildString {
        append("Train ${trainState.trainId}")
        append(", speed: ${currentPosition.speed} km/h")
        append(", connection: ${trainState.connectionStatus}")
        append(", data quality: ${(trainState.dataQuality?.overallScore?.times(100)?.toInt() ?: 0)}%")
        append(", last update: ${trainState.lastUpdateTime}")
        
        if (trainState.currentPosition?.isSafetyReliable == false) {
            append(", WARNING: Data reliability low")
        }
    }
}

/**
 * Canvas-based train marker with status indicators
 */
@Composable
private fun TrainMarkerCanvas(
    trainState: RealTimeTrainState,
    isMoving: Boolean,
    modifier: Modifier = Modifier
) {
    val trainColor = Color(0xFF2196F3) // Default blue for trains
    val connectionColor = getConnectionStatusColor(trainState.connectionStatus)
    
    // Pulsing animation for moving trains
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = if (isMoving) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 4
        
        // Draw connection status ring
        drawCircle(
            color = connectionColor,
            radius = radius * 1.5f,
            center = center,
            alpha = 0.3f
        )
        
        // Draw main train marker with pulse effect
        drawCircle(
            color = trainColor,
            radius = radius * pulseScale,
            center = center
        )
        
        // Draw direction indicator if moving
        trainState.currentPosition?.let { position ->
            if (isMoving && position.speed > 0.1) {
                drawDirectionIndicator(
                    center = center,
                    radius = radius,
                    heading = position.heading,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Draw direction indicator arrow
 */
private fun DrawScope.drawDirectionIndicator(
    center: Offset,
    radius: Float,
    heading: Double,
    color: Color
) {
    rotate(degrees = heading.toFloat(), pivot = center) {
        val arrowSize = radius * 0.6f
        val arrowTop = Offset(center.x, center.y - arrowSize)
        val arrowLeft = Offset(center.x - arrowSize * 0.5f, center.y + arrowSize * 0.3f)
        val arrowRight = Offset(center.x + arrowSize * 0.5f, center.y + arrowSize * 0.3f)
        
        // Draw arrow triangle
        drawLine(color, arrowTop, arrowLeft, strokeWidth = 2f)
        drawLine(color, arrowTop, arrowRight, strokeWidth = 2f)
        drawLine(color, arrowLeft, arrowRight, strokeWidth = 2f)
    }
}



/**
 * Legacy animation duration calculation (kept for backward compatibility).
 */
private fun calculateAnimationDuration(
    distance: Float,
    speed: Double
): Int {
    return calculateEnhancedAnimationDuration(distance, speed, AnimationPriority.MEDIUM)
}

/**
 * Get color based on train status
 */
private fun getTrainStatusColor(status: TrainStatus): Color {
    return when (status) {
        TrainStatus.ON_TIME -> Color(0xFF4CAF50) // Green
        TrainStatus.DELAYED -> Color(0xFFFF9800) // Orange
        TrainStatus.STOPPED -> Color(0xFFF44336) // Red
        TrainStatus.MAINTENANCE -> Color(0xFF9C27B0) // Purple
        TrainStatus.EMERGENCY -> Color(0xFFD32F2F) // Dark Red
        TrainStatus.CANCELLED -> Color(0xFF757575) // Gray
    }
}

/**
 * Get color based on connection status
 */
private fun getConnectionStatusColor(status: ConnectionStatus): Color {
    return when (status) {
        ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
        ConnectionStatus.DISCONNECTED -> Color(0xFFF44336)
        ConnectionStatus.RECONNECTING -> Color(0xFFFF9800)
        ConnectionStatus.FAILED -> Color(0xFFD32F2F)
        ConnectionStatus.UNKNOWN -> Color(0xFF757575)
    }
}