package com.example.pravaahan.presentation.ui.screens.conflictresolution

import app.cash.turbine.test
import com.example.pravaahan.core.error.AppError
import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.usecase.AcceptRecommendationUseCase
import com.example.pravaahan.domain.usecase.GetConflictByIdUseCase
import com.example.pravaahan.domain.usecase.GetTrainsUseCase
import com.example.pravaahan.domain.usecase.SubmitManualOverrideUseCase
import com.example.pravaahan.fake.FakeConflictRepository
import com.example.pravaahan.fake.FakeTrainRepository
import com.example.pravaahan.util.CoroutineTestExtension
import com.example.pravaahan.util.TestDataFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for ConflictResolutionViewModel
 * Tests conflict resolution scenarios, error handling, and user actions
 */
@ExtendWith(CoroutineTestExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ConflictResolutionViewModelTest {

    private val mockLogger: Logger = mock()
    private val mockErrorHandler: ErrorHandler = mock()

    private lateinit var fakeConflictRepository: FakeConflictRepository
    private lateinit var fakeTrainRepository: FakeTrainRepository
    private lateinit var getConflictByIdUseCase: GetConflictByIdUseCase
    private lateinit var getTrainsUseCase: GetTrainsUseCase
    private lateinit var acceptRecommendationUseCase: AcceptRecommendationUseCase
    private lateinit var submitManualOverrideUseCase: SubmitManualOverrideUseCase
    private lateinit var viewModel: ConflictResolutionViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testConflictId = "test_conflict_001"

    @BeforeEach
    fun setup() {
        fakeConflictRepository = FakeConflictRepository()
        fakeTrainRepository = FakeTrainRepository()
        getConflictByIdUseCase = GetConflictByIdUseCase(fakeConflictRepository)
        getTrainsUseCase = GetTrainsUseCase(fakeTrainRepository)
        acceptRecommendationUseCase = AcceptRecommendationUseCase(fakeConflictRepository)
        submitManualOverrideUseCase = SubmitManualOverrideUseCase(fakeConflictRepository)
        
        // Setup mock error handler
        whenever(mockErrorHandler.handleError(any())).thenReturn(AppError.NetworkError.NoConnection)
    }

    private fun createViewModel(conflictId: String = testConflictId): ConflictResolutionViewModel {
        return ConflictResolutionViewModel(
            getConflictByIdUseCase = getConflictByIdUseCase,
            getTrainsUseCase = getTrainsUseCase,
            acceptRecommendationUseCase = acceptRecommendationUseCase,
            submitManualOverrideUseCase = submitManualOverrideUseCase,
            errorHandler = mockErrorHandler,
            logger = mockLogger
        )
    }

    @Test
    fun `when ViewModel is initialized, should load conflict data successfully`() = runTest(testDispatcher) {
        // Arrange
        val testConflict = TestDataFactory.createConflictAlert(id = testConflictId)
        fakeConflictRepository.addConflict(testConflict)

        // Act
        viewModel = createViewModel()

        // Assert
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            assertNull(initialState.conflict)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(testConflict, loadedState.conflict)
            assertNull(loadedState.error)
        }
    }

    @Test
    fun `when conflict not found, should show error state`() = runTest(testDispatcher) {
        // Arrange
        val nonExistentConflictId = "non_existent_conflict"

        // Act
        viewModel = createViewModel(nonExistentConflictId)

        // Assert
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNull(errorState.conflict)
            assertNotNull(errorState.error)
        }
    }

    @Test
    fun `when AcceptRecommendation action is triggered, should accept recommendation successfully`() = runTest(testDispatcher) {
        // Arrange
        val testConflict = TestDataFactory.createConflictAlert(id = testConflictId)
        fakeConflictRepository.addConflict(testConflict)
        viewModel = createViewModel()

        // Wait for initial load
        viewModel.uiState.test {
            awaitItem() // loading state
            awaitItem() // loaded state

            // Act
            viewModel.handleAction(ConflictResolutionAction.AcceptRecommendation)

            // Assert
            val processingState = awaitItem()
            assertTrue(processingState.isSubmittingResolution)

            val completedState = awaitItem()
            assertFalse(completedState.isSubmittingResolution)
            assertTrue(completedState.resolutionSuccess)
            assertNull(completedState.error)
        }
    }

    @Test
    fun `when SubmitManualOverride action is triggered, should submit override successfully`() = runTest(testDispatcher) {
        // Arrange
        val testConflict = TestDataFactory.createConflictAlert(id = testConflictId)
        fakeConflictRepository.addConflict(testConflict)
        viewModel = createViewModel()
        val overrideReason = "Manual intervention required due to signal failure"

        // Wait for initial load
        viewModel.uiState.test {
            awaitItem() // loading state
            awaitItem() // loaded state

            // Act
            viewModel.handleAction(ConflictResolutionAction.SubmitManualOverride)

            // Assert
            val processingState = awaitItem()
            assertTrue(processingState.isSubmittingResolution)

            val completedState = awaitItem()
            assertFalse(completedState.isSubmittingResolution)
            assertTrue(completedState.resolutionSuccess)
            assertNull(completedState.error)
        }
    }

    @Test
    fun `when UpdateManualOverrideText action is triggered, should update override text`() = runTest(testDispatcher) {
        // Arrange
        val testConflict = TestDataFactory.createConflictAlert(id = testConflictId)
        fakeConflictRepository.addConflict(testConflict)
        viewModel = createViewModel()
        val newOverrideText = "Emergency stop required"

        // Wait for initial load
        viewModel.uiState.test {
            awaitItem() // loading state
            awaitItem() // loaded state

            // Act
            viewModel.handleAction(ConflictResolutionAction.UpdateManualOverrideText(newOverrideText))

            // Assert
            val updatedState = awaitItem()
            assertEquals(newOverrideText, updatedState.manualOverrideText)
        }
    }

    @Test
    fun `when network error occurs during recommendation acceptance, should handle error`() = runTest(testDispatcher) {
        // Arrange
        val testConflict = TestDataFactory.createConflictAlert(id = testConflictId)
        fakeConflictRepository.addConflict(testConflict)
        fakeConflictRepository.setShouldThrowError(true)
        viewModel = createViewModel()

        // Wait for initial load
        viewModel.uiState.test {
            awaitItem() // loading state
            awaitItem() // loaded state

            // Act
            viewModel.handleAction(ConflictResolutionAction.AcceptRecommendation)

            // Assert
            val processingState = awaitItem()
            assertTrue(processingState.isSubmittingResolution)

            val errorState = awaitItem()
            assertFalse(errorState.isSubmittingResolution)
            assertFalse(errorState.resolutionSuccess)
            assertNotNull(errorState.error)
        }
    }

    @Test
    fun `when ClearError action is triggered, should clear error state`() = runTest(testDispatcher) {
        // Arrange
        fakeConflictRepository.setShouldThrowError(true)
        viewModel = createViewModel()

        // Wait for error state
        viewModel.uiState.test {
            awaitItem() // loading state
            val errorState = awaitItem() // error state
            assertNotNull(errorState.error)

            // Act
            viewModel.handleAction(ConflictResolutionAction.ClearError)

            // Assert
            val clearedState = awaitItem()
            assertNull(clearedState.error)
        }
    }

    @Test
    fun `when Retry action is triggered, should retry loading conflict`() = runTest(testDispatcher) {
        // Arrange
        fakeConflictRepository.setShouldThrowError(true)
        viewModel = createViewModel()

        // Wait for error state, then fix the error and retry
        viewModel.uiState.test {
            awaitItem() // loading state
            val errorState = awaitItem() // error state
            assertNotNull(errorState.error)

            // Fix the error condition
            fakeConflictRepository.setShouldThrowError(false)
            val testConflict = TestDataFactory.createConflictAlert(id = testConflictId)
            fakeConflictRepository.addConflict(testConflict)

            // Act
            viewModel.handleAction(ConflictResolutionAction.Retry)

            // Assert
            val retryingState = awaitItem()
            assertTrue(retryingState.isLoading)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertNull(successState.error)
            assertEquals(testConflict, successState.conflict)
        }
    }

    @Test
    fun `when critical conflict is loaded, should indicate high priority`() = runTest(testDispatcher) {
        // Arrange
        val criticalConflict = TestDataFactory.createConflictAlert(
            id = testConflictId,
            severity = com.example.pravaahan.domain.model.ConflictSeverity.CRITICAL,
            conflictType = com.example.pravaahan.domain.model.ConflictType.POTENTIAL_COLLISION
        )
        fakeConflictRepository.addConflict(criticalConflict)

        // Act
        viewModel = createViewModel()

        // Assert
        viewModel.uiState.test {
            awaitItem() // loading state
            val loadedState = awaitItem() // loaded state

            assertEquals(com.example.pravaahan.domain.model.ConflictSeverity.CRITICAL, loadedState.conflict?.severity)
            assertEquals(com.example.pravaahan.domain.model.ConflictType.POTENTIAL_COLLISION, loadedState.conflict?.conflictType)
        }
    }

    @Test
    fun `when override text is empty, should not allow submission`() = runTest(testDispatcher) {
        // Arrange
        val testConflict = TestDataFactory.createConflictAlert(id = testConflictId)
        fakeConflictRepository.addConflict(testConflict)
        viewModel = createViewModel()

        // Wait for initial load
        viewModel.uiState.test {
            awaitItem() // loading state
            val loadedState = awaitItem() // loaded state

            // Verify initial state has empty override text
            assertTrue(loadedState.manualOverrideText.isEmpty())

            // Act - Try to submit with empty text
            viewModel.handleAction(ConflictResolutionAction.SubmitManualOverride)

            // Assert - Should not process empty override
            expectNoEvents() // No state change should occur
        }
    }

    @Test
    fun `when conflict resolution is successful, should log controller action`() = runTest(testDispatcher) {
        // Arrange
        val testConflict = TestDataFactory.createConflictAlert(id = testConflictId)
        fakeConflictRepository.addConflict(testConflict)
        viewModel = createViewModel()

        // Wait for initial load and accept recommendation
        viewModel.uiState.test {
            awaitItem() // loading state
            awaitItem() // loaded state

            viewModel.handleAction(ConflictResolutionAction.AcceptRecommendation)

            awaitItem() // processing state
            val completedState = awaitItem() // completed state

            assertTrue(completedState.resolutionSuccess)

            // Verify controller action was logged
            val loggedActions = fakeConflictRepository.getControllerActions()
            assertTrue(loggedActions.isNotEmpty())
            assertTrue(loggedActions.any { it.conflictId == testConflictId })
        }
    }

    // Note: onCleared() is protected and cannot be tested directly
}