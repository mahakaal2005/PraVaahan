package com.example.pravaahan.presentation.ui.components.accessibility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pravaahan.core.accessibility.*
import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.presentation.ui.components.TrainStatusChip
import com.example.pravaahan.presentation.ui.components.TrainPriorityChip

/**
 * Accessibility-enhanced train card with comprehensive support for screen readers and touch optimization
 */
@Composable
fun AccessibilityEnhancedTrainCard(
    train: Train,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val accessibilityState = rememberAccessibilityState()
    val screenSizeClass = rememberScreenSizeClass()
    
    val touchTargetSize = if (accessibilityState.isEnabled) 56.dp else 48.dp
    val contentPadding = when (screenSizeClass.widthClass) {
        WindowSizeClass.COMPACT -> 16.dp
        WindowSizeClass.MEDIUM -> 24.dp
        WindowSizeClass.EXPANDED -> 32.dp
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = touchTargetSize)
            .selectable(
                selected = isSelected,
                onClick = onClick
            )
            .semantics {
                contentDescription = "Train ${train.name}, status: ${train.status}, " +
                        "from ${train.currentLocation.name} to ${train.destination.name}"
                role = Role.Button
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        if (screenSizeClass.widthClass == WindowSizeClass.COMPACT) {
            CompactTrainCardContent(
                train = train,
                isSelected = isSelected,
                contentPadding = contentPadding
            )
        } else {
            ExpandedTrainCardContent(
                train = train,
                isSelected = isSelected,
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun CompactTrainCardContent(
    train: Train,
    isSelected: Boolean,
    contentPadding: Dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Train name and status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = train.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        heading()
                        contentDescription = "Train ${train.name}"
                    }
            )
            
            TrainStatusChip(
                status = train.status,
                modifier = Modifier.semantics {
                    contentDescription = "Status: ${train.status}"
                }
            )
        }
        
        // Route information
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Route from ${train.currentLocation.name} to ${train.destination.name}"
                    }
            ) {
                Text(
                    text = "From: ${train.currentLocation.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "To: ${train.destination.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            TrainPriorityChip(
                priority = train.priority,
                modifier = Modifier.semantics {
                    contentDescription = "Priority: ${train.priority}"
                }
            )
        }
        
        // Estimated arrival time
        Text(
            text = "ETA: ${train.estimatedArrival}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = "Estimated arrival time: ${train.estimatedArrival}"
            }
        )
    }
}

@Composable
private fun ExpandedTrainCardContent(
    train: Train,
    isSelected: Boolean,
    contentPadding: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Train information column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = train.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.semantics {
                    heading()
                    contentDescription = "Train ${train.name}"
                }
            )
            
            Text(
                text = "${train.currentLocation.name} → ${train.destination.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "Route from ${train.currentLocation.name} to ${train.destination.name}"
                }
            )
            
            Text(
                text = "ETA: ${train.estimatedArrival}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "Estimated arrival time: ${train.estimatedArrival}"
                }
            )
        }
        
        // Status and priority column
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrainStatusChip(
                status = train.status,
                modifier = Modifier.semantics {
                    contentDescription = "Status: ${train.status}"
                }
            )
            
            TrainPriorityChip(
                priority = train.priority,
                modifier = Modifier.semantics {
                    contentDescription = "Priority: ${train.priority}"
                }
            )
        }
    }
}

/**
 * Accessibility-enhanced train list with optimized scrolling and selection
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AccessibilityEnhancedTrainList(
    trains: List<Train>,
    selectedTrainId: String?,
    onTrainSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = 16.dp
    
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Train list with ${trains.size} trains"
            },
        verticalArrangement = Arrangement.spacedBy(spacing),
        contentPadding = PaddingValues(vertical = spacing)
    ) {
        items(
            items = trains,
            key = { it.id }
        ) { train ->
            AccessibilityEnhancedTrainCard(
                train = train,
                onClick = { onTrainSelected(train.id) },
                isSelected = train.id == selectedTrainId,
                modifier = Modifier.animateItemPlacement()
            )
        }
        
        // Add extra space at the bottom for accessibility
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}