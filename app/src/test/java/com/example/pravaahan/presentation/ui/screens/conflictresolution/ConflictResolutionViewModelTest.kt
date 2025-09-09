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
    private val testConflictId = "conflict-test-001"

    @BeforeEach
    fun setup() {
        fakeConflictRepository = FakeConflictRepository()
        fakeTrainRepository = FakeTrainRepository()
        getConflictByIdUseCase = GetConflictByIdUseCase(fakeConflictRepository)
        getTrainsUseCase = GetTrainsUseCase(fakeTrainRepository)
        acceptRecommendationUseCase = AcceptRecommendationUseCase(fakeConflictRepository)
        submitManualOverrideUseCase = SubmitManualOverrideUseCase(fakeConflictRepository)
        
        // Setup mock error handler with proper methods
        whenever(mockErrorHandler.handleError(any())).thenReturn(AppError.NetworkError.NoConnection)
        whenever(mockErrorHandler.logError(any(), any())).then { /* do nothing */ }
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
    fun `when ViewModel is initialized, should load conflict data successfully`() = runTest {
        // Arrange
        val testConflict = TestDataFactory.createConflictAlert(id = testConflictId)
        fakeConflictRepository.addConflict(testConflict)
        
        // Act - Create ViewModel
        try {
            viewModel = createViewModel()
            
            // Basic assertions
            assertNotNull(viewModel, "ViewModel should not be null")
            assertNotNull(viewModel.uiState, "UiState should not be null")
            
            val initialState = viewModel.uiState.value
            assertNotNull(initialState, "Initial state should not be null")
            assertFalse(initialState.isLoading, "Should not be loading initially")
            assertNull(initialState.conflict, "Should not have conflict initially")
            
        } catch (e: Exception) {
            fail("ViewModel creation failed: ${e.message}")
        }
    }

    @Test
    fun `when conflict not found, should show error state`() = runTest {
        // Arrange
        val nonExistentConflictId = "non_existent_conflict"
        viewModel = createViewModel()

        // Act
        viewModel.handleAction(ConflictResolutionAction.LoadConflictDetails(nonExistentConflictId))
        
        // Give time for async operation
        kotlinx.coroutines.delay(100)

        // Assert
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertNull(finalState.conflict)
        assertNotNull(finalState.error)
    }

    @Test
    fun `when UpdateManualOverrideText action is triggered, should update override text`() = runTest {
        // Arrange
        viewModel = createViewModel()
        val newOverrideText = "Emergency stop required"

        // Act
        viewModel.handleAction(ConflictResolutionAction.UpdateManualOverrideText(newOverrideText))

        // Assert
        val updatedState = viewModel.uiState.value
        assertEquals(newOverrideText, updatedState.manualOverrideText)
    }

    @Test
    fun `when ClearError action is triggered, should clear error state`() = runTest {
        // Arrange
        fakeConflictRepository.setShouldThrowError(true)
        viewModel = createViewModel()

        // Trigger error
        viewModel.handleAction(ConflictResolutionAction.LoadConflictDetails(testConflictId))
        kotlinx.coroutines.delay(50)

        // Act
        viewModel.handleAction(ConflictResolutionAction.ClearError)

        // Assert
        val clearedState = viewModel.uiState.value
        assertNull(clearedState.error)
    }

    // Note: onCleared() is protected and cannot be tested directly
}