package com.example.pravaahan.presentation.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pravaahan.data.sample.SampleRailwayData
import com.example.pravaahan.domain.model.*
import com.example.pravaahan.presentation.ui.screens.dashboard.DashboardUiState
import com.example.pravaahan.presentation.ui.screens.dashboard.SystemStatus
import com.example.pravaahan.presentation.ui.theme.PravahanTheme
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the enhanced DashboardScreen with real-time features
 * Tests accessibility, performance, and user interactions
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenRealTimeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardScreen_displaysRealTimeConnectionStatus() {
        // Arrange
        val uiState = DashboardUiState(
            connectionStatus = ConnectionState.CONNECTED,
            dataQuality = createSampleDataQuality(),
            realTimeModeEnabled = true
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithContentDescription("Real-time connection status: connected")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Connected")
            .assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysDataQualityIndicators() {
        // Arrange
        val dataQuality = createSampleDataQuality(overallScore = 0.89)
        val uiState = DashboardUiState(
            connectionStatus = ConnectionState.CONNECTED,
            dataQuality = dataQuality,
            realTimeModeEnabled = true
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithContentDescription("Data quality: Excellent, 89% reliability")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Excellent")
            .assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysRealTimeMapWithTrains() {
        // Arrange
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val sectionConfig = SampleRailwayData.createSampleSection()
        val uiState = DashboardUiState(
            trainStates = trainStates,
            sectionConfig = sectionConfig,
            connectionStatus = ConnectionState.CONNECTED
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithContentDescription("Railway section map showing ${trainStates.size} trains")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Railway Section Map")
            .assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysTrainStatusPanel() {
        // Arrange
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val uiState = DashboardUiState(
            trainStates = trainStates,
            connectionStatus = ConnectionState.CONNECTED
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Real-time Train Status")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("${trainStates.size} trains with live position data")
            .assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_handlesDisconnectedState() {
        // Arrange
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val sectionConfig = SampleRailwayData.createSampleSection()
        val uiState = DashboardUiState(
            trainStates = trainStates,
            sectionConfig = sectionConfig,
            connectionStatus = ConnectionState.DISCONNECTED,
            realTimeModeEnabled = false
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Disconnected")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Offline")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Retry real-time connection")
            .assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_handlesReconnectingState() {
        // Arrange
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val sectionConfig = SampleRailwayData.createSampleSection()
        val uiState = DashboardUiState(
            trainStates = trainStates,
            sectionConfig = sectionConfig,
            connectionStatus = ConnectionState.RECONNECTING,
            realTimeModeEnabled = true
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Reconnecting")
            .assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_handlesErrorState() {
        // Arrange
        val uiState = DashboardUiState(
            error = "Failed to connect to real-time services",
            connectionStatus = ConnectionState.FAILED
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Map Unavailable")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Failed to connect to real-time services")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Retry")
            .assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_realTimeModeToggle_isAccessible() {
        // Arrange
        val uiState = DashboardUiState(
            connectionStatus = ConnectionState.CONNECTED,
            realTimeModeEnabled = true
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithContentDescription("Real-time mode is enabled")
            .assertIsDisplayed()
        
        // Test toggle functionality
        composeTestRule
            .onNode(hasContentDescription("Real-time mode is enabled"))
            .assertIsToggleable()
    }

    @Test
    fun dashboardScreen_trainStatusItems_areAccessible() {
        // Arrange
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val uiState = DashboardUiState(
            trainStates = trainStates,
            connectionStatus = ConnectionState.CONNECTED,
            selectedTrainId = trainStates.first().trainId
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert - Check that train status items have proper content descriptions
        trainStates.forEach { trainState ->
            val expectedDescription = "Train ${trainState.trainId}, data quality ${(trainState.dataQuality.overallScore * 100).toInt()}%"
            composeTestRule
                .onNodeWithContentDescription(expectedDescription)
                .assertIsDisplayed()
        }
    }

    @Test
    fun dashboardScreen_retryConnection_triggersAction() {
        // Arrange
        var retryActionTriggered = false
        val uiState = DashboardUiState(
            connectionStatus = ConnectionState.DISCONNECTED
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { action ->
                        if (action is com.example.pravaahan.presentation.ui.screens.dashboard.DashboardAction.RetryConnection) {
                            retryActionTriggered = true
                        }
                    },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithContentDescription("Retry real-time connection")
            .performClick()
        
        assert(retryActionTriggered) { "Retry connection action should be triggered" }
    }

    @Test
    fun dashboardScreen_trainSelection_triggersAction() {
        // Arrange
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val targetTrain = trainStates.first()
        var selectedTrainId: String? = null
        
        val uiState = DashboardUiState(
            trainStates = trainStates,
            connectionStatus = ConnectionState.CONNECTED
        )

        // Act
        composeTestRule.setContent {
            PravahanTheme {
                DashboardContent(
                    uiState = uiState,
                    onAction = { action ->
                        if (action is com.example.pravaahan.presentation.ui.screens.dashboard.DashboardAction.SelectTrain) {
                            selectedTrainId = action.trainId
                        }
                    },
                    onTrainClick = { },
                    onConflictClick = { },
                    onSettingsClick = { }
                )
            }
        }

        // Assert
        val expectedDescription = "Train ${targetTrain.trainId}, data quality ${(targetTrain.dataQuality.overallScore * 100).toInt()}%"
        composeTestRule
            .onNodeWithContentDescription(expectedDescription)
            .performClick()
        
        assert(selectedTrainId == targetTrain.trainId) { "Train selection should trigger action with correct train ID" }
    }

    private fun createSampleDataQuality(overallScore: Double = 0.89): DataQualityIndicators {
        return DataQualityIndicators(
            signalStrength = 0.9,
            gpsAccuracy = 5.0,
            dataFreshness = 0.95,
            validationStatus = ValidationStatus.VALID,
            sourceReliability = 0.92,
            anomalyFlags = emptySet(),
            overallScore = overallScore
        )
    }
}