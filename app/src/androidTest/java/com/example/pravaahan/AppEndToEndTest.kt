package com.example.pravaahan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pravaahan.core.health.AppStartupVerifier
import com.example.pravaahan.core.health.HealthStatus
import com.example.pravaahan.domain.model.*
import com.example.pravaahan.util.ComposeTestUtils
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive end-to-end tests for PraVaahan railway control system.
 * Tests complete user journeys and system integration.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppEndToEndTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var appStartupVerifier: AppStartupVerifier

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun appLaunchesSuccessfullyAndShowsDashboard() {
        // Verify app launches without crashes
        composeTestRule.onNodeWithText("PraVaahan").assertIsDisplayed()
        
        // Verify dashboard elements are present
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Refresh trains").assertIsDisplayed()
    }

    @Test
    fun appStartupVerificationPasses() = runTest {
        // Verify all app components are healthy
        val healthStatus = appStartupVerifier.verifyAppComponents()
        assertTrue(
            healthStatus is HealthStatus.Healthy,
            "App startup verification should pass: $healthStatus"
        )
    }

    @Test
    fun completeNavigationFlowWorks() {
        // Start at dashboard
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()

        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // Navigate back to dashboard
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()

        // Test train details navigation (if trains are available)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Look for any train card and click it
        val trainCards = composeTestRule.onAllNodesWithTag("train_card")
        if (trainCards.fetchSemanticsNodes().isNotEmpty()) {
            trainCards.onFirst().performClick()
            
            // Verify we're on train details screen
            composeTestRule.onNodeWithText("Train Details").assertIsDisplayed()
            
            // Navigate back
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
            composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()
        }
    }

    @Test
    fun errorHandlingDisplaysUserFriendlyMessages() {
        // Wait for initial load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Check if error message is displayed (in case of network issues)
        val errorNodes = composeTestRule.onAllNodesWithTag("error_message")
        if (errorNodes.fetchSemanticsNodes().isNotEmpty()) {
            // Verify error message is user-friendly
            val errorText = errorNodes.onFirst().fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)
            assertTrue(
                errorText?.any { it.text.contains("connection") || it.text.contains("network") || it.text.contains("try again") } == true,
                "Error message should be user-friendly"
            )
            
            // Verify retry button is present
            composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        }
    }

    @Test
    fun realTimeDataUpdatesWork() {
        // Wait for initial data load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Verify trains are displayed (if available)
        val trainCards = composeTestRule.onAllNodesWithTag("train_card")
        if (trainCards.fetchSemanticsNodes().isNotEmpty()) {
            // Verify train status chips are displayed
            composeTestRule.onAllNodesWithTag("train_status_chip").assertCountEquals(
                trainCards.fetchSemanticsNodes().size
            )
            
            // Verify priority chips are displayed
            composeTestRule.onAllNodesWithTag("train_priority_chip").assertCountEquals(
                trainCards.fetchSemanticsNodes().size
            )
        }

        // Test refresh functionality
        composeTestRule.onNodeWithContentDescription("Refresh trains").performClick()
        
        // Verify loading state appears briefly
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        
        // Wait for refresh to complete
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("loading_indicator").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun conflictResolutionFlowWorks() {
        // Wait for initial load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Look for conflict alerts
        val conflictAlerts = composeTestRule.onAllNodesWithTag("conflict_alert_card")
        if (conflictAlerts.fetchSemanticsNodes().isNotEmpty()) {
            // Click on first conflict alert
            conflictAlerts.onFirst().performClick()
            
            // Verify we're on conflict resolution screen
            composeTestRule.onNodeWithText("Conflict Resolution").assertIsDisplayed()
            
            // Verify action buttons are present
            composeTestRule.onNodeWithText("Accept Recommendation").assertIsDisplayed()
            composeTestRule.onNodeWithText("Manual Override").assertIsDisplayed()
            
            // Navigate back
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
            composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()
        }
    }

    @Test
    fun themeAndAccessibilityWork() {
        // Verify Material Design 3 components are used
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()
        
        // Test theme switching
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Look for theme toggle (if implemented)
        val themeToggle = composeTestRule.onAllNodesWithText("Dark Theme")
        if (themeToggle.fetchSemanticsNodes().isNotEmpty()) {
            themeToggle.onFirst().performClick()
            // Theme should change (visual verification would be manual)
        }
        
        // Navigate back
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        
        // Verify accessibility content descriptions are present
        composeTestRule.onNodeWithContentDescription("Refresh trains").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun statePreservationDuringNavigation() {
        // Wait for initial load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Record initial state (number of trains)
        val initialTrainCount = composeTestRule.onAllNodesWithTag("train_card").fetchSemanticsNodes().size

        // Navigate to settings and back
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()

        // Verify state is preserved
        composeTestRule.onNodeWithText("Train Control Dashboard").assertIsDisplayed()
        val finalTrainCount = composeTestRule.onAllNodesWithTag("train_card").fetchSemanticsNodes().size
        
        assertEquals(
            initialTrainCount,
            finalTrainCount,
            "Train count should be preserved during navigation"
        )
    }
}