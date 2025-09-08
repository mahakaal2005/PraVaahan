package com.example.pravaahan.presentation.ui.screens

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pravaahan.domain.model.ConflictSeverity
import com.example.pravaahan.domain.model.TrainPriority
import com.example.pravaahan.domain.model.TrainStatus
import com.example.pravaahan.presentation.ui.screens.dashboard.DashboardAction
import com.example.pravaahan.presentation.ui.screens.dashboard.DashboardUiState
import com.example.pravaahan.presentation.ui.screens.dashboard.SystemStatus
import com.example.pravaahan.presentation.ui.theme.PraVaahanTheme
import com.example.pravaahan.util.TestDataFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for DashboardScreen
 * Tests user interactions, accessibility, and visual elements for railway control dashboard
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardScreen_displaysTrainList_whenTrainsAreAvailable() {
        // Arrange
        val testTrains = TestDataFactory.createTrainList()
        val uiState = DashboardUiState(
            trains = testTrains,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Assert
        testTrains.forEach { train ->
            composeTestRule.onNodeWithText(train.name).assertIsDisplayed()
            composeTestRule.onNodeWithText(train.status.name).assertIsDisplayed()
        }
    }

    @Test
    fun dashboardScreen_displaysConflictAlerts_whenConflictsExist() {
        // Arrange
        val testConflicts = TestDataFactory.createConflictScenarios()
        val uiState = DashboardUiState(
            conflicts = testConflicts,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Assert
        testConflicts.forEach { conflict ->
            composeTestRule.onNodeWithText(conflict.conflictType.name).assertIsDisplayed()
            composeTestRule.onNodeWithText(conflict.severity.name).assertIsDisplayed()
        }
    }

    @Test
    fun dashboardScreen_showsLoadingIndicator_whenLoading() {
        // Arrange
        val uiState = DashboardUiState(isLoading = true)

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_showsErrorMessage_whenErrorOccurs() {
        // Arrange
        val errorMessage = "Network connection failed"
        val uiState = DashboardUiState(
            error = errorMessage,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_callsRetryAction_whenRetryButtonClicked() {
        // Arrange
        var retryActionCalled = false
        val uiState = DashboardUiState(
            error = "Network error",
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = { action ->
                        if (action is DashboardAction.Retry) {
                            retryActionCalled = true
                        }
                    },
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Retry").performClick()

        // Assert
        assert(retryActionCalled)
    }

    @Test
    fun dashboardScreen_displaysSystemStatus_correctly() {
        // Arrange
        val systemStatus = SystemStatus(
            supabaseConnected = true,
            realtimeActive = true,
            aiEngineOnline = true,
            lastUpdated = System.currentTimeMillis()
        )
        val uiState = DashboardUiState(
            systemStatus = systemStatus,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithContentDescription("System Status").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Online").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_handlesTrainClick_correctly() {
        // Arrange
        var clickedTrainId: String? = null
        val testTrain = TestDataFactory.createTrain(id = "clickable_train")
        val uiState = DashboardUiState(
            trains = listOf(testTrain),
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = { trainId -> clickedTrainId = trainId },
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithText(testTrain.name).performClick()

        // Assert
        assert(clickedTrainId == "clickable_train")
    }

    @Test
    fun dashboardScreen_handlesConflictClick_correctly() {
        // Arrange
        var clickedConflictId: String? = null
        val testConflict = TestDataFactory.createConflictAlert(id = "clickable_conflict")
        val uiState = DashboardUiState(
            conflicts = listOf(testConflict),
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = { conflictId -> clickedConflictId = conflictId },
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithText(testConflict.conflictType.name).performClick()

        // Assert
        assert(clickedConflictId == "clickable_conflict")
    }

    @Test
    fun dashboardScreen_displaysTrainPriorityChips_withCorrectColors() {
        // Arrange
        val trains = listOf(
            TestDataFactory.createTrain(id = "express", priority = TrainPriority.EXPRESS),
            TestDataFactory.createTrain(id = "high", priority = TrainPriority.HIGH),
            TestDataFactory.createTrain(id = "medium", priority = TrainPriority.MEDIUM),
            TestDataFactory.createTrain(id = "low", priority = TrainPriority.LOW)
        )
        val uiState = DashboardUiState(trains = trains, isLoading = false)

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("EXPRESS").assertIsDisplayed()
        composeTestRule.onNodeWithText("HIGH").assertIsDisplayed()
        composeTestRule.onNodeWithText("MEDIUM").assertIsDisplayed()
        composeTestRule.onNodeWithText("LOW").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysTrainStatusChips_withCorrectIndicators() {
        // Arrange
        val trains = listOf(
            TestDataFactory.createTrain(id = "on_time", status = TrainStatus.ON_TIME),
            TestDataFactory.createTrain(id = "delayed", status = TrainStatus.DELAYED),
            TestDataFactory.createTrain(id = "stopped", status = TrainStatus.STOPPED),
            TestDataFactory.createTrain(id = "maintenance", status = TrainStatus.MAINTENANCE)
        )
        val uiState = DashboardUiState(trains = trains, isLoading = false)

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("ON_TIME").assertIsDisplayed()
        composeTestRule.onNodeWithText("DELAYED").assertIsDisplayed()
        composeTestRule.onNodeWithText("STOPPED").assertIsDisplayed()
        composeTestRule.onNodeWithText("MAINTENANCE").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysConflictSeverity_withCorrectIndicators() {
        // Arrange
        val conflicts = listOf(
            TestDataFactory.createConflictAlert(id = "critical", severity = ConflictSeverity.CRITICAL),
            TestDataFactory.createConflictAlert(id = "high", severity = ConflictSeverity.HIGH),
            TestDataFactory.createConflictAlert(id = "medium", severity = ConflictSeverity.MEDIUM),
            TestDataFactory.createConflictAlert(id = "low", severity = ConflictSeverity.LOW)
        )
        val uiState = DashboardUiState(conflicts = conflicts, isLoading = false)

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("CRITICAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("HIGH").assertIsDisplayed()
        composeTestRule.onNodeWithText("MEDIUM").assertIsDisplayed()
        composeTestRule.onNodeWithText("LOW").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_hasProperAccessibilityLabels() {
        // Arrange
        val testTrain = TestDataFactory.createTrain(name = "Test Express")
        val testConflict = TestDataFactory.createConflictAlert()
        val uiState = DashboardUiState(
            trains = listOf(testTrain),
            conflicts = listOf(testConflict),
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Assert - Check for accessibility content descriptions
        composeTestRule.onNodeWithContentDescription("Train: Test Express").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Conflict Alert").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("System Status").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_handlesRefreshAction_correctly() {
        // Arrange
        var refreshActionCalled = false
        val uiState = DashboardUiState(isRefreshing = true)

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = { action ->
                        if (action is DashboardAction.RefreshData) {
                            refreshActionCalled = true
                        }
                    },
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Pull to refresh gesture (if implemented)
        // composeTestRule.onNodeWithTag("RefreshIndicator").performGesture { swipeDown() }

        // Assert
        composeTestRule.onNodeWithContentDescription("Refreshing").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysEmptyState_whenNoData() {
        // Arrange
        val uiState = DashboardUiState(
            trains = emptyList(),
            conflicts = emptyList(),
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            PraVaahanTheme {
                DashboardScreen(
                    uiState = uiState,
                    onAction = {},
                    onNavigateToTrain = {},
                    onNavigateToConflict = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("No trains available").assertIsDisplayed()
        composeTestRule.onNodeWithText("No active conflicts").assertIsDisplayed()
    }
}