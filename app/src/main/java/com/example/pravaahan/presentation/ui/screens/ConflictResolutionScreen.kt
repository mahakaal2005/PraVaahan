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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pravaahan.presentation.ui.components.ErrorMessage
import com.example.pravaahan.presentation.ui.components.LoadingIndicator
import com.example.pravaahan.presentation.ui.components.TrainCard
import com.example.pravaahan.presentation.ui.screens.conflictresolution.ConflictResolutionAction
import com.example.pravaahan.presentation.ui.screens.conflictresolution.ConflictResolutionUiState
import com.example.pravaahan.presentation.ui.screens.conflictresolution.ConflictResolutionViewModel
import com.example.pravaahan.presentation.ui.theme.ConflictRed
import com.example.pravaahan.presentation.ui.theme.ConflictRedContainer
import com.example.pravaahan.presentation.ui.theme.OnTimeGreen
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
 * Conflict resolution screen - Handle train conflicts and AI recommendations
 * Provides detailed conflict information and resolution options
 */
@Composable
fun ConflictResolutionScreen(
    conflictId: String,
    onBackClick: () -> Unit,
    onTrainClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConflictResolutionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(conflictId) {
        viewModel.handleAction(ConflictResolutionAction.LoadConflictDetails(conflictId))
    }
    
    LaunchedEffect(uiState.resolutionSuccess) {
        if (uiState.resolutionSuccess) {
            snackbarHostState.showSnackbar("Conflict resolution submitted successfully")
            viewModel.handleAction(ConflictResolutionAction.ClearSuccess)
        }
    }
    
    ConflictResolutionContent(
        uiState = uiState,
        onAction = viewModel::handleAction,
        onBackClick = onBackClick,
        onTrainClick = onTrainClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConflictResolutionContent(
    uiState: ConflictResolutionUiState,
    onAction: (ConflictResolutionAction) -> Unit,
    onBackClick: () -> Unit,
    onTrainClick: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
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
                            text = "Conflict Resolution",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: ${uiState.conflict?.id ?: "Loading..."}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.conflict == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            
            uiState.error != null && uiState.conflict == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorMessage(
                        message = uiState.error,
                        onRetry = { onAction(ConflictResolutionAction.Retry) }
                    )
                }
            }
            
            uiState.conflict != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error message if there's an error but we have data
                    if (uiState.error != null) {
                        item {
                            ErrorMessage(
                                message = uiState.error,
                                onRetry = { onAction(ConflictResolutionAction.Retry) }
                            )
                        }
                    }
                    
                    // Conflict Overview
                    item {
                        ConflictOverviewCard(
                            conflict = uiState.conflict,
                            isResolved = uiState.resolutionSuccess
                        )
                    }
                    
                    // AI Recommendation
                    item {
                        AIRecommendationCard(
                            recommendation = uiState.conflict.recommendation,
                            severity = uiState.conflict.severity
                        )
                    }
                    
                    // Involved Trains
                    if (uiState.involvedTrains.isNotEmpty()) {
                        item {
                            Text(
                                text = "Involved Trains",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        items(
                            items = uiState.involvedTrains,
                            key = { it.id }
                        ) { train ->
                            TrainCard(
                                train = train,
                                onClick = { onTrainClick(train.id) }
                            )
                        }
                    }
                    
                    // Resolution Actions
                    if (!uiState.resolutionSuccess) {
                        item {
                            ResolutionActionsCard(
                                manualOverrideText = uiState.manualOverrideText,
                                onManualOverrideTextChange = { text ->
                                    onAction(ConflictResolutionAction.UpdateManualOverrideText(text))
                                },
                                onAcceptRecommendation = {
                                    onAction(ConflictResolutionAction.AcceptRecommendation)
                                },
                                onSubmitOverride = {
                                    onAction(ConflictResolutionAction.SubmitManualOverride)
                                },
                                isSubmitting = uiState.isSubmittingResolution
                            )
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = OnTimeGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Conflict resolution submitted successfully",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
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

@Composable
private fun ConflictOverviewCard(
    conflict: ConflictAlert,
    isResolved: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isResolved) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                ConflictRedContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isResolved) "Resolved" else "Active Conflict",
                    tint = if (isResolved) OnTimeGreen else ConflictRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isResolved) "Conflict Resolved" else when (conflict.conflictType) {
                        ConflictType.POTENTIAL_COLLISION -> "Collision Risk"
                        ConflictType.SCHEDULE_CONFLICT -> "Schedule Conflict"
                        ConflictType.TRACK_CONGESTION -> "Track Congestion"
                        ConflictType.SIGNAL_FAILURE -> "Signal Failure"
                        ConflictType.MAINTENANCE_WINDOW -> "Maintenance Window"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isResolved) OnTimeGreen else ConflictRed
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(
                    label = "Severity",
                    value = conflict.severity.name
                )
                InfoColumn(
                    label = "Trains Involved",
                    value = "${conflict.trainsInvolved.size}"
                )
                InfoColumn(
                    label = "Detected",
                    value = "12:30 PM" // TODO: Format detectedAt
                )
            }
        }
    }
}

