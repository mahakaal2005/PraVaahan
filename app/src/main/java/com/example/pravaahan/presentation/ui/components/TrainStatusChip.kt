package com.example.pravaahan.presentation.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pravaahan.domain.model.TrainStatus
import com.example.pravaahan.presentation.ui.theme.AtRiskOrange
import com.example.pravaahan.presentation.ui.theme.AtRiskOrangeContainer
import com.example.pravaahan.presentation.ui.theme.ConflictRed
import com.example.pravaahan.presentation.ui.theme.ConflictRedContainer
import com.example.pravaahan.presentation.ui.theme.DelayedAmber
import com.example.pravaahan.presentation.ui.theme.DelayedAmberContainer
import com.example.pravaahan.presentation.ui.theme.MaintenanceBlue
import com.example.pravaahan.presentation.ui.theme.MaintenanceBlueContainer
import com.example.pravaahan.presentation.ui.theme.OnTimeGreen
import com.example.pravaahan.presentation.ui.theme.OnTimeGreenContainer
import com.example.pravaahan.presentation.ui.theme.PravahanTheme

/**
 * A chip component that displays train status with appropriate colors
 * Following Material Design 3 principles with railway-specific color coding
 */
@Composable
fun TrainStatusChip(
    status: TrainStatus,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, text) = when (status) {
        TrainStatus.ON_TIME -> Triple(
            OnTimeGreenContainer,
            OnTimeGreen,
            "On Time"
        )
        TrainStatus.DELAYED -> Triple(
            DelayedAmberContainer,
            DelayedAmber,
            "Delayed"
        )
        TrainStatus.STOPPED -> Triple(
            AtRiskOrangeContainer,
            AtRiskOrange,
            "Stopped"
        )
        TrainStatus.MAINTENANCE -> Triple(
            MaintenanceBlueContainer,
            MaintenanceBlue,
            "Maintenance"
        )
        TrainStatus.EMERGENCY -> Triple(
            ConflictRedContainer,
            ConflictRed,
            "Emergency"
        )
    }

    AssistChip(
        onClick = { /* Status chips are typically non-interactive */ },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun TrainStatusChipPreview() {
    PravahanTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp)
        ) {
            TrainStatus.values().forEach { status ->
                TrainStatusChip(
                    status = status,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}