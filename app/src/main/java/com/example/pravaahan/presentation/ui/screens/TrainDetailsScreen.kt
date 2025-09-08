package com.example.pravaahan.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pravaahan.presentation.ui.components.ConflictAlertCard
import com.example.pravaahan.presentation.ui.components.ErrorMessage
import com.example.pravaahan.presentation.ui.components.LoadingIndicator
import com.example.pravaahan.presentation.ui.components.TrainPriorityChip
import com.example.pravaahan.presentation.ui.components.TrainStatusChip
import com.example.pravaahan.presentation.ui.screens.traindetails.PerformanceMetrics
import com.example.pravaahan.presentation.ui.screens.traindetails.TrainDetailsAction
import com.example.pravaahan.presentation.ui.screens.traindetails.TrainDetailsUiState
import com.example.pravaahan.presentation.ui.screens.traindetails.TrainDetailsViewModel
import com.example.pravaahan.presentation.ui.theme.PravahanTheme
import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.ConflictSeverity
import com.example.pravaahan.domain.model.ConflictType
import com.example.pravaahan.domain.model.Location
import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.model.TrainPriority
import com.example.pravaahan.domain.model.TrainStatus
import kotlinx.datetime.Clock

/**
 * Train details screen - Detailed view of a specific train
 * Shows comprehensive train information, status, and related conflicts
 */
@Composable
fun TrainDetailsScreen(
    trainId: String,
    onBackClick: () -> Unit,
    onConflictClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(trainId) {
        viewModel.handleAction(TrainDetailsAction.LoadTrainDetails(trainId))
    }
    
    TrainDetailsContent(
        uiState = uiState,
        onAction = viewModel::handleAction,
        onBackClick = onBackClick,
        onConflictClick = onConflictClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainDetailsContent(
    uiState: TrainDetailsUiState,
    onAction: (TrainDetailsAction) -> Unit,
    onBackClick: () -> Unit,
    onConflictClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.train?.name ?: "Train Details",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.train != null) {
                            Text(
                                text = "Train ${uiState.train.trainNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.train == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            
            uiState.error != null && uiState.train == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorMessage(
                        message = uiState.error,
                        onRetry = { onAction(TrainDetailsAction.Retry) }
                    )
                }
            }
            
            uiState.train != null -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { onAction(TrainDetailsAction.RefreshData) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Error message if there's an error but we have data
                        if (uiState.error != null) {
                            item {
                                ErrorMessage(
                                    message = uiState.error,
                                    onRetry = { onAction(TrainDetailsAction.Retry) }
                                )
                            }
                        }
                        
                        // Train Status Overview
                        item {
                            TrainStatusOverviewCard(train = uiState.train)
                        }
                        
                        // Location and Movement
                        item {
                            LocationCard(train = uiState.train)
                        }
                        
                        // Performance Metrics
                        item {
                            PerformanceCard(
                                train = uiState.train,
                                performanceMetrics = uiState.performanceMetrics
                            )
                        }
                        
                        // Related Conflicts
                        if (uiState.relatedConflicts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Related Conflicts",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            items(
                                items = uiState.relatedConflicts,
                                key = { it.id }
                            ) { conflict ->
                                ConflictAlertCard(
                                    conflict = conflict,
                                    onAcceptRecommendation = { /* TODO: Handle accept */ },
                                    onOverride = { onConflictClick(conflict.id) }
                                )
                            }
                        }
                        
                        // Bottom spacing
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainStatusOverviewCard(
    train: Train,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Status Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrainStatusChip(status = train.status)
                TrainPriorityChip(priority = train.priority)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    label = "Current Speed",
                    value = "${train.speed.toInt()} km/h",
                    icon = Icons.Default.LocationOn
                )
                InfoItem(
                    label = "ETA",
                    value = "12:45 PM", // TODO: Format estimatedArrival
                    icon = Icons.Default.LocationOn
                )
            }
        }
    }
}

@Composable
private fun LocationCard(
    train: Train,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Location & Route",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LocationItem(
                    label = "Current Location",
                    value = "Section ${train.currentLocation.sectionId}",
                    coordinates = "${train.currentLocation.latitude}, ${train.currentLocation.longitude}"
                )
                LocationItem(
                    label = "Destination",
                    value = "Section ${train.destination.sectionId}",
                    coordinates = "${train.destination.latitude}, ${train.destination.longitude}"
                )
                LocationItem(
                    label = "Remaining Distance",
                    value = "${train.remainingDistance().toInt()} km",
                    coordinates = null
                )
            }
        }
    }
}

@Composable
private fun PerformanceCard(
    train: Train,
    performanceMetrics: PerformanceMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "On-Time %", 
                    value = "${performanceMetrics.onTimePercentage.toInt()}%"
                )
                MetricItem(
                    label = "Avg Speed", 
                    value = "${performanceMetrics.averageSpeed.toInt()} km/h"
                )
                MetricItem(
                    label = "Fuel Eff.", 
                    value = performanceMetrics.fuelEfficiency
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LocationItem(
    label: String,
    value: String,
    coordinates: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        if (coordinates != null) {
            Text(
                text = coordinates,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

// Sample data for preview
private val sampleTrain = Train(
    id = "train-001",
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
)

private val sampleConflicts = listOf(
    ConflictAlert(
        id = "conflict-001",
        trainsInvolved = listOf("train-001", "train-002"),
        conflictType = ConflictType.POTENTIAL_COLLISION,
        severity = ConflictSeverity.HIGH,
        detectedAt = Clock.System.now(),
        estimatedImpactTime = Clock.System.now(),
        recommendation = "Reduce speed to 60 km/h and maintain safe distance."
    )
)


@Preview(showBackground = true)
@Composable
private fun TrainDetailsScreenPreview() {
    PravahanTheme {
        TrainDetailsContent(
            uiState = TrainDetailsUiState(
                train = sampleTrain,
                relatedConflicts = sampleConflicts,
                performanceMetrics = PerformanceMetrics(
                    onTimePercentage = 94.0,
                    averageSpeed = 85.0,
                    fuelEfficiency = "Good",
                    lastUpdated = System.currentTimeMillis()
                )
            ),
            onAction = { },
            onBackClick = { },
            onConflictClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainDetailsScreenLoadingPreview() {
    PravahanTheme {
        TrainDetailsContent(
            uiState = TrainDetailsUiState(isLoading = true),
            onAction = { },
            onBackClick = { },
            onConflictClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainDetailsScreenErrorPreview() {
    PravahanTheme {
        TrainDetailsContent(
            uiState = TrainDetailsUiState(error = "Train not found"),
            onAction = { },
            onBackClick = { },
            onConflictClick = { }
        )
    }
}