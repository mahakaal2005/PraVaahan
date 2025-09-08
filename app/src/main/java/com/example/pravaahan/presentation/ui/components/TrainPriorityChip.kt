package com.example.pravaahan.presentation.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pravaahan.domain.model.TrainPriority
import com.example.pravaahan.presentation.ui.theme.ExpressPurple
import com.example.pravaahan.presentation.ui.theme.ExpressPurpleContainer
import com.example.pravaahan.presentation.ui.theme.HighPriorityRed
import com.example.pravaahan.presentation.ui.theme.HighPriorityRedContainer
import com.example.pravaahan.presentation.ui.theme.LowPriorityGreen
import com.example.pravaahan.presentation.ui.theme.LowPriorityGreenContainer
import com.example.pravaahan.presentation.ui.theme.MediumPriorityOrange
import com.example.pravaahan.presentation.ui.theme.MediumPriorityOrangeContainer
import com.example.pravaahan.presentation.ui.theme.PravahanTheme

/**
 * A chip component that displays train priority with appropriate colors
 * Following Material Design 3 principles with railway-specific priority color coding
 */
@Composable
fun TrainPriorityChip(
    priority: TrainPriority,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, text) = when (priority) {
        TrainPriority.EXPRESS -> Triple(
            ExpressPurpleContainer,
            ExpressPurple,
            "Express"
        )
        TrainPriority.HIGH -> Triple(
            HighPriorityRedContainer,
            HighPriorityRed,
            "High"
        )
        TrainPriority.MEDIUM -> Triple(
            MediumPriorityOrangeContainer,
            MediumPriorityOrange,
            "Medium"
        )
        TrainPriority.LOW -> Triple(
            LowPriorityGreenContainer,
            LowPriorityGreen,
            "Low"
        )
    }

    AssistChip(
        onClick = { /* Priority chips are typically non-interactive */ },
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
private fun TrainPriorityChipPreview() {
    PravahanTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp)
        ) {
            TrainPriority.values().forEach { priority ->
                TrainPriorityChip(
                    priority = priority,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}