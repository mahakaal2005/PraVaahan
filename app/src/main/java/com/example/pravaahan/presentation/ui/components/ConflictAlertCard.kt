package com.example.pravaahan.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.ConflictSeverity
import com.example.pravaahan.domain.model.ConflictType
import com.example.pravaahan.presentation.ui.theme.ConflictRed
import com.example.pravaahan.presentation.ui.theme.ConflictRedContainer
import com.example.pravaahan.presentation.ui.theme.PravahanTheme
import kotlinx.datetime.Clock

/**
 * A card component that displays conflict alerts with appropriate urgency styling
 * Following Material Design 3 principles with railway-specific conflict information
 */
@Composable
fun ConflictAlertCard(
    conflict: ConflictAlert,
    onAcceptRecommendation: () -> Unit,
    onOverride: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ConflictRedContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with warning icon and conflict type
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Conflict Alert",
                    tint = ConflictRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (conflict.conflictType) {
                        ConflictType.POTENTIAL_COLLISION -> "Collision Risk"
                        ConflictType.SCHEDULE_CONFLICT -> "Schedule Conflict"
                        ConflictType.TRACK_CONGESTION -> "Track Congestion"
                        ConflictType.SIGNAL_FAILURE -> "Signal Failure"
                        ConflictType.MAINTENANCE_WINDOW -> "Maintenance Window"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ConflictRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Severity indicator
            Text(
                text = "Severity: ${conflict.severity.name}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = ConflictRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Trains involved
            Text(
                text = "Trains Involved:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            Text(
                text = conflict.trainsInvolved.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AI Recommendation
            Text(
                text = "AI Recommendation:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            Text(
                text = conflict.recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onAcceptRecommendation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Accept",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                OutlinedButton(
                    onClick = onOverride,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ConflictRed
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Override",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConflictAlertCardPreview() {
    PravahanTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConflictAlertCard(
                conflict = ConflictAlert(
                    id = "conflict-001",
                    trainsInvolved = listOf("12001", "12002"),
                    conflictType = ConflictType.POTENTIAL_COLLISION,
                    severity = ConflictSeverity.HIGH,
                    detectedAt = Clock.System.now(),
                    estimatedImpactTime = Clock.System.now(),
                    recommendation = "Reduce speed of Train 12001 to 60 km/h and hold Train 12002 at next signal."
                ),
                onAcceptRecommendation = { },
                onOverride = { }
            )
        }
    }
}