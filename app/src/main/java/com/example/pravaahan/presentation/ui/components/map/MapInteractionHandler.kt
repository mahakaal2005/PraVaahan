package com.example.pravaahan.presentation.ui.components.map

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.RailwaySectionConfig
import com.example.pravaahan.domain.model.RealTimeTrainState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Handles touch interactions for railway section map with comprehensive logging
 */
@Singleton
class MapInteractionHandler @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "MapInteraction"
        private const val TRAIN_SELECTION_RADIUS = 30f // Touch target size in pixels
        private const val MIN_DRAG_DISTANCE = 10f // Minimum distance to start drag
    }
    
    /**
     * Apply gesture handling to map with comprehensive interaction logging
     */
    @Composable
    fun Modifier.mapGestures(
        mapState: MapState,
        trainStates: List<RealTimeTrainState>,
        config: RailwaySectionConfig,
        onTrainSelected: (String) -> Unit,
        onMapTapped: (Offset) -> Unit = {}
    ): Modifier {
        return this
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        handleTap(offset, mapState, trainStates, config, onTrainSelected, onMapTapped)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        logger.debug(TAG, "Pan gesture started at: ($offset)")
                    },
                    onDrag = { _, dragAmount ->
                        val newOffset = mapState.panOffset + dragAmount
                        mapState.updatePanOffset(newOffset)
                        
                        logger.debug(TAG, "Map panned to: (${mapState.panOffset.x}, ${mapState.panOffset.y})")
                    },
                    onDragEnd = {
                        logger.debug(TAG, "Pan gesture ended at: (${mapState.panOffset.x}, ${mapState.panOffset.y})")
                        logger.info(TAG, "Map pan completed: offset=(${mapState.panOffset.x}, ${mapState.panOffset.y})")
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, pan, zoom, _ ->
                        // Handle zoom
                        val newZoom = mapState.zoom * zoom
                        val oldZoom = mapState.zoom
                        mapState.updateZoom(newZoom)
                        
                        // Handle pan during zoom
                        val newPanOffset = mapState.panOffset + pan
                        mapState.updatePanOffset(newPanOffset)
                        
                        // Log zoom operations
                        if (abs(zoom - 1f) > 0.01f) {
                            logger.debug(TAG, "Map zoom changed from ${oldZoom}x to ${mapState.zoom}x")
                            logger.info(TAG, "Map zoom changed to ${mapState.zoom}x, pan offset: (${mapState.panOffset.x}, ${mapState.panOffset.y})")
                        }
                    }
                )
            }
    }
    
    /**
     * Handle tap gestures with train selection and accessibility logging
     */
    private fun handleTap(
        tapOffset: Offset,
        mapState: MapState,
        trainStates: List<RealTimeTrainState>,
        config: RailwaySectionConfig,
        onTrainSelected: (String) -> Unit,
        onMapTapped: (Offset) -> Unit
    ) {
        logger.debug(TAG, "Tap detected at screen coordinates: (${tapOffset.x}, ${tapOffset.y})")
        
        // Convert to railway coordinates for logging
        val railwayCoords = mapState.screenToRailway(tapOffset)
        logger.debug(TAG, "Tap at railway coordinates: (${railwayCoords.x}, ${railwayCoords.y})")
        
        // Check for train selection first
        val selectedTrain = findTrainAtPosition(tapOffset, trainStates, mapState)
        
        if (selectedTrain != null) {
            logger.info(TAG, "Train selected: ${selectedTrain.trainId} at coordinates (${tapOffset.x}, ${tapOffset.y})")
            
            // Log accessibility event
            val description = "Train ${selectedTrain.trainId}, speed: ${selectedTrain.currentPosition?.speed ?: 0.0} km/h"
            logger.info(TAG, "Screen reader accessed train ${selectedTrain.trainId}: $description")
            
            onTrainSelected(selectedTrain.trainId)
        } else {
            // No train selected, handle general map tap
            logger.debug(TAG, "No train found at tap location, handling general map interaction")
            onMapTapped(tapOffset)
        }
    }
    
    /**
     * Find train at given screen position with hit testing
     */
    private fun findTrainAtPosition(
        screenPosition: Offset,
        trainStates: List<RealTimeTrainState>,
        mapState: MapState
    ): RealTimeTrainState? {
        return trainStates.find { trainState ->
            trainState.currentPosition?.let { position ->
                val trainScreenPos = mapState.railwayToScreen(
                    Offset(
                        position.latitude.toFloat(),
                        position.longitude.toFloat()
                    )
                )
                
                val distance = calculateDistance(screenPosition, trainScreenPos)
                val isWithinRange = distance <= TRAIN_SELECTION_RADIUS
                
                if (isWithinRange) {
                    logger.debug(TAG, "Train ${trainState.trainId} found at distance $distance pixels from tap")
                }
                
                isWithinRange
            } ?: false
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
     * Check if gesture should be treated as drag vs tap
     */
    private fun isDragGesture(startOffset: Offset, currentOffset: Offset): Boolean {
        return calculateDistance(startOffset, currentOffset) > MIN_DRAG_DISTANCE
    }
    
    /**
     * Log performance metrics for gesture handling
     */
    fun logGesturePerformance(gestureType: String, processingTime: Long) {
        logger.debug(TAG, "$gestureType gesture processed in ${processingTime}ms")
        
        if (processingTime > 16) { // More than one frame at 60fps
            logger.warn(TAG, "Slow gesture processing: $gestureType took ${processingTime}ms")
        }
    }
}

/**
 * Gesture types for logging
 */
enum class GestureType {
    TAP,
    DRAG,
    ZOOM,
    TRANSFORM
}