package com.example.pravaahan.presentation.ui.components.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Optimized renderer for railway section maps with performance monitoring
 */
@Singleton
class RailwayRenderer @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "MapRenderer"
        
        // Railway industry standard dimensions (in dp converted to px)
        private const val TRACK_WIDTH = 4f
        private const val STATION_SIZE = 16f
        private const val SIGNAL_SIZE = 12f
        private const val TEXT_SIZE = 12f
        
        // Railway industry standard colors
        private val TRACK_COLOR = Color(0xFF424242)
        private val STATION_COLOR = Color(0xFF1976D2)
        private val JUNCTION_COLOR = Color(0xFFFF6F00)
        private val DEPOT_COLOR = Color(0xFF388E3C)
    }
    
    private var lastRenderTime = 0L
    private var elementCount = 0
    
    /**
     * Render complete railway section with performance monitoring
     */
    fun renderRailwaySection(
        drawScope: DrawScope,
        config: RailwaySectionConfig,
        mapState: MapState,
        textMeasurer: TextMeasurer,
        isHighContrast: Boolean = false
    ) {
        val renderTime = measureTimeMillis {
            elementCount = 0
            
            with(drawScope) {
                // Apply zoom and pan transformations
                translate(mapState.panOffset.x, mapState.panOffset.y) {
                    // Draw tracks first (background layer)
                    renderTracks(this, config.tracks, mapState, isHighContrast)
                    
                    // Draw stations (middle layer)
                    renderStations(this, config.stations, mapState, textMeasurer, isHighContrast)
                    
                    // Draw signals (foreground layer)
                    renderSignals(this, config.signals, mapState, isHighContrast)
                }
            }
        }
        
        lastRenderTime = renderTime
        
        // Log performance metrics
        logger.debug(TAG, "Map render completed in ${renderTime}ms, drew $elementCount elements")
        
        if (renderTime > 16) { // More than one frame at 60fps
            logger.warn(TAG, "Slow render detected: ${renderTime}ms for $elementCount elements")
        }
    }
    
    /**
     * Render railway tracks with industry-standard styling
     */
    private fun renderTracks(
        drawScope: DrawScope,
        tracks: List<Track>,
        mapState: MapState,
        isHighContrast: Boolean
    ) {
        val trackColor = if (isHighContrast) Color.Black else TRACK_COLOR
        val trackWidth = TRACK_WIDTH * mapState.zoom
        
        tracks.forEach { track ->
            val startScreen = mapState.railwayToScreen(track.startPosition)
            val endScreen = mapState.railwayToScreen(track.endPosition)
            
            // Only draw if track is visible on screen
            if (isTrackVisible(startScreen, endScreen, drawScope.size)) {
                drawScope.drawLine(
                    color = trackColor,
                    start = startScreen,
                    end = endScreen,
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round
                )
                
                // Draw track ties for better visual representation
                if (mapState.zoom > 1.5f) {
                    drawTrackTies(drawScope, startScreen, endScreen, trackColor, mapState.zoom)
                }
                
                elementCount++
            }
        }
        
        logger.debug(TAG, "Rendered ${tracks.size} tracks with zoom ${mapState.zoom}")
    }
    
    /**
     * Render railway stations with accessibility support
     */
    private fun renderStations(
        drawScope: DrawScope,
        stations: List<Station>,
        mapState: MapState,
        textMeasurer: TextMeasurer,
        isHighContrast: Boolean
    ) {
        stations.forEach { station ->
            val screenPos = mapState.railwayToScreen(station.position)
            
            // Only draw if station is visible
            if (isPointVisible(screenPos, drawScope.size)) {
                val stationColor = getStationColor(station.type, isHighContrast)
                val stationSize = STATION_SIZE * mapState.zoom
                
                // Draw station symbol
                when (station.type) {
                    StationType.MAJOR -> drawMajorStation(drawScope, screenPos, stationSize, stationColor)
                    StationType.REGULAR -> drawRegularStation(drawScope, screenPos, stationSize, stationColor)
                    StationType.JUNCTION -> drawJunctionStation(drawScope, screenPos, stationSize, stationColor)
                    StationType.DEPOT -> drawDepotStation(drawScope, screenPos, stationSize, stationColor)
                }
                
                // Draw station label if zoom level is sufficient
                if (mapState.zoom > 1.2f) {
                    drawStationLabel(drawScope, screenPos, station.name, textMeasurer, isHighContrast)
                }
                
                elementCount++
            }
        }
        
        logger.debug(TAG, "Rendered ${stations.size} stations")
    }
    
    /**
     * Render railway signals with status indication
     */
    private fun renderSignals(
        drawScope: DrawScope,
        signals: List<Signal>,
        mapState: MapState,
        isHighContrast: Boolean
    ) {
        signals.forEach { signal ->
            val screenPos = mapState.railwayToScreen(signal.position)
            
            if (isPointVisible(screenPos, drawScope.size)) {
                val signalSize = SIGNAL_SIZE * mapState.zoom
                val signalColor = if (isHighContrast) Color.Black else signal.status.color
                
                // Draw signal based on type
                when (signal.type) {
                    SignalType.HOME -> drawHomeSignal(drawScope, screenPos, signalSize, signalColor)
                    SignalType.DISTANT -> drawDistantSignal(drawScope, screenPos, signalSize, signalColor)
                    SignalType.SHUNT -> drawShuntSignal(drawScope, screenPos, signalSize, signalColor)
                    SignalType.AUTOMATIC -> drawAutomaticSignal(drawScope, screenPos, signalSize, signalColor)
                }
                
                elementCount++
            }
        }
        
        logger.debug(TAG, "Rendered ${signals.size} signals")
    }
    
    // Station drawing methods
    private fun drawMajorStation(drawScope: DrawScope, position: Offset, size: Float, color: Color) {
        drawScope.drawRect(
            color = color,
            topLeft = Offset(position.x - size/2, position.y - size/2),
            size = Size(size, size),
            style = Stroke(width = 2f)
        )
        drawScope.drawRect(
            color = color,
            topLeft = Offset(position.x - size/4, position.y - size/4),
            size = Size(size/2, size/2)
        )
    }
    
    private fun drawRegularStation(drawScope: DrawScope, position: Offset, size: Float, color: Color) {
        drawScope.drawCircle(
            color = color,
            radius = size/2,
            center = position,
            style = Stroke(width = 2f)
        )
        drawScope.drawCircle(
            color = color,
            radius = size/4,
            center = position
        )
    }
    
    private fun drawJunctionStation(drawScope: DrawScope, position: Offset, size: Float, color: Color) {
        val path = Path().apply {
            moveTo(position.x, position.y - size/2)
            lineTo(position.x + size/2, position.y + size/2)
            lineTo(position.x - size/2, position.y + size/2)
            close()
        }
        drawScope.drawPath(path, color, style = Stroke(width = 2f))
        drawScope.drawPath(path, color.copy(alpha = 0.3f))
    }
    
    private fun drawDepotStation(drawScope: DrawScope, position: Offset, size: Float, color: Color) {
        drawScope.drawRect(
            color = color,
            topLeft = Offset(position.x - size/2, position.y - size/2),
            size = Size(size, size),
            style = Stroke(width = 2f)
        )
        // Draw depot symbol (house-like shape)
        val roofPath = Path().apply {
            moveTo(position.x - size/3, position.y - size/6)
            lineTo(position.x, position.y - size/3)
            lineTo(position.x + size/3, position.y - size/6)
        }
        drawScope.drawPath(roofPath, color, style = Stroke(width = 1f))
    }
    
    // Signal drawing methods
    private fun drawHomeSignal(drawScope: DrawScope, position: Offset, size: Float, color: Color) {
        drawScope.drawCircle(color = color, radius = size/2, center = position)
        drawScope.drawCircle(
            color = Color.White,
            radius = size/2,
            center = position,
            style = Stroke(width = 1f)
        )
    }
    
    private fun drawDistantSignal(drawScope: DrawScope, position: Offset, size: Float, color: Color) {
        val path = Path().apply {
            moveTo(position.x - size/2, position.y)
            lineTo(position.x, position.y - size/2)
            lineTo(position.x + size/2, position.y)
            lineTo(position.x, position.y + size/2)
            close()
        }
        drawScope.drawPath(path, color)
        drawScope.drawPath(path, Color.White, style = Stroke(width = 1f))
    }
    
    private fun drawShuntSignal(drawScope: DrawScope, position: Offset, size: Float, color: Color) {
        drawScope.drawRect(
            color = color,
            topLeft = Offset(position.x - size/3, position.y - size/3),
            size = Size(size * 2/3, size * 2/3)
        )
    }
    
    private fun drawAutomaticSignal(drawScope: DrawScope, position: Offset, size: Float, color: Color) {
        drawScope.drawCircle(color = color, radius = size/3, center = position)
        drawScope.drawCircle(
            color = Color.White,
            radius = size/3,
            center = position,
            style = Stroke(width = 1f)
        )
    }
    
    // Helper methods
    private fun drawTrackTies(
        drawScope: DrawScope,
        start: Offset,
        end: Offset,
        color: Color,
        zoom: Float
    ) {
        val tieSpacing = 20f * zoom
        val tieLength = 8f * zoom
        val distance = kotlin.math.sqrt(
            (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
        )
        
        if (distance > 0) {
            val tieCount = (distance / tieSpacing).toInt()
            val dx = (end.x - start.x) / distance
            val dy = (end.y - start.y) / distance
            
            for (i in 0..tieCount) {
                val t = i * tieSpacing
                val tieCenter = Offset(start.x + dx * t, start.y + dy * t)
                val perpX = -dy * tieLength / 2
                val perpY = dx * tieLength / 2
                
                drawScope.drawLine(
                    color = color.copy(alpha = 0.6f),
                    start = Offset(tieCenter.x + perpX, tieCenter.y + perpY),
                    end = Offset(tieCenter.x - perpX, tieCenter.y - perpY),
                    strokeWidth = 1f
                )
            }
        }
    }
    
    private fun drawStationLabel(
        drawScope: DrawScope,
        position: Offset,
        name: String,
        textMeasurer: TextMeasurer,
        isHighContrast: Boolean
    ) {
        val textColor = if (isHighContrast) Color.Black else Color(0xFF212121)
        val textStyle = TextStyle(
            color = textColor,
            fontSize = (TEXT_SIZE).sp,
            fontWeight = FontWeight.Medium
        )
        
        drawScope.drawText(
            textMeasurer = textMeasurer,
            text = name,
            topLeft = Offset(position.x + STATION_SIZE, position.y - TEXT_SIZE/2),
            style = textStyle
        )
    }
    
    private fun getStationColor(type: StationType, isHighContrast: Boolean): Color {
        if (isHighContrast) return Color.Black
        
        return when (type) {
            StationType.MAJOR -> STATION_COLOR
            StationType.REGULAR -> STATION_COLOR
            StationType.JUNCTION -> JUNCTION_COLOR
            StationType.DEPOT -> DEPOT_COLOR
        }
    }
    
    private fun isTrackVisible(start: Offset, end: Offset, canvasSize: Size): Boolean {
        return !(start.x < 0 && end.x < 0) &&
               !(start.x > canvasSize.width && end.x > canvasSize.width) &&
               !(start.y < 0 && end.y < 0) &&
               !(start.y > canvasSize.height && end.y > canvasSize.height)
    }
    
    private fun isPointVisible(point: Offset, canvasSize: Size): Boolean {
        return point.x >= -50 && point.x <= canvasSize.width + 50 &&
               point.y >= -50 && point.y <= canvasSize.height + 50
    }
    
    /**
     * Get last render performance metrics
     */
    fun getLastRenderMetrics(): RenderMetrics {
        return RenderMetrics(
            renderTime = lastRenderTime,
            elementCount = elementCount
        )
    }
}

/**
 * Performance metrics for map rendering
 */
data class RenderMetrics(
    val renderTime: Long,
    val elementCount: Int
)