@Composable
private fun AIRecommendationCard(
    recommendation: String,
    severity: ConflictSeverity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "AI Recommendation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Confidence: ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = when (severity) {
                        ConflictSeverity.CRITICAL -> "95%"
                        ConflictSeverity.HIGH -> "88%"
                        ConflictSeverity.MEDIUM -> "75%"
                        ConflictSeverity.LOW -> "65%"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ResolutionActionsCard(
    manualOverrideText: String,
    onManualOverrideTextChange: (String) -> Unit,
    onAcceptRecommendation: () -> Unit,
    onSubmitOverride: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Resolution Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Accept AI Recommendation
            Button(
                onClick = onAcceptRecommendation,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp).height(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Accept"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accept AI Recommendation")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Or provide manual override:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = manualOverrideText,
                onValueChange = onManualOverrideTextChange,
                label = { Text("Manual Override Instructions") },
                placeholder = { Text("Enter your resolution instructions...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onSubmitOverride,
                modifier = Modifier.fillMaxWidth(),
                enabled = manualOverrideText.isNotBlank() && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp).height(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submitting...")
                } else {
                    Text("Submit Manual Override")
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(
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

// Sample data for preview
private val sampleConflict = ConflictAlert(
    id = "conflict-001",
    trainsInvolved = listOf("train-001", "train-002"),
    conflictType = ConflictType.POTENTIAL_COLLISION,
    severity = ConflictSeverity.HIGH,
    detectedAt = Clock.System.now(),
    estimatedImpactTime = Clock.System.now(),
    recommendation = "Reduce speed of Train 12001 to 60 km/h and hold Train 12002 at next signal until clear path is available."
)

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

@Preview(showBackground = true)
@Composable
private fun ConflictResolutionScreenPreview() {
    PravahanTheme {
        ConflictResolutionContent(
            uiState = ConflictResolutionUiState(
                conflict = sampleConflict,
                involvedTrains = sampleTrains,
                manualOverrideText = ""
            ),
            onAction = { },
            onBackClick = { },
            onTrainClick = { },
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConflictResolutionScreenLoadingPreview() {
    PravahanTheme {
        ConflictResolutionContent(
            uiState = ConflictResolutionUiState(isLoading = true),
            onAction = { },
            onBackClick = { },
            onTrainClick = { },
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConflictResolutionScreenSuccessPreview() {
    PravahanTheme {
        ConflictResolutionContent(
            uiState = ConflictResolutionUiState(
                conflict = sampleConflict,
                involvedTrains = sampleTrains,
                resolutionSuccess = true
            ),
            onAction = { },
            onBackClick = { },
            onTrainClick = { },
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}