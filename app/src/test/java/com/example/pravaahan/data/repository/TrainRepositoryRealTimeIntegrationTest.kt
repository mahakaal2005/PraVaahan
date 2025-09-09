package com.example.pravaahan.data.repository

import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.data.service.SupabaseRealTimePositionService
import com.example.pravaahan.domain.model.*
import com.example.pravaahan.fake.FakeTrainRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test for TrainRepository real-time capabilities
 */
@ExtendWith(MockKExtension::class)
class TrainRepositoryRealTimeIntegrationTest {
    
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var logger: Logger
    private lateinit var errorHandler: ErrorHandler
    private lateinit var fakeRepository: FakeTrainRepository
    
    @BeforeEach
    fun setup() {
        supabaseClient = mockk()
        logger = mockk(relaxed = true)
        errorHandler = mockk()
        fakeRepository = FakeTrainRepository()
    }
    
    @Test
    fun `real-time position integration works correctly`() = runTest {
        // Arrange
        val sectionId = "section_1"
        val testPosition = TrainPosition(
            trainId = "TEST_TRAIN_001",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 85.5,
            heading = 45.0,
            timestamp = Clock.System.now(),
            sectionId = sectionId,
            accuracy = 5.2
        )
        
        // Add position to fake repository
        fakeRepository.addTrainPosition(testPosition)
        
        // Act
        val positions = fakeRepository.getTrainPositionsRealtime(sectionId).first()
        
        // Assert
        assertEquals(1, positions.size)
        assertEquals(testPosition.trainId, positions[0].trainId)
        assertEquals(testPosition.sectionId, positions[0].sectionId)
        assertEquals(testPosition.speed, positions[0].speed)
    }
    
    @Test
    fun `real-time train states integration works correctly`() = runTest {
        // Arrange
        val sectionId = "section_1"
        val testPosition = TrainPosition(
            trainId = "TEST_TRAIN_001",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 85.5,
            heading = 45.0,
            timestamp = Clock.System.now(),
            sectionId = sectionId,
            accuracy = 5.2
        )
        
        // Add train and position to fake repository
        val testTrain = Train(
            id = testPosition.trainId,
            name = "Test Train",
            trainNumber = "12345",
            currentLocation = Location(
                latitude = testPosition.latitude,
                longitude = testPosition.longitude,
                sectionId = sectionId
            ),
            destination = Location(
                latitude = 28.7041,
                longitude = 77.1025,
                sectionId = sectionId
            ),
            status = TrainStatus.ON_TIME,
            priority = TrainPriority.MEDIUM,
            speed = testPosition.speed,
            estimatedArrival = Clock.System.now().plus(kotlin.time.Duration.parse("1h")),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        fakeRepository.addTrain(testTrain)
        fakeRepository.addTrainPosition(testPosition)
        
        // Act
        val trainStates = fakeRepository.getRealTimeTrainStates(sectionId).first()
        
        // Assert
        assertTrue(trainStates.isNotEmpty())
        val trainState = trainStates.find { it.trainId == testPosition.trainId }
        assertEquals(testPosition.trainId, trainState?.trainId)
        assertEquals(testPosition, trainState?.currentPosition)
        assertTrue(trainState?.dataQuality?.overallScore ?: 0.0 > 0.8)
    }
    
    @Test
    fun `update train position works correctly`() = runTest {
        // Arrange
        val testPosition = TrainPosition(
            trainId = "TEST_TRAIN_002",
            latitude = 28.6200,
            longitude = 77.2150,
            speed = 92.3,
            heading = 90.0,
            timestamp = Clock.System.now(),
            sectionId = "section_1",
            accuracy = 3.8
        )
        
        // Act
        val result = fakeRepository.updateTrainPosition(testPosition)
        
        // Assert
        assertTrue(result.isSuccess)
        
        // Verify position was added
        val positions = fakeRepository.getTrainPositionsRealtime("section_1").first()
        val addedPosition = positions.find { it.trainId == testPosition.trainId }
        assertEquals(testPosition, addedPosition)
    }
}