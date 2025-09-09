package com.example.pravaahan.presentation.ui.components.map

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.pravaahan.domain.model.MapBounds
import com.example.pravaahan.domain.model.RailwaySectionConfig
import kotlin.math.max
import kotlin.math.min

/**
 * State holder for railway section map with zoom and pan capabilities
 */
@Stable
class MapState(
    initialZoom: Float = 1f,
    initialPanOffset: Offset = Offset.Zero,
    private val config: RailwaySectionConfig
) {
    // Zoom constraints for railway maps
    companion object {
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 5.0f
        
        val Saver: Saver<MapState, *> = listSaver(
            save = { listOf(it.zoom, it.panOffset.x, it.panOffset.y) },
            restore = { values ->
                MapState(
                    initialZoom = values[0] as Float,
                    initialPanOffset = Offset(values[1] as Float, values[2] as Float),
                    config = RailwaySectionConfig("", "", emptyList(), emptyList(), emptyList(), MapBounds(0f, 0f, 0f, 0f))
                )
            }
        )
    }
    
    var zoom by mutableStateOf(initialZoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set
    
    var panOffset by mutableStateOf(initialPanOffset)
        private set
    
    var canvasSize by mutableStateOf(Size.Zero)
        internal set
    
    /**
     * Update zoom level with constraints
     */
    fun updateZoom(newZoom: Float) {
        zoom = newZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }
    
    /**
     * Update pan offset with bounds checking
     */
    fun updatePanOffset(newOffset: Offset) {
        if (canvasSize != Size.Zero) {
            val bounds = calculatePanBounds()
            panOffset = Offset(
                x = newOffset.x.coerceIn(bounds.minX, bounds.maxX),
                y = newOffset.y.coerceIn(bounds.minY, bounds.maxY)
            )
        } else {
            panOffset = newOffset
        }
    }
    
    /**
     * Convert railway coordinates to screen coordinates
     */
    fun railwayToScreen(railwayPosition: Offset): Offset {
        val scaledX = (railwayPosition.x - config.bounds.minX) / config.bounds.width * canvasSize.width
        val scaledY = (railwayPosition.y - config.bounds.minY) / config.bounds.height * canvasSize.height
        
        return Offset(
            x = scaledX * zoom + panOffset.x,
            y = scaledY * zoom + panOffset.y
        )
    }
    
    /**
     * Convert screen coordinates to railway coordinates
     */
    fun screenToRailway(screenPosition: Offset): Offset {
        val adjustedX = (screenPosition.x - panOffset.x) / zoom
        val adjustedY = (screenPosition.y - panOffset.y) / zoom
        
        return Offset(
            x = config.bounds.minX + (adjustedX / canvasSize.width) * config.bounds.width,
            y = config.bounds.minY + (adjustedY / canvasSize.height) * config.bounds.height
        )
    }
    
    /**
     * Check if a screen position is within railway bounds
     */
    fun isPositionInBounds(screenPosition: Offset): Boolean {
        val railwayPos = screenToRailway(screenPosition)
        return railwayPos.x >= config.bounds.minX && 
               railwayPos.x <= config.bounds.maxX &&
               railwayPos.y >= config.bounds.minY && 
               railwayPos.y <= config.bounds.maxY
    }
    
    /**
     * Calculate pan bounds to prevent excessive panning
     */
    private fun calculatePanBounds(): MapBounds {
        val scaledWidth = canvasSize.width * zoom
        val scaledHeight = canvasSize.height * zoom
        
        val maxPanX = max(0f, (scaledWidth - canvasSize.width) / 2)
        val maxPanY = max(0f, (scaledHeight - canvasSize.height) / 2)
        
        return MapBounds(
            minX = -maxPanX,
            maxX = maxPanX,
            minY = -maxPanY,
            maxY = maxPanY
        )
    }
    
    /**
     * Reset map to initial state
     */
    fun reset() {
        zoom = 1f
        panOffset = Offset.Zero
    }
    
    /**
     * Fit map to show all content
     */
    fun fitToContent() {
        if (canvasSize != Size.Zero) {
            val scaleX = canvasSize.width / config.bounds.width
            val scaleY = canvasSize.height / config.bounds.height
            zoom = min(scaleX, scaleY).coerceIn(MIN_ZOOM, MAX_ZOOM)
            panOffset = Offset.Zero
        }
    }
}