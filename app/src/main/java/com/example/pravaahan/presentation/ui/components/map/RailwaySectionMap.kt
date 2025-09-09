package com.example.pravaahan.presentation.ui.components.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pravaahan.BuildConfig
import com.example.pravaahan.core.logging.Logger

import com.example.pravaahan.domain.model.RailwaySectionConfig
import com.example.pravaahan.domain.model.RealTimeTrainState
import com.example.pravaahan.presentation.ui.components.AnimatedTrainMarker
import javax.inject.Inject
import kotlin.system.measureTimeMillis

/**
 * Main railway section map composable with Canvas-based rendering and comprehensive logging
 */
/**
 * Main railway section map composable with Canvas-based rendering and comprehensive logging
 */
@Composable
fun RailwaySectionMap(
    trainStates: List<RealTimeTrainState>,
    sectionConfig: RailwaySectionConfig,
    onTrainSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isHighContrast: Boolean = false
) {
    val viewModel: RailwayMapViewModel = hiltViewModel()
    val renderer = viewModel.renderer
    val interactionHandler = viewModel.interactionHandler
    val logger = viewModel.logger
    
    // State management with persistence across configuration changes
    val mapState = rememberSaveable(
        saver = MapState.Saver
    ) { MapState(config = sectionConfig) }
    
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    // Track performance metrics
    var lastDrawTime by remember { mutableStateOf(0L) }
    var memoryUsage by remember { mutableStateOf(0L) }
    
    // Initialize map on first composition
    LaunchedEffect(sectionConfig) {
        logger.info("RailwaySectionMap", "Initializing railway section map for section ${sectionConfig.sectionId} with ${sectionConfig.tracks.size} tracks")
        mapState.fitToContent()
    }
    
    // Monitor memory usage
    LaunchedEffect(trainStates.size) {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        memoryUsage = usedMemory / (1024 * 1024) // Convert to MB
        
        logger.debug("RailwaySectionMap", "Memory usage: ${memoryUsage}MB")
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .semantics {
                contentDescription = "Railway section map showing ${trainStates.size} trains on ${sectionConfig.name}"
            }
    ) {
        // Main map canvas with optimized rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer(
                    compositingStrategy = CompositingStrategy.Offscreen // Clip content to bounds
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Simple tap handling for now
                        logger.debug("RailwaySectionMap", "Map tapped at: ($offset)")
                    }
                }
        ) {
            // Update canvas size in map state
            mapState.canvasSize = size
            
            // Measure drawing performance
            val drawTime = measureTimeMillis {
                // Render railway infrastructure
                renderer.renderRailwaySection(
                    drawScope = this,
                    config = sectionConfig,
                    mapState = mapState,
                    textMeasurer = textMeasurer,
                    isHighContrast = isHighContrast
                )
            }
            
            lastDrawTime = drawTime
            
            // Log performance metrics
            logger.debug("RailwaySectionMap", "Canvas draw cycle: ${drawTime}ms, memory usage: ${memoryUsage}MB")
            
            if (drawTime > 16) { // More than one frame at 60fps
                logger.warn("RailwaySectionMap", "Slow canvas draw detected: ${drawTime}ms")
            }
        }
        
        // Animated train markers overlay
        trainStates.forEach { trainState ->
            AnimatedTrainMarker(
                trainState = trainState,
                mapState = mapState,
                onTrainSelected = onTrainSelected,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Map controls overlay
        MapControls(
            mapState = mapState,
            onResetView = {
                logger.info("RailwaySectionMap", "Map view reset requested")
                mapState.reset()
            },
            onFitToContent = {
                logger.info("RailwaySectionMap", "Fit to content requested")
                mapState.fitToContent()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
        
        // Performance overlay (debug mode)
        if (BuildConfig.DEBUG) {
            PerformanceOverlay(
                renderTime = lastDrawTime,
                memoryUsage = memoryUsage,
                trainCount = trainStates.size,
                zoom = mapState.zoom,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }
    }
    
    // Log map state changes
    LaunchedEffect(mapState.zoom, mapState.panOffset) {
        logger.debug("RailwaySectionMap", "Map state changed: zoom=${mapState.zoom}, pan=(${mapState.panOffset.x}, ${mapState.panOffset.y})")
    }
}

/**
 * Extension function to apply interaction handler
 */
private fun Modifier.with(
    handler: MapInteractionHandler,
    block: MapInteractionHandler.() -> Modifier
): Modifier = this.then(handler.block())

/**
 * Debug performance overlay
 */
@Composable
private fun PerformanceOverlay(
    renderTime: Long,
    memoryUsage: Long,
    trainCount: Int,
    zoom: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                MaterialTheme.shapes.small
            )
            .padding(8.dp)
    ) {
        androidx.compose.material3.Text(
            text = "Render: ${renderTime}ms",
            style = MaterialTheme.typography.bodySmall,
            color = if (renderTime > 16) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        androidx.compose.material3.Text(
            text = "Memory: ${memoryUsage}MB",
            style = MaterialTheme.typography.bodySmall
        )
        androidx.compose.material3.Text(
            text = "Trains: $trainCount",
            style = MaterialTheme.typography.bodySmall
        )
        androidx.compose.material3.Text(
            text = "Zoom: ${String.format("%.1f", zoom)}x",
            style = MaterialTheme.typography.bodySmall
        )
    }
}