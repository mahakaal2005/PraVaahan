package com.example.pravaahan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * Comprehensive navigation flow tests for all screens in the app.
 * Verifies that navigation works correctly and state is preserved.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun dashboardToSettingsNavigationWorks() {
        // Start at dashboard
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()

        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        
        // Verify settings screen is displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Verify settings content is present
        composeTestRule.onNodeWithText("App Settings").assertIsDisplayed()
    }

    @Test
    fun settingsToDashboardBackNavigationWorks() {
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // Navigate back using back button
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        
        // Verify we're back at dashboard
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()
    }

    @Test
    fun dashboardToTrainDetailsNavigationWorks() {
        // Wait for trains to load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Look for train cards
        val trainCards = composeTestRule.onAllNodesWithTag("train_card")
        
        if (trainCards.fetchSemanticsNodes().isNotEmpty()) {
            // Click on first train card
            trainCards.onFirst().performClick()
            
            // Verify train details screen is displayed
            composeTestRule.onNodeWithText("Train Details").assertIsDisplayed()
            
            // Verify train details content is present
            composeTestRule.onNodeWithText("Train Information").assertIsDisplayed()
        } else {
            // If no trains available, verify empty state is handled
            val emptyStateNodes = composeTestRule.onAllNodesWithText("No trains available")
            assertTrue(
                emptyStateNodes.fetchSemanticsNodes().isNotEmpty(),
                "Should show empty state when no trains are available"
            )
        }
    }

    @Test
    fun trainDetailsBackNavigationWorks() {
        // Wait for trains to load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        val trainCards = composeTestRule.onAllNodesWithTag("train_card")
        
        if (trainCards.fetchSemanticsNodes().isNotEmpty()) {
            // Navigate to train details
            trainCards.onFirst().performClick()
            composeTestRule.onNodeWithText("Train Details").assertIsDisplayed()

            // Navigate back
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
            
            // Verify we're back at dashboard
            composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()
        }
    }

    @Test
    fun dashboardToConflictResolutionNavigationWorks() {
        // Wait for initial load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Look for conflict alert cards
        val conflictAlerts = composeTestRule.onAllNodesWithTag("conflict_alert_card")
        
        if (conflictAlerts.fetchSemanticsNodes().isNotEmpty()) {
            // Click on first conflict alert
            conflictAlerts.onFirst().performClick()
            
            // Verify conflict resolution screen is displayed
            composeTestRule.onNodeWithText("Conflict Resolution").assertIsDisplayed()
            
            // Verify conflict resolution content is present
            composeTestRule.onNodeWithText("Conflict Details").assertIsDisplayed()
            composeTestRule.onNodeWithText("Accept Recommendation").assertIsDisplayed()
            composeTestRule.onNodeWithText("Manual Override").assertIsDisplayed()
        } else {
            // If no conflicts, this is actually good for railway operations
            println("No conflicts detected - system is operating normally")
        }
    }

    @Test
    fun conflictResolutionBackNavigationWorks() {
        // Wait for initial load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        val conflictAlerts = composeTestRule.onAllNodesWithTag("conflict_alert_card")
        
        if (conflictAlerts.fetchSemanticsNodes().isNotEmpty()) {
            // Navigate to conflict resolution
            conflictAlerts.onFirst().performClick()
            composeTestRule.onNodeWithText("Conflict Resolution").assertIsDisplayed()

            // Navigate back
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
            
            // Verify we're back at dashboard
            composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()
        }
    }

    @Test
    fun deepNavigationAndBackStackWorks() {
        // Test deep navigation: Dashboard -> Settings -> Back -> Train Details -> Back
        
        // Start at dashboard
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()

        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // Navigate back to dashboard
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()

        // Wait for trains to load
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Navigate to train details (if available)
        val trainCards = composeTestRule.onAllNodesWithTag("train_card")
        if (trainCards.fetchSemanticsNodes().isNotEmpty()) {
            trainCards.onFirst().performClick()
            composeTestRule.onNodeWithText("Train Details").assertIsDisplayed()

            // Navigate back to dashboard
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
            composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()
        }
    }

    @Test
    fun navigationPreservesDataState() {
        // Wait for initial data load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Record initial state
        val initialTrainCount = composeTestRule.onAllNodesWithTag("train_card").fetchSemanticsNodes().size
        val initialConflictCount = composeTestRule.onAllNodesWithTag("conflict_alert_card").fetchSemanticsNodes().size

        // Navigate through multiple screens
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()

        // Verify data state is preserved
        val finalTrainCount = composeTestRule.onAllNodesWithTag("train_card").fetchSemanticsNodes().size
        val finalConflictCount = composeTestRule.onAllNodesWithTag("conflict_alert_card").fetchSemanticsNodes().size

        assertTrue(
            initialTrainCount == finalTrainCount,
            "Train count should be preserved during navigation"
        )
        
        assertTrue(
            initialConflictCount == finalConflictCount,
            "Conflict count should be preserved during navigation"
        )
    }

    @Test
    fun allScreensHaveProperAccessibilitySupport() {
        // Test dashboard accessibility
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Refresh trains").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()

        // Test settings accessibility
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()

        // Test train details accessibility (if trains available)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        val trainCards = composeTestRule.onAllNodesWithTag("train_card")
        if (trainCards.fetchSemanticsNodes().isNotEmpty()) {
            trainCards.onFirst().performClick()
            composeTestRule.onNodeWithText("Train Details").assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
            
            // Navigate back
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        }
    }
}