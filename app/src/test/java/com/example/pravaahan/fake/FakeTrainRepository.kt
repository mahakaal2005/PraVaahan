package com.example.pravaahan.fake

import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.model.TrainStatus
import com.example.pravaahan.domain.model.Location
import com.example.pravaahan.domain.repository.TrainRepository
import com.example.pravaahan.util.TestDataFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of TrainRepository for testing
 * Provides realistic railway data scenarios and controllable behavior
 */
class FakeTrainRepository : TrainRepository {
    
    private val _trains = MutableStateFlow<List<Train>>(emptyList())
    private val _errors = mutableMapOf<String, Exception>()
    private var _shouldThrowError = false
    private var _networkDelay = 0L
    
    init {
        // Initialize with default test data
        _trains.value = TestDataFactory.createTrainList()
    }
    
    override fun getTrainsRealtime(): Flow<List<Train>> {
        return _trains.asStateFlow()
    }
    
    override fun getTrainById(id: String): Flow<Train?> {
        return _trains.map { trains -> trains.find { it.id == id } }
    }
    
    override suspend fun updateTrainStatus(id: String, status: TrainStatus): Result<Unit> {
        simulateNetworkDelay()
        
        if (_shouldThrowError) {
            return Result.failure(_errors["api_error"] ?: RuntimeException("Failed to update train status"))
        }
        
        val currentTrains = _trains.value.toMutableList()
        val trainIndex = currentTrains.indexOfFirst { it.id == id }
        
        if (trainIndex == -1) {
            return Result.failure(NoSuchElementException("Train with id $id not found"))
        }
        
        currentTrains[trainIndex] = currentTrains[trainIndex].copy(status = status)
        _trains.value = currentTrains
        
        return Result.success(Unit)
    }
    
    override suspend fun updateTrainLocation(id: String, location: Location): Result<Unit> {
        simulateNetworkDelay()
        
        if (_shouldThrowError) {
            return Result.failure(_errors["api_error"] ?: RuntimeException("Failed to update train location"))
        }
        
        val currentTrains = _trains.value.toMutableList()
        val trainIndex = currentTrains.indexOfFirst { it.id == id }
        
        if (trainIndex == -1) {
            return Result.failure(NoSuchElementException("Train with id $id not found"))
        }
        
        currentTrains[trainIndex] = currentTrains[trainIndex].copy(currentLocation = location)
        _trains.value = currentTrains
        
        return Result.success(Unit)
    }
    
    override suspend fun getTrainsByStatus(status: TrainStatus): Result<List<Train>> {
        simulateNetworkDelay()
        
        if (_shouldThrowError) {
            return Result.failure(_errors["api_error"] ?: RuntimeException("Failed to get trains by status"))
        }
        
        val filteredTrains = _trains.value.filter { it.status == status }
        return Result.success(filteredTrains)
    }
    
    override suspend fun getTrainsInSection(sectionId: String): Result<List<Train>> {
        simulateNetworkDelay()
        
        if (_shouldThrowError) {
            return Result.failure(_errors["api_error"] ?: RuntimeException("Failed to get trains in section"))
        }
        
        val filteredTrains = _trains.value.filter { it.currentLocation.sectionId == sectionId }
        return Result.success(filteredTrains)
    }
    
    // Test control methods
    
    /**
     * Sets the trains data for testing
     */
    fun setTrains(trains: List<Train>) {
        _trains.value = trains
    }
    
    /**
     * Adds a single train to the current list
     */
    fun addTrain(train: Train) {
        _trains.value = _trains.value + train
    }
    
    /**
     * Removes a train from the current list
     */
    fun removeTrain(trainId: String) {
        _trains.value = _trains.value.filter { it.id != trainId }
    }
    
    /**
     * Simulates network errors for testing error handling
     */
    fun setShouldThrowError(shouldThrow: Boolean, error: Exception? = null) {
        _shouldThrowError = shouldThrow
        if (error != null) {
            _errors["test_error"] = error
        }
    }
    
    /**
     * Sets network delay for testing loading states
     */
    fun setNetworkDelay(delayMs: Long) {
        _networkDelay = delayMs
    }
    
    /**
     * Clears all data and resets to initial state
     */
    fun reset() {
        _trains.value = TestDataFactory.createTrainList()
        _errors.clear()
        _shouldThrowError = false
        _networkDelay = 0L
    }
    
    /**
     * Simulates real-time updates by updating train data
     */
    fun simulateRealtimeUpdate(trainId: String, status: TrainStatus? = null, location: Location? = null) {
        val currentTrains = _trains.value.toMutableList()
        val trainIndex = currentTrains.indexOfFirst { it.id == trainId }
        
        if (trainIndex != -1) {
            var updatedTrain = currentTrains[trainIndex]
            
            status?.let { updatedTrain = updatedTrain.copy(status = it) }
            location?.let { updatedTrain = updatedTrain.copy(currentLocation = it) }
            
            currentTrains[trainIndex] = updatedTrain
            _trains.value = currentTrains
        }
    }
    
    /**
     * Gets current trains for verification in tests
     */
    fun getCurrentTrains(): List<Train> = _trains.value
    
    private suspend fun simulateNetworkDelay() {
        if (_networkDelay > 0) {
            kotlinx.coroutines.delay(_networkDelay)
        }
    }
}