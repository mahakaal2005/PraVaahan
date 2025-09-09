package com.example.pravaahan.presentation.ui.components.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Map control buttons for zoom, pan, and reset operations
 */
@Composable
fun MapControls(
    mapState: MapState,
    onResetView: () -> Unit,
    onFitToContent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom controls
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Zoom in button
                FilledIconButton(
                    onClick = {
                        mapState.updateZoom(mapState.zoom * 1.2f)
                    },
                    modifier = Modifier
                        .size(40.dp)
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
                FilledIconButton(
                    onClick = {
                        mapState.updateZoom(mapState.zoom / 1.2f)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .semantics {
                            contentDescription = "Zoom out on railway map"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = null
                    )
                }
            }
        }
        
        // Navigation controls
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Fit to content button
                FilledIconButton(
                    onClick = onFitToContent,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics {
                            contentDescription = "Fit railway section to view"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null
                    )
                }
                
                // Reset view button
                FilledIconButton(
                    onClick = onResetView,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics {
                            contentDescription = "Reset map view to default"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null
                    )
                }
            }
        }
        
        // Zoom level indicator
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "${String.format("%.1f", mapState.zoom)}x",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}