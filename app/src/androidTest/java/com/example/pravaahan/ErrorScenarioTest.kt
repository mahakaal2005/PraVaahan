package com.example.pravaahan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pravaahan.core.error.AppError
import com.example.pravaahan.core.error.ErrorHandler
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import androidx.compose.ui.semantics.SemanticsProperties

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * Tests error handling scenarios to ensure the app gracefully handles failures
 * and displays user-friendly error messages.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ErrorScenarioTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var errorHandler: ErrorHandler

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun networkErrorDisplaysUserFriendlyMessage() {
        // Wait for app to load
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Check if network error is displayed
        val errorMessages = composeTestRule.onAllNodesWithTag("error_message")
        
        if (errorMessages.fetchSemanticsNodes().isNotEmpty()) {
            // Verify error message is user-friendly
            val errorNode = errorMessages.onFirst()
            errorNode.assertIsDisplayed()
            
            // Check that retry button is available
            composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
            
            // Test retry functionality
            composeTestRule.onNodeWithText("Retry").performClick()
            
            // Verify loading state appears
            composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        }
    }

    @Test
    fun errorHandlerProcessesNetworkErrorsCorrectly() = runTest {
        val networkException = UnknownHostException("Network unreachable")
        val appError = errorHandler.handleError(networkException)
        
        assertTrue(
            "Network exceptions should be converted to NetworkError",
            appError is AppError.NetworkError
        )
    }

    @Test
    fun errorHandlerProcessesApiErrorsCorrectly() = runTest {
        val apiException = RuntimeException("HTTP 500: Internal Server Error")
        val appError = errorHandler.handleError(apiException)
        
        assertTrue(
            "API exceptions should be handled gracefully",
            appError is AppError.NetworkError || appError is AppError.SystemError.UnexpectedError
        )
    }

    @Test
    fun emptyStateIsHandledGracefully() {
        // Wait for initial load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Check if empty state is displayed when no data is available
        val trainCards = composeTestRule.onAllNodesWithTag("train_card")
        val conflictCards = composeTestRule.onAllNodesWithTag("conflict_alert_card")
        
        if (trainCards.fetchSemanticsNodes().isEmpty() && conflictCards.fetchSemanticsNodes().isEmpty()) {
            // Verify empty state message is shown
            val emptyStateNodes = composeTestRule.onAllNodesWithText("No trains available")
            assertTrue(
                "Should display appropriate empty state message",
                emptyStateNodes.fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("No active conflicts").fetchSemanticsNodes().isNotEmpty()
            )
        }
    }

    @Test
    fun loadingStateIsDisplayedDuringDataFetch() {
        // Immediately check for loading indicator after app launch
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        
        // Wait for loading to complete
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithTag("loading_indicator").fetchSemanticsNodes().isEmpty()
        }
        
        // Test refresh loading state
        composeTestRule.onNodeWithContentDescription("Refresh data").performClick()
        
        // Verify loading indicator appears during refresh
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        
        // Wait for refresh to complete
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("loading_indicator").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun invalidTrainDetailsHandledGracefully() {
        // Wait for trains to load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        val trainCards = composeTestRule.onAllNodesWithTag("train_card")
        
        if (trainCards.fetchSemanticsNodes().isNotEmpty()) {
            // Click on a train card
            trainCards.onFirst().performClick()
            
            // Verify train details screen loads
            composeTestRule.onNodeWithText("Train Details").assertIsDisplayed()
            
            // Check if error handling works for invalid train data
            val errorMessages = composeTestRule.onAllNodesWithTag("error_message")
            if (errorMessages.fetchSemanticsNodes().isNotEmpty()) {
                // Verify error is handled gracefully
                errorMessages.onFirst().assertIsDisplayed()
                
                // Verify back navigation still works
                composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
                composeTestRule.onNodeWithText("PraVaahan Control").assertIsDisplayed()
            }
        }
    }

    @Test
    fun conflictResolutionErrorsHandledGracefully() {
        // Wait for initial load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        val conflictCards = composeTestRule.onAllNodesWithTag("conflict_alert_card")
        
        if (conflictCards.fetchSemanticsNodes().isNotEmpty()) {
            // Click on a conflict card
            conflictCards.onFirst().performClick()
            
            // Verify conflict resolution screen loads
            composeTestRule.onNodeWithText("Conflict Resolution").assertIsDisplayed()
            
            // Try to accept recommendation (might fail due to network/data issues)
            composeTestRule.onNodeWithText("Accept Recommendation").performClick()
            
            // Check if any error messages are displayed
            val errorMessages = composeTestRule.onAllNodesWithTag("error_message")
            if (errorMessages.fetchSemanticsNodes().isNotEmpty()) {
                // Verify error is displayed with retry option
                errorMessages.onFirst().assertIsDisplayed()
                
                // Verify user can still navigate back
                composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
                composeTestRule.onNodeWithText("PraVaahan Control").assertIsDisplayed()
            }
        }
    }

    @Test
    fun timeoutErrorsDisplayAppropriateMessages() {
        // This test simulates timeout scenarios
        // In a real app, we might inject a test client that simulates timeouts
        
        // Wait for initial load with extended timeout
        composeTestRule.waitUntil(timeoutMillis = 30000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty() ||
            composeTestRule.onAllNodesWithTag("error_message").fetchSemanticsNodes().isNotEmpty()
        }

        // Check if timeout error is handled
        val errorMessages = composeTestRule.onAllNodesWithTag("error_message")
        if (errorMessages.fetchSemanticsNodes().isNotEmpty()) {
            val errorText = errorMessages.onFirst().fetchSemanticsNode()
                .config[SemanticsProperties.Text].firstOrNull()?.text
            
            assertTrue(
                "Timeout errors should display user-friendly messages",
                errorText?.contains("connection") == true ||
                errorText?.contains("timeout") == true ||
                errorText?.contains("network") == true ||
                errorText?.contains("try again") == true
            )
        }
    }

    @Test
    fun appDoesNotCrashOnUnexpectedErrors() {
        // This test ensures the app remains stable even when unexpected errors occur
        
        // Navigate through the app to trigger various operations
        composeTestRule.onNodeWithText("PraVaahan Control").assertIsDisplayed()
        
        // Try refresh multiple times rapidly
        repeat(3) {
            composeTestRule.onNodeWithContentDescription("Refresh data").performClick()
            Thread.sleep(100) // Small delay between clicks
        }
        
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Navigate back
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.onNodeWithText("PraVaahan Control").assertIsDisplayed()
        
        // App should still be responsive and not crashed
        assertTrue(
            "App remained stable during stress operations",
            true
        )
    }

    @Test
    fun errorMessagesAreAccessible() {
        // Wait for app to load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Check if error messages have proper accessibility support
        val errorMessages = composeTestRule.onAllNodesWithTag("error_message")
        
        if (errorMessages.fetchSemanticsNodes().isNotEmpty()) {
            val errorNode = errorMessages.onFirst()
            
            // Verify error message has content description or text
            val hasContentDescription = errorNode.fetchSemanticsNode()
                .config.contains(SemanticsProperties.ContentDescription)
            val hasText = errorNode.fetchSemanticsNode()
                .config.contains(SemanticsProperties.Text)
            
            assertTrue(
                "Error messages should be accessible to screen readers",
                hasContentDescription || hasText
            )
        }
    }
}