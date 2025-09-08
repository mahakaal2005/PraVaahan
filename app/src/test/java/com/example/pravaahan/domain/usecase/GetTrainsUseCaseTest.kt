package com.example.pravaahan.domain.usecase

import app.cash.turbine.test
import com.example.pravaahan.domain.model.TrainStatus
import com.example.pravaahan.fake.FakeTrainRepository
import com.example.pravaahan.util.CoroutineTestExtension
import com.example.pravaahan.util.TestDataFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith


/**
 * Tests for GetTrainsUseCase
 * Covers business logic for retrieving and processing train data
 */
@ExtendWith(CoroutineTestExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
class GetTrainsUseCaseTest {

    private lateinit var fakeRepository: FakeTrainRepository
    private lateinit var useCase: GetTrainsUseCase

    @BeforeEach
    fun setup() {
        fakeRepository = FakeTrainRepository()
        useCase = GetTrainsUseCase(fakeRepository)
    }

    @Test
    fun `when trains are available, should return all trains`() = runTest {
        // Arrange
        val testTrains = TestDataFactory.createTrainList()
        fakeRepository.setTrains(testTrains)

        // Act & Assert
        useCase().test {
            val trains = awaitItem()
            assertEquals(testTrains.size, trains.size)
            assertEquals(testTrains, trains)
        }
    }

    @Test
    fun `when no trains are available, should return empty list`() = runTest {
        // Arrange
        fakeRepository.setTrains(emptyList())

        // Act & Assert
        useCase().test {
            val trains = awaitItem()
            assertTrue(trains.isEmpty())
        }
    }

    @Test
    fun `when trains have different priorities, should return all trains`() = runTest {
        // Arrange
        val testTrains = listOf(
            TestDataFactory.createTrain(
                id = "express_001",
                priority = com.example.pravaahan.domain.model.TrainPriority.EXPRESS
            ),
            TestDataFactory.createTrain(
                id = "high_001",
                priority = com.example.pravaahan.domain.model.TrainPriority.HIGH
            ),
            TestDataFactory.createTrain(
                id = "medium_001",
                priority = com.example.pravaahan.domain.model.TrainPriority.MEDIUM
            ),
            TestDataFactory.createTrain(
                id = "low_001",
                priority = com.example.pravaahan.domain.model.TrainPriority.LOW
            )
        )
        fakeRepository.setTrains(testTrains)

        // Act & Assert
        useCase().test {
            val trains = awaitItem()
            assertEquals(4, trains.size)
            
            // Verify all priority levels are present
            val priorities = trains.map { it.priority }.toSet()
            assertTrue(priorities.contains(com.example.pravaahan.domain.model.TrainPriority.EXPRESS))
            assertTrue(priorities.contains(com.example.pravaahan.domain.model.TrainPriority.HIGH))
            assertTrue(priorities.contains(com.example.pravaahan.domain.model.TrainPriority.MEDIUM))
            assertTrue(priorities.contains(com.example.pravaahan.domain.model.TrainPriority.LOW))
        }
    }

    @Test
    fun `when trains have different statuses, should return all trains`() = runTest {
        // Arrange
        val testTrains = listOf(
            TestDataFactory.createTrain(id = "on_time", status = TrainStatus.ON_TIME),
            TestDataFactory.createTrain(id = "delayed", status = TrainStatus.DELAYED),
            TestDataFactory.createTrain(id = "stopped", status = TrainStatus.STOPPED),
            TestDataFactory.createTrain(id = "maintenance", status = TrainStatus.MAINTENANCE)
        )
        fakeRepository.setTrains(testTrains)

        // Act & Assert
        useCase().test {
            val trains = awaitItem()
            assertEquals(4, trains.size)
            
            // Verify all status types are present
            val statuses = trains.map { it.status }.toSet()
            assertTrue(statuses.contains(TrainStatus.ON_TIME))
            assertTrue(statuses.contains(TrainStatus.DELAYED))
            assertTrue(statuses.contains(TrainStatus.STOPPED))
            assertTrue(statuses.contains(TrainStatus.MAINTENANCE))
        }
    }

    @Test
    fun `when real-time updates occur, should emit updated train list`() = runTest {
        // Arrange
        val initialTrains = listOf(
            TestDataFactory.createTrain(id = "train_001", status = TrainStatus.ON_TIME)
        )
        fakeRepository.setTrains(initialTrains)

        // Act & Assert
        useCase().test {
            // Initial state
            val initialTrainList = awaitItem()
            assertEquals(TrainStatus.ON_TIME, initialTrainList.first().status)

            // Simulate real-time update
            fakeRepository.simulateRealtimeUpdate("train_001", TrainStatus.DELAYED)

            // Updated state
            val updatedTrainList = awaitItem()
            assertEquals(TrainStatus.DELAYED, updatedTrainList.first().status)
        }
    }

    @Test
    fun `when trains are added dynamically, should emit updated list`() = runTest {
        // Arrange
        val initialTrains = listOf(TestDataFactory.createTrain(id = "train_001"))
        fakeRepository.setTrains(initialTrains)

        // Act & Assert
        useCase().test {
            // Initial state
            val initialList = awaitItem()
            assertEquals(1, initialList.size)

            // Add new train
            val newTrain = TestDataFactory.createTrain(id = "train_002", name = "New Express")
            fakeRepository.addTrain(newTrain)

            // Updated state
            val updatedList = awaitItem()
            assertEquals(2, updatedList.size)
            assertTrue(updatedList.any { it.id == "train_002" })
        }
    }

    @Test
    fun `when trains are removed dynamically, should emit updated list`() = runTest {
        // Arrange
        val initialTrains = listOf(
            TestDataFactory.createTrain(id = "train_001"),
            TestDataFactory.createTrain(id = "train_002")
        )
        fakeRepository.setTrains(initialTrains)

        // Act & Assert
        useCase().test {
            // Initial state
            val initialList = awaitItem()
            assertEquals(2, initialList.size)

            // Remove train
            fakeRepository.removeTrain("train_001")

            // Updated state
            val updatedList = awaitItem()
            assertEquals(1, updatedList.size)
            assertTrue(updatedList.none { it.id == "train_001" })
            assertTrue(updatedList.any { it.id == "train_002" })
        }
    }

    @Test
    fun `when repository provides trains with edge case data, should handle gracefully`() = runTest {
        // Arrange - Create trains with edge case values
        val edgeCaseTrains = listOf(
            TestDataFactory.createTrain(
                id = "edge_001",
                speed = 0.0, // Stopped train
                estimatedArrival = kotlinx.datetime.Clock.System.now() // Arriving now
            ),
            TestDataFactory.createTrain(
                id = "edge_002",
                speed = 200.0, // Very fast train
                currentLocation = TestDataFactory.createLocation(0.0, 0.0, "UNKNOWN") // Unknown location
            )
        )
        fakeRepository.setTrains(edgeCaseTrains)

        // Act & Assert
        useCase().test {
            val trains = awaitItem()
            assertEquals(2, trains.size)
            
            val stoppedTrain = trains.find { it.id == "edge_001" }
            assertEquals(0.0, stoppedTrain?.speed)
            
            val fastTrain = trains.find { it.id == "edge_002" }
            assertEquals(200.0, fastTrain?.speed)
            assertEquals("UNKNOWN", fastTrain?.currentLocation?.sectionId)
        }
    }

    @Test
    fun `when multiple real-time updates occur rapidly, should handle all updates`() = runTest {
        // Arrange
        val train = TestDataFactory.createTrain(id = "rapid_update_train", status = TrainStatus.ON_TIME)
        fakeRepository.setTrains(listOf(train))

        // Act & Assert
        useCase().test {
            // Initial state
            val initialList = awaitItem()
            assertEquals(TrainStatus.ON_TIME, initialList.first().status)

            // Rapid updates
            fakeRepository.simulateRealtimeUpdate("rapid_update_train", TrainStatus.DELAYED)
            val delayedList = awaitItem()
            assertEquals(TrainStatus.DELAYED, delayedList.first().status)

            fakeRepository.simulateRealtimeUpdate("rapid_update_train", TrainStatus.STOPPED)
            val stoppedList = awaitItem()
            assertEquals(TrainStatus.STOPPED, stoppedList.first().status)

            fakeRepository.simulateRealtimeUpdate("rapid_update_train", TrainStatus.ON_TIME)
            val backOnTimeList = awaitItem()
            assertEquals(TrainStatus.ON_TIME, backOnTimeList.first().status)
        }
    }
}