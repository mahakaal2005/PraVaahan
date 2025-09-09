package com.example.pravaahan.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.pravaahan.presentation.ui.components.map.RailwaySectionMap
import com.example.pravaahan.data.sample.SampleRailwayData
import com.example.pravaahan.domain.model.ConnectionState
import com.example.pravaahan.domain.model.DataQualityIndicators
import com.example.pravaahan.domain.model.NetworkQuality
import com.example.pravaahan.domain.model.ValidationStatus
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

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
                        
                        // Real-time Connection Status Bar
                        item {
                            RealTimeConnectionStatusBar(
                                connectionStatus = uiState.connectionStatus,
                                dataQuality = uiState.dataQuality,
                                realTimeModeEnabled = uiState.realTimeModeEnabled,
                                onToggleRealTimeMode = { enabled ->
                                    onAction(DashboardAction.ToggleRealTimeMode(enabled))
                                },
                                onRetryConnection = {
                                    onAction(DashboardAction.RetryConnection)
                                }
                            )
                        }
                        
                        // Real-time Railway Map Section
                        if (uiState.sectionConfig != null) {
                            item {
                                SectionHeader(
                                    title = "Railway Section Map",
                                    subtitle = if (uiState.trainStates.isNotEmpty()) {
                                        "Real-time train positions on ${uiState.sectionConfig.name} (${uiState.trainStates.size} trains)"
                                    } else {
                                        "Railway section: ${uiState.sectionConfig.name}"
                                    }
                                )
                            }
                            
                            item {
                                RealTimeMapCard(
                                    trainStates = uiState.trainStates,
                                    sectionConfig = uiState.sectionConfig,
                                    connectionStatus = uiState.connectionStatus,
                                    isLoading = uiState.isLoading,
                                    error = uiState.error,
                                    onTrainSelected = { trainId ->
                                        onAction(DashboardAction.SelectTrain(trainId))
                                    },
                                    onRetry = {
                                        onAction(DashboardAction.RetryConnection)
                                    }
                                )
                            }
                        }
                        
                        // Train Status Panel
                        if (uiState.trainStates.isNotEmpty()) {
                            item {
                                TrainStatusPanel(
                                    trainStates = uiState.trainStates,
                                    selectedTrainId = uiState.selectedTrainId,
                                    onTrainSelected = { trainId ->
                                        onAction(DashboardAction.SelectTrain(trainId))
                                    }
                                )
                            }
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

/**
 * Real-time connection status bar with clear separation of status and controls
 */
@Composable
private fun RealTimeConnectionStatusBar(
    connectionStatus: ConnectionState,
    dataQuality: DataQualityIndicators?,
    realTimeModeEnabled: Boolean,
    onToggleRealTimeMode: (Boolean) -> Unit,
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    
    val connectionColor by animateColorAsState(
        targetValue = when (connectionStatus) {
            ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
            ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.tertiary
            ConnectionState.DISCONNECTED, ConnectionState.FAILED -> MaterialTheme.colorScheme.error
            ConnectionState.UNKNOWN -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(300),
        label = "connection_color"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 200.dp)
            .semantics {
                contentDescription = "Real-time system status and controls"
            },
        colors = CardDefaults.cardColors(
            containerColor = connectionColor.copy(alpha = 0.1f)
        )
    ) {
        if (isCompact) {
            // Compact layout for mobile
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Section (Read-only)
                StatusIndicatorsSection(
                    connectionStatus = connectionStatus,
                    dataQuality = dataQuality,
                    compact = true
                )
                
                // Controls Section (Interactive)
                RealTimeControlsSection(
                    realTimeModeEnabled = realTimeModeEnabled,
                    onToggleRealTimeMode = onToggleRealTimeMode,
                    connectionStatus = connectionStatus,
                    onRetryConnection = onRetryConnection,
                    compact = true
                )
            }
        } else {
            // Full layout for tablets/desktop
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Section (Read-only)
                StatusIndicatorsSection(
                    connectionStatus = connectionStatus,
                    dataQuality = dataQuality,
                    compact = false
                )
                
                // Controls Section (Interactive)
                RealTimeControlsSection(
                    realTimeModeEnabled = realTimeModeEnabled,
                    onToggleRealTimeMode = onToggleRealTimeMode,
                    connectionStatus = connectionStatus,
                    onRetryConnection = onRetryConnection,
                    compact = false
                )
            }
        }
    }
}

