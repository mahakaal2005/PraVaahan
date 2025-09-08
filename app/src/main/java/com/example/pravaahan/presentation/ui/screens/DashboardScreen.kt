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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import com.example.pravaahan.presentation.ui.components.TrainCard
import com.example.pravaahan.presentation.ui.screens.dashboard.DashboardAction
import com.example.pravaahan.presentation.ui.screens.dashboard.DashboardUiState
import com.example.pravaahan.presentation.ui.screens.dashboard.DashboardViewModel
import com.example.pravaahan.presentation.ui.screens.dashboard.SystemStatus
import com.example.pravaahan.presentation.ui.theme.PravahanTheme
import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.ConflictSeverity
import com.example.pravaahan.domain.model.ConflictType
import com.example.pravaahan.domain.model.Location
import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.model.TrainPriority
import com.example.pravaahan.domain.model.TrainStatus
import kotlinx.datetime.Clock

// Add logging
private const val TAG = "DashboardScreen"

/**
 * Dashboard screen - Main overview of all trains and system status
 * Shows active trains, conflicts, and system health indicators
 */
@Composable
fun DashboardScreen(
    onTrainClick: (String) -> Unit,
    onConflictClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // Debug logging
    android.util.Log.d(TAG, "DashboardScreen composable called")
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Log UI state changes
    android.util.Log.d(TAG, "UI State: isLoading=${uiState.isLoading}, trains=${uiState.trains.size}, error=${uiState.error}")
    
    DashboardContent(
        uiState = uiState,
        onAction = viewModel::handleAction,
        onTrainClick = onTrainClick,
        onConflictClick = onConflictClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    onTrainClick: (String) -> Unit,
    onConflictClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Debug logging
    android.util.Log.d(TAG, "DashboardContent rendering - isLoading: ${uiState.isLoading}")
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "PraVaahan Control",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(DashboardAction.RefreshData) }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh data"
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.trains.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            
            uiState.error != null && uiState.trains.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorMessage(
                        message = uiState.error,
                        onRetry = { onAction(DashboardAction.Retry) }
                    )
                }
            }
            
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { onAction(DashboardAction.RefreshData) },
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
                        // System Status Section
                        item {
                            SystemStatusCard(systemStatus = uiState.systemStatus)
                        }
                        
                        // Error message if there's an error but we have data
                        if (uiState.error != null) {
                            item {
                                ErrorMessage(
                                    message = uiState.error,
                                    onRetry = { onAction(DashboardAction.Retry) }
                                )
                            }
                        }
                        
                        // Active Conflicts Section
                        if (uiState.conflicts.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Active Conflicts",
                                    subtitle = "${uiState.conflicts.size} conflicts require attention"
                                )
                            }
                            
                            items(
                                items = uiState.conflicts,
                                key = { it.id }
                            ) { conflict ->
                                ConflictAlertCard(
                                    conflict = conflict,
                                    onAcceptRecommendation = { /* TODO: Handle accept */ },
                                    onOverride = { onConflictClick(conflict.id) }
                                )
                            }
                        }
                        
                        // Active Trains Section
                        item {
                            SectionHeader(
                                title = "Active Trains",
                                subtitle = if (uiState.trains.isNotEmpty()) {
                                    "${uiState.trains.size} trains in operation"
                                } else {
                                    "No trains currently active"
                                }
                            )
                        }
                        
                        if (uiState.trains.isNotEmpty()) {
                            items(
                                items = uiState.trains,
                                key = { it.id }
                            ) { train ->
                                TrainCard(
                                    train = train,
                                    onClick = { onTrainClick(train.id) }
                                )
                            }
                        } else {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No active trains",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemStatusCard(
    systemStatus: SystemStatus,
    modifier: Modifier = Modifier
) {
    val allSystemsHealthy = systemStatus.supabaseConnected && 
                           systemStatus.realtimeActive && 
                           systemStatus.aiEngineOnline
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allSystemsHealthy) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (allSystemsHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "System Status",
                    tint = if (allSystemsHealthy) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "System Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (allSystemsHealthy) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem(
                    label = "Supabase", 
                    status = if (systemStatus.supabaseConnected) "Connected" else "Disconnected",
                    isHealthy = systemStatus.supabaseConnected
                )
                StatusItem(
                    label = "Real-time", 
                    status = if (systemStatus.realtimeActive) "Active" else "Inactive",
                    isHealthy = systemStatus.realtimeActive
                )
                StatusItem(
                    label = "AI Engine", 
                    status = if (systemStatus.aiEngineOnline) "Online" else "Offline",
                    isHealthy = systemStatus.aiEngineOnline
                )
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    status: String,
    isHealthy: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (isHealthy) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onErrorContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isHealthy) 
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            else 
                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

// Sample data for preview and placeholder functionality
private val sampleTrains = listOf(
    Train(
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
    ),
    Train(
        id = "train-002",
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

private val sampleConflicts = listOf(
    ConflictAlert(
        id = "conflict-001",
        trainsInvolved = listOf("train-001", "train-002"),
        conflictType = ConflictType.POTENTIAL_COLLISION,
        severity = ConflictSeverity.HIGH,
        detectedAt = Clock.System.now(),
        estimatedImpactTime = Clock.System.now(),
        recommendation = "Reduce speed of Train 12001 to 60 km/h and hold Train 12002 at next signal."
    )
)

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    PravahanTheme {
        DashboardContent(
            uiState = DashboardUiState(
                trains = sampleTrains,
                conflicts = sampleConflicts,
                systemStatus = SystemStatus(
                    supabaseConnected = true,
                    realtimeActive = true,
                    aiEngineOnline = true,
                    lastUpdated = System.currentTimeMillis()
                )
            ),
            onAction = { },
            onTrainClick = { },
            onConflictClick = { },
            onSettingsClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenLoadingPreview() {
    PravahanTheme {
        DashboardContent(
            uiState = DashboardUiState(isLoading = true),
            onAction = { },
            onTrainClick = { },
            onConflictClick = { },
            onSettingsClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenErrorPreview() {
    PravahanTheme {
        DashboardContent(
            uiState = DashboardUiState(error = "Failed to connect to server"),
            onAction = { },
            onTrainClick = { },
            onConflictClick = { },
            onSettingsClick = { }
        )
    }
}