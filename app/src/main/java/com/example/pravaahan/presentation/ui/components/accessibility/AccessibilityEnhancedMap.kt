package com.example.pravaahan.presentation.ui.components.accessibility

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.pravaahan.core.accessibility.*
import com.example.pravaahan.domain.model.RailwaySectionConfig
import com.example.pravaahan.domain.model.RealTimeTrainState
import com.example.pravaahan.presentation.ui.components.map.MapState
import com.example.pravaahan.presentation.ui.components.map.RailwayRenderer
import com.example.pravaahan.core.logging.AppLogger

/**
 * Accessibility-enhanced railway section map with comprehensive touch optimization and screen reader support
 */
@Composable
fun AccessibilityEnhancedMap(
    trainStates: List<RealTimeTrainState>,
    sectionConfig: RailwaySectionConfig,
    onTrainSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibilityState = rememberAccessibilityState()
    val screenSizeClass = rememberScreenSizeClass()
    
    // Map state with accessibility considerations
    val mapState = remember(sectionConfig) {
        MapState(config = sectionConfig)
    }
    
    val mapHeight = when {
        screenSizeClass.isLandscape -> 300.dp
        screenSizeClass.heightClass == WindowSizeClass.COMPACT -> 200.dp
        else -> 250.dp
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight)
            .semantics {
                contentDescription = "Railway section map showing ${trainStates.size} trains on ${sectionConfig.name}"
                role = Role.Image
            }
    ) {
        // Main map canvas with accessibility enhancements
        val textMeasurer = rememberTextMeasurer()
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    compositingStrategy = CompositingStrategy.Offscreen
                )
        ) {
            // Render railway section
            val renderer = RailwayRenderer(AppLogger())
            renderer.renderRailwaySection(
                drawScope = this,
                config = sectionConfig,
                mapState = mapState,
                textMeasurer = textMeasurer,
                isHighContrast = false
            )
        }
        
        // Accessibility-enhanced train markers
        trainStates.forEach { trainState ->
            AccessibilityEnhancedTrainMarker(
                trainState = trainState,
                mapState = mapState,
                onTrainSelected = onTrainSelected,
                isAccessibilityEnabled = accessibilityState.isEnabled,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Accessibility controls overlay
        AccessibilityMapControls(
            mapState = mapState,
            trainCount = trainStates.size,
            sectionName = sectionConfig.name,
            isAccessibilityEnabled = accessibilityState.isEnabled,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
        
        // Screen reader announcement area
        if (accessibilityState.isTalkBackEnabled) {
            ScreenReaderAnnouncementArea(
                trainStates = trainStates,
                mapState = mapState,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

/**
 * Accessibility-enhanced train marker with larger touch targets and haptic feedback
 */
@Composable
private fun AccessibilityEnhancedTrainMarker(
    trainState: RealTimeTrainState,
    mapState: MapState,
    onTrainSelected: (String) -> Unit,
    isAccessibilityEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val position = trainState.currentPosition
    if (position == null) return
    
    // Calculate screen position
    val screenPosition = mapState.railwayToScreen(
        androidx.compose.ui.geometry.Offset(
            position.latitude.toFloat(),
            position.longitude.toFloat()
        )
    )
    
    // Enhanced touch target size for accessibility
    val touchTargetSize = if (isAccessibilityEnabled) 56.dp else 48.dp
    
    Box(
        modifier = modifier
            .offset(
                x = screenPosition.x.dp - touchTargetSize / 2,
                y = screenPosition.y.dp - touchTargetSize / 2
            )
            .size(touchTargetSize)
            .semantics {
                contentDescription = "Train ${trainState.train?.id ?: "unknown"} at current position"
                role = Role.Button
            }
    ) {
        // Train marker visual representation
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = when (trainState.train?.status) {
                    com.example.pravaahan.domain.model.TrainStatus.ON_TIME -> MaterialTheme.colorScheme.primary
                    com.example.pravaahan.domain.model.TrainStatus.DELAYED -> MaterialTheme.colorScheme.tertiary
                    com.example.pravaahan.domain.model.TrainStatus.STOPPED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Train,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Accessibility-enhanced map controls with larger touch targets and clear labels
 */
@Composable
private fun AccessibilityMapControls(
    mapState: MapState,
    trainCount: Int,
    sectionName: String,
    isAccessibilityEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val buttonSize = if (isAccessibilityEnabled) 56.dp else 48.dp
    
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zoom in button
        FloatingActionButton(
            onClick = {
                mapState.updateZoom(mapState.zoom * 1.2f)
            },
            modifier = Modifier
                .size(buttonSize)
                .semantics {
                    contentDescription = "Zoom in on railway map"
                }
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = null
            )
        }
        
        // Zoom out button
        FloatingActionButton(
            onClick = {
                mapState.updateZoom(mapState.zoom * 0.8f)
            },
            modifier = Modifier
                .size(buttonSize)
                .semantics {
                    contentDescription = "Zoom out on railway map"
                }
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOut,
                contentDescription = null
            )
        }
        
        // Reset view button
        FloatingActionButton(
            onClick = {
                mapState.reset()
            },
            modifier = Modifier
                .size(buttonSize)
                .semantics {
                    contentDescription = "Reset map view to show all trains"
                }
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = null
            )
        }
        
        // Accessibility info button
        if (isAccessibilityEnabled) {
            FloatingActionButton(
                onClick = {
                    // Announce current map state
                },
                modifier = Modifier
                    .size(buttonSize)
                    .semantics {
                        contentDescription = "Announce current map information: $trainCount trains on $sectionName"
                        liveRegion = LiveRegionMode.Assertive
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Accessibility,
                    contentDescription = null
                )
            }
        }
    }
}

/**
 * Screen reader announcement area for dynamic map updates
 */
@Composable
private fun ScreenReaderAnnouncementArea(
    trainStates: List<RealTimeTrainState>,
    mapState: MapState,
    modifier: Modifier = Modifier
) {
    var lastAnnouncementTime by remember { mutableStateOf(0L) }
    val currentTime = System.currentTimeMillis()
    
    // Announce significant changes every 30 seconds
    LaunchedEffect(trainStates.size, mapState.zoom) {
        if (currentTime - lastAnnouncementTime > 30000) {
            lastAnnouncementTime = currentTime
        }
    }
    
    Box(
        modifier = modifier
            .size(1.dp) // Invisible but accessible
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Map showing ${trainStates.size} trains, " +
                        "zoom level ${String.format("%.1f", mapState.zoom)}x"
            }
    )
}