/**
 * Status indicators section - purely informational, no user controls
 */
@Composable
private fun StatusIndicatorsSection(
    connectionStatus: ConnectionState,
    dataQuality: DataQualityIndicators?,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    if (compact) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConnectionStatusBadge(
                    connectionStatus = connectionStatus,
                    compact = true
                )
                
                if (dataQuality != null) {
                    DataQualityBadge(
                        dataQuality = dataQuality,
                        compact = true
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionStatusBadge(
                connectionStatus = connectionStatus,
                compact = false
            )
            
            if (dataQuality != null) {
                DataQualityBadge(
                    dataQuality = dataQuality,
                    compact = false
                )
            }
        }
    }
}

/**
 * Real-time controls section - interactive user controls
 */
@Composable
private fun RealTimeControlsSection(
    realTimeModeEnabled: Boolean,
    onToggleRealTimeMode: (Boolean) -> Unit,
    connectionStatus: ConnectionState,
    onRetryConnection: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    if (compact) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Controls",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RealTimeModeControl(
                    enabled = realTimeModeEnabled,
                    onToggle = onToggleRealTimeMode,
                    compact = true
                )
                
                if (connectionStatus == ConnectionState.DISCONNECTED || connectionStatus == ConnectionState.FAILED) {
                    RetryConnectionButton(
                        onRetry = onRetryConnection,
                        compact = true
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (connectionStatus == ConnectionState.DISCONNECTED || connectionStatus == ConnectionState.FAILED) {
                RetryConnectionButton(
                    onRetry = onRetryConnection,
                    compact = false
                )
            }
            
            RealTimeModeControl(
                enabled = realTimeModeEnabled,
                onToggle = onToggleRealTimeMode,
                compact = false
            )
        }
    }
}

/**
 * Connection status badge - purely informational, no user interaction
 */
@Composable
private fun ConnectionStatusBadge(
    connectionStatus: ConnectionState,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, text, description) = when (connectionStatus) {
        ConnectionState.CONNECTED -> Triple(
            Icons.Default.SignalWifi4Bar,
            "Connected",
            "Real-time connection is active and stable"
        )
        ConnectionState.RECONNECTING -> Triple(
            Icons.Default.NetworkCheck,
            "Reconnecting",
            "Attempting to restore real-time connection"
        )
        ConnectionState.DISCONNECTED -> Triple(
            Icons.Default.SignalWifiOff,
            "Disconnected",
            "Real-time connection is not available"
        )
        ConnectionState.FAILED -> Triple(
            Icons.Default.Error,
            "Failed",
            "Real-time connection has failed"
        )
        ConnectionState.UNKNOWN -> Triple(
            Icons.Default.Warning,
            "Unknown",
            "Real-time connection status is unknown"
        )
    }
    
    val statusColor = when (connectionStatus) {
        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
        ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.tertiary
        ConnectionState.DISCONNECTED, ConnectionState.FAILED -> MaterialTheme.colorScheme.error
        ConnectionState.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
    
    Surface(
        modifier = modifier.semantics {
            contentDescription = description
        },
        color = statusColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(if (compact) 16.dp else 18.dp)
            )
            
            if (!compact) {
                Column {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                    Text(
                        text = "Connection",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }
        }
    }
}

/**
 * Data quality badge - purely informational, no user interaction
 */
@Composable
private fun DataQualityBadge(
    dataQuality: DataQualityIndicators,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val qualityColor = when {
        dataQuality.overallScore >= 0.8 -> MaterialTheme.colorScheme.primary
        dataQuality.overallScore >= 0.6 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    
    val qualityText = when {
        dataQuality.overallScore >= 0.8 -> "Excellent"
        dataQuality.overallScore >= 0.6 -> "Good"
        dataQuality.overallScore >= 0.4 -> "Fair"
        else -> "Poor"
    }
    
    Surface(
        modifier = modifier.semantics {
            contentDescription = "Data quality: $qualityText, ${(dataQuality.overallScore * 100).toInt()}% reliability"
        },
        color = qualityColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = qualityColor,
                modifier = Modifier.size(if (compact) 16.dp else 18.dp)
            )
            
            if (!compact) {
                Column {
                    Text(
                        text = qualityText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = qualityColor
                    )
                    Text(
                        text = "Data Quality",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                // Quality progress indicator
                LinearProgressIndicator(
                    progress = { dataQuality.overallScore.toFloat() },
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = qualityColor,
                    trackColor = qualityColor.copy(alpha = 0.3f)
                )
            } else {
                Text(
                    text = qualityText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = qualityColor
                )
            }
        }
    }
}

/**
 * Real-time mode control - interactive user control with clear labeling
 */
@Composable
private fun RealTimeModeControl(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.semantics {
            contentDescription = if (enabled) "Live updates are enabled" else "Live updates are disabled"
        }
    ) {
        if (!compact) {
            Icon(
                imageVector = if (enabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
            
            Column {
                Text(
                    text = "Live Updates",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (enabled) "Active" else "Paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            Text(
                text = "Live Updates",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun RetryConnectionButton(
    onRetry: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    if (compact) {
        TextButton(
            onClick = onRetry,
            modifier = modifier.semantics {
                contentDescription = "Retry real-time connection"
            }
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Retry",
                style = MaterialTheme.typography.labelMedium
            )
        }
    } else {
        OutlinedButton(
            onClick = onRetry,
            modifier = modifier.semantics {
                contentDescription = "Retry real-time connection"
            }
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry Connection")
        }
    }
}

/**
 * Enhanced real-time map card with loading and error states
 */
@Composable
private fun RealTimeMapCard(
    trainStates: List<com.example.pravaahan.domain.model.RealTimeTrainState>,
    sectionConfig: com.example.pravaahan.domain.model.RailwaySectionConfig,
    connectionStatus: ConnectionState,
    isLoading: Boolean,
    error: String?,
    onTrainSelected: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp)
            .semantics {
                contentDescription = "Railway section map showing ${trainStates.size} trains"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading && trainStates.isEmpty() -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LoadingIndicator(message = "Loading railway map...")
                        }
                    }
                }
                
                error != null && trainStates.isEmpty() -> {
                    // Error state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Map error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Text(
                                text = "Map Unavailable",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            
                            Button(onClick = onRetry) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                
                connectionStatus == ConnectionState.DISCONNECTED -> {
                    // Disconnected state with offline map
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Show map with cached data if available
                        RailwaySectionMap(
                            trainStates = trainStates,
                            sectionConfig = sectionConfig,
                            onTrainSelected = onTrainSelected,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Offline indicator overlay
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SignalWifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                else -> {
                    // Normal state with real-time map
                    RailwaySectionMap(
                        trainStates = trainStates,
                        sectionConfig = sectionConfig,
                        onTrainSelected = onTrainSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Connection quality indicator
                    if (connectionStatus == ConnectionState.RECONNECTING) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Reconnecting",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Train status panel showing real-time information for all trains
 */
@Composable
private fun TrainStatusPanel(
    trainStates: List<com.example.pravaahan.domain.model.RealTimeTrainState>,
    selectedTrainId: String?,
    onTrainSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(
            title = "Real-time Train Status",
            subtitle = "${trainStates.size} trains with live position data"
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                trainStates.forEach { trainState ->
                    TrainStatusItem(
                        trainState = trainState,
                        isSelected = trainState.trainId == selectedTrainId,
                        onClick = { onTrainSelected(trainState.trainId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainStatusItem(
    trainState: com.example.pravaahan.domain.model.RealTimeTrainState,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "background_color"
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .semantics {
                contentDescription = "Train ${trainState.trainId}, data quality ${(trainState.dataQuality?.overallScore?.times(100)?.toInt() ?: 0)}%"
            },
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Train ${trainState.trainId}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                
                trainState.currentPosition?.let { position ->
                    Text(
                        text = "Speed: ${position.speed.toInt()} km/h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Data quality indicator
                val qualityColor = when {
                    (trainState.dataQuality?.overallScore ?: 0.0) >= 0.8 -> MaterialTheme.colorScheme.primary
                    (trainState.dataQuality?.overallScore ?: 0.0) >= 0.6 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = qualityColor,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                
                // Connection status indicator
                Icon(
                    imageVector = when (trainState.connectionStatus) {
                        ConnectionState.CONNECTED -> Icons.Default.CheckCircle
                        ConnectionState.RECONNECTING -> Icons.Default.NetworkCheck
                        else -> Icons.Default.Warning
                    },
                    contentDescription = "Connection: ${trainState.connectionStatus.name.lowercase()}",
                    tint = when (trainState.connectionStatus) {
                        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                        ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
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
        type = ConflictType.POTENTIAL_COLLISION,
        severity = ConflictSeverity.HIGH,
        involvedTrains = listOf("train-001", "train-002"),
        description = "Potential collision detected between trains",
        timestamp = Clock.System.now(),
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
                ),
                trainStates = SampleRailwayData.createSampleTrainStates(),
                sectionConfig = SampleRailwayData.createSampleSection(),
                connectionStatus = ConnectionState.CONNECTED,
                dataQuality = DataQualityIndicators(
                    latency = 50L,
                    accuracy = 0.9,
                    completeness = 0.95,
                    signalStrength = 0.9,
                    gpsAccuracy = 5.0,
                    dataFreshness = 95L,
                    validationStatus = ValidationStatus.VALID,
                    sourceReliability = 0.92,
                    anomalyFlags = emptyList()
                ),
                realTimeModeEnabled = true,
                selectedTrainId = "train-001"
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
private fun DashboardScreenDisconnectedPreview() {
    PravahanTheme {
        DashboardContent(
            uiState = DashboardUiState(
                trains = sampleTrains,
                conflicts = emptyList(),
                systemStatus = SystemStatus(
                    supabaseConnected = false,
                    realtimeActive = false,
                    aiEngineOnline = true,
                    lastUpdated = System.currentTimeMillis()
                ),
                trainStates = SampleRailwayData.createSampleTrainStates(),
                sectionConfig = SampleRailwayData.createSampleSection(),
                connectionStatus = ConnectionState.DISCONNECTED,
                dataQuality = DataQualityIndicators(
                    latency = 200L,
                    accuracy = 0.3,
                    completeness = 0.4,
                    signalStrength = 0.2,
                    gpsAccuracy = 15.0,
                    dataFreshness = 300L,
                    validationStatus = ValidationStatus.INVALID,
                    sourceReliability = 0.4,
                    anomalyFlags = listOf(com.example.pravaahan.domain.model.AnomalyFlag.SIGNAL_INTERFERENCE)
                ),
                realTimeModeEnabled = false
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
private fun DashboardScreenReconnectingPreview() {
    PravahanTheme {
        DashboardContent(
            uiState = DashboardUiState(
                trains = sampleTrains,
                conflicts = sampleConflicts,
                systemStatus = SystemStatus(
                    supabaseConnected = true,
                    realtimeActive = false,
                    aiEngineOnline = true,
                    lastUpdated = System.currentTimeMillis()
                ),
                trainStates = SampleRailwayData.createSampleTrainStates(),
                sectionConfig = SampleRailwayData.createSampleSection(),
                connectionStatus = ConnectionState.RECONNECTING,
                dataQuality = DataQualityIndicators(
                    latency = 100L,
                    accuracy = 0.7,
                    completeness = 0.75,
                    signalStrength = 0.6,
                    gpsAccuracy = 15.0,
                    dataFreshness = 70L,
                    validationStatus = ValidationStatus.PENDING,
                    sourceReliability = 0.75,
                    anomalyFlags = emptyList()
                ),
                realTimeModeEnabled = true
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