package com.example.pravaahan.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.ConflictSeverity
import com.example.pravaahan.domain.model.ConflictType
import com.example.pravaahan.domain.model.Location
import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.model.TrainPriority
import com.example.pravaahan.domain.model.TrainStatus
import com.example.pravaahan.presentation.ui.theme.PravahanTheme
import com.example.pravaahan.presentation.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * A showcase screen demonstrating the Material Design 3 theme implementation
 * Shows all components with proper theming and color usage
 */
@Composable
fun ThemeShowcase(
    themeManager: ThemeManager,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = false)

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header with theme toggle
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PraVaahan Theme",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Material Design 3 Implementation",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                scope.launch {
                                    themeManager.toggleTheme()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Toggle theme",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Typography Scale",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Display Large",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "Headline Large",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Title Large",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Body Large - This is the main body text used throughout the application for readable content.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Label Large",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Status Components",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Train Status Chips",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TrainStatusChip(status = TrainStatus.ON_TIME)
                            TrainStatusChip(status = TrainStatus.DELAYED)
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TrainStatusChip(status = TrainStatus.STOPPED)
                            TrainStatusChip(status = TrainStatus.EMERGENCY)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Priority Chips",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TrainPriorityChip(priority = TrainPriority.EXPRESS)
                            TrainPriorityChip(priority = TrainPriority.HIGH)
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TrainPriorityChip(priority = TrainPriority.MEDIUM)
                            TrainPriorityChip(priority = TrainPriority.LOW)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Train Cards",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(sampleTrains) { train ->
                TrainCard(
                    train = train,
                    onClick = { }
                )
            }

            item {
                Text(
                    text = "Conflict Alert",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                ConflictAlertCard(
                    conflict = sampleConflict,
                    onAcceptRecommendation = { },
                    onOverride = { }
                )
            }

            item {
                Text(
                    text = "Buttons & Actions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Primary Action")
                            }
                            
                            OutlinedButton(
                                onClick = { },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Secondary")
                            }
                        }
                        
                        LoadingIndicator(
                            message = "Loading train data...",
                            modifier = Modifier.height(100.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private val sampleTrains = listOf(
    Train(
        id = "1",
        name = "Rajdhani Express",
        trainNumber = "12001",
        currentLocation = Location(28.6139, 77.2090, "DEL-001"),
        destination = Location(19.0760, 72.8777, "BOM-001"),
        status = TrainStatus.ON_TIME,
        priority = TrainPriority.EXPRESS,
        speed = 120.0,
        estimatedArrival = Clock.System.now(),
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    ),
    Train(
        id = "2",
        name = "Shatabdi Express",
        trainNumber = "12002",
        currentLocation = Location(28.6139, 77.2090, "DEL-002"),
        destination = Location(26.9124, 75.7873, "JAI-001"),
        status = TrainStatus.DELAYED,
        priority = TrainPriority.HIGH,
        speed = 85.0,
        estimatedArrival = Clock.System.now(),
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
)

private val sampleConflict = ConflictAlert(
    id = "conflict-001",
    trainsInvolved = listOf("12001", "12002"),
    conflictType = ConflictType.POTENTIAL_COLLISION,
    severity = ConflictSeverity.HIGH,
    detectedAt = Clock.System.now(),
    estimatedImpactTime = Clock.System.now(),
    recommendation = "Reduce speed of Train 12001 to 60 km/h and hold Train 12002 at next signal."
)

@Preview(showBackground = true)
@Composable
private fun ThemeShowcasePreview() {
    PravahanTheme {
        // Note: Preview won't show theme switching functionality
        // as it requires Hilt injection
    }
}