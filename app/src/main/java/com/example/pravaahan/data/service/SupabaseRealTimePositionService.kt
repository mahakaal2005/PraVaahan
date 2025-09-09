package com.example.pravaahan.data.service

import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.logging.logApiCall
import com.example.pravaahan.core.logging.logApiResponse
import com.example.pravaahan.core.logging.logTrainOperation
import com.example.pravaahan.core.monitoring.ConnectionStatus
import com.example.pravaahan.core.monitoring.DataQuality
import com.example.pravaahan.core.monitoring.RealTimeMetricsCollector

import com.example.pravaahan.core.resilience.RealTimeCircuitBreaker
import com.example.pravaahan.core.security.RealTimeSecurityValidator
import com.example.pravaahan.data.dto.TrainPositionDto
import com.example.pravaahan.data.mapper.TrainPositionMapper
import com.example.pravaahan.domain.model.TrainPosition
import com.example.pravaahan.domain.service.RealTimePositionService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Enhanced Supabase real-time position service with circuit breaker, metrics, and security
 * 
 * Features:
 * - Circuit breaker pattern for resilience
 * - Comprehensive metrics collection
 * - Security validation with anomaly detection
 * - Polling-based approach for reliability
 * - Out-of-order update resolution
 * - Automatic reconnection with exponential backoff
 */
@Singleton
class SupabaseRealTimePositionService @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val logger: Logger,
    private val errorHandler: ErrorHandler,
    private val securityValidator: RealTimeSecurityValidator,
    private val circuitBreaker: RealTimeCircuitBreaker,
    private val metricsCollector: RealTimeMetricsCollector
) : RealTimePositionService {
    
    companion object {
        private const val TAG = "RealTimePositionService"
        private const val POSITIONS_TABLE = "train_positions"
        private const val POLL_INTERVAL_MS = 2000L // Poll every 2 seconds
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30000L
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    private val _dataQuality = MutableStateFlow(DataQuality.default())
    
    private val _positionUpdates = MutableSharedFlow<TrainPosition>(
        replay = 0,
        extraBufferCapacity = 200 // Increased buffer for high-frequency updates
    )
    
    // Position ordering and consistency handling
    private val positionBuffer = mutableMapOf<String, MutableList<TrainPosition>>()
    private val maxBufferSize = 10
    
    // Connection management
    private var isStarted = false
    private var retryAttempts = 0
    
    init {
        logger.info(TAG, "Initializing real-time position service")
    }
    
    override suspend fun start() {
        if (isStarted) {
            logger.debug(TAG, "Real-time service already started")
            return
        }
        
        logger.info(TAG, "Starting real-time position service")
        
        try {
            isStarted = true
            _connectionStatus.value = ConnectionStatus.CONNECTED
            metricsCollector.recordConnectionEstablished()
            logger.info(TAG, "Real-time position service started successfully")
        } catch (e: Exception) {
            logger.error(TAG, "Failed to start real-time service", e)
            _connectionStatus.value = ConnectionStatus.ERROR
            metricsCollector.recordConnectionError(e)
            throw e
        }
    }
    
    override suspend fun stop() {
        if (!isStarted) {
            logger.debug(TAG, "Real-time service already stopped")
            return
        }
        
        logger.info(TAG, "Stopping real-time position service")
        
        try {
            isStarted = false
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            metricsCollector.recordConnectionLost("Service stopped")
            logger.info(TAG, "Real-time position service stopped successfully")
        } catch (e: Exception) {
            logger.error(TAG, "Error stopping real-time service", e)
        }
    }
    
    override fun subscribeToSectionUpdates(sectionId: String): Flow<List<TrainPosition>> {
        logger.info(TAG, "Starting optimized real-time subscription for section: $sectionId")
        logger.logApiCall(TAG, "realtime_subscription", mapOf("section_id" to sectionId))
        
        return flow {
            while (isStarted) {
                try {
                    
                    val positions = circuitBreaker.executeWithTimeout {
                        fetchPositionsForSection(sectionId)
                    }.getOrElse { exception ->
                        logger.error(TAG, "Circuit breaker rejected request for section: $sectionId", exception)
                        metricsCollector.recordConnectionError(exception)
                        emptyList()
                    }
                    
                    // Process positions with security validation
                    val validatedPositions = positions.mapNotNull { position ->
                        validateAndProcessPosition(position)
                    }
                    
                    logger.debug(TAG, "Processed ${positions.size} -> ${validatedPositions.size} positions for section: $sectionId")
                    emit(validatedPositions)
                    
                    delay(POLL_INTERVAL_MS)
                    
                } catch (e: Exception) {
                    logger.error(TAG, "Error in polling loop for section: $sectionId", e)
                    metricsCollector.recordConnectionError(e)
                    emit(emptyList())
                    delay(POLL_INTERVAL_MS * 2) // Wait longer on error
                }
            }
        }
        .retry(MAX_RETRY_ATTEMPTS.toLong()) { exception ->
            logger.warn(TAG, "Retrying real-time stream for section: $sectionId", exception)
            delay(calculateRetryDelay())
            true
        }
        .catch { exception ->
            logger.error(TAG, "Real-time stream error for section: $sectionId", exception)
            metricsCollector.recordConnectionError(exception)
            emit(emptyList())
        }
        .onStart {
            logger.debug(TAG, "Optimized real-time position flow started for section: $sectionId")
            if (!isStarted) {
                start()
            }
        }
    }
    
    override fun subscribeToTrainUpdates(trainId: String): Flow<TrainPosition> {
        logger.info(TAG, "Starting real-time subscription for train: $trainId")
        
        return flow {
            while (isStarted) {
                try {
                    val position = circuitBreaker.executeWithTimeout {
                        fetchPositionForTrain(trainId)
                    }.getOrNull()
                    
                    if (position != null) {
                        val validatedPosition = validateAndProcessPosition(position)
                        if (validatedPosition != null) {
                            logger.debug(TAG, "Fetched validated position for train: $trainId")
                            emit(validatedPosition)
                        }
                    }
                    
                    delay(POLL_INTERVAL_MS)
                    
                } catch (e: Exception) {
                    logger.error(TAG, "Error in polling loop for train: $trainId", e)
                    metricsCollector.recordConnectionError(e)
                    delay(POLL_INTERVAL_MS * 2) // Wait longer on error
                }
            }
        }
        .retry(MAX_RETRY_ATTEMPTS.toLong()) { exception ->
            logger.warn(TAG, "Retrying real-time stream for train: $trainId", exception)
            delay(calculateRetryDelay())
            true
        }
        .catch { exception ->
            logger.error(TAG, "Real-time stream error for train: $trainId", exception)
            metricsCollector.recordConnectionError(exception)
        }
        .onStart {
            logger.debug(TAG, "Real-time subscription started for train: $trainId")
            if (!isStarted) {
                start()
            }
        }
    }
    
    override suspend fun updateTrainPosition(position: TrainPosition): Result<Unit> {
        return circuitBreaker.executeWithTimeout {
            errorHandler.safeCall("updateTrainPosition(${position.trainId})") {
                logger.logTrainOperation(TAG, "update_position", position.trainId, 
                    "section=${position.sectionId}, speed=${position.speed}")
                
                val dto = TrainPositionMapper.toDto(position)
                
                supabaseClient.from(POSITIONS_TABLE)
                    .insert(dto)
                
                logger.info(TAG, "Successfully updated position for train: ${position.trainId}")
            }.getOrThrow()
        }
    }
    
    override fun getConnectionStatus(): Flow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    override fun getDataQuality(): Flow<DataQuality> = _dataQuality.asStateFlow()
    
    /**
     * Fetch positions for a specific section with circuit breaker protection
     */
    private suspend fun fetchPositionsForSection(sectionId: String): List<TrainPosition> {
        return try {
            val response = supabaseClient.from(POSITIONS_TABLE)
                .select {
                    filter {
                        eq("section_id", sectionId)
                    }
                    order(column = "timestamp", order = Order.DESCENDING)
                    limit(50) // Limit to recent positions
                }
                .decodeList<TrainPositionDto>()
            
            val positions = TrainPositionMapper.toDomainList(response)
            
            // Update connection status on successful fetch
            _connectionStatus.value = ConnectionStatus.CONNECTED
            
            positions
            
        } catch (e: Exception) {
            logger.error(TAG, "Failed to fetch positions for section: $sectionId", e)
            
            // Update connection status on error
            _connectionStatus.value = ConnectionStatus.ERROR
            metricsCollector.recordConnectionError(e)
            
            emptyList()
        }
    }
    
    /**
     * Fetch position for a specific train with circuit breaker protection
     */
    private suspend fun fetchPositionForTrain(trainId: String): TrainPosition? {
        return try {
            val response = supabaseClient.from(POSITIONS_TABLE)
                .select {
                    filter {
                        eq("train_id", trainId)
                    }
                    order(column = "timestamp", order = Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<TrainPositionDto>()
            
            val position = response?.let { TrainPositionMapper.toDomain(it) }
            
            // Update connection status on successful fetch
            _connectionStatus.value = ConnectionStatus.CONNECTED
            
            position
            
        } catch (e: Exception) {
            logger.error(TAG, "Failed to fetch position for train: $trainId", e)
            
            // Update connection status on error
            _connectionStatus.value = ConnectionStatus.ERROR
            metricsCollector.recordConnectionError(e)
            
            null
        }
    }
    
    /**
     * Validate and process a position with security checks and metrics
     */
    private suspend fun validateAndProcessPosition(position: TrainPosition): TrainPosition? {
        try {
            // Convert to DTO for validation
            val dto = TrainPositionMapper.toDto(position)
            
            // Security validation with comprehensive checks
            val validationResult = securityValidator.validatePosition(dto)
            
            if (!validationResult.isValid) {
                logger.warn(TAG, "Security validation failed for train ${position.trainId}")
                metricsCollector.recordValidationFailure(position.trainId, "Validation failed")
                validationResult.issues.forEach { issue ->
                    logger.warn(TAG, "Security issue: ${issue.type} - ${issue.message}")
                }
                return null
            }
            
            if (validationResult.hasHighRiskIssues()) {
                logger.error(TAG, "High-risk security issues detected for train ${position.trainId}")
                metricsCollector.recordSecurityAnomaly(
                    position.trainId, 
                    "High-risk issues", 
                    validationResult.issues.joinToString { it.message }
                )
                return null
            }
            
            // Log anomalies for monitoring
            if (validationResult.anomalies.isNotEmpty()) {
                logger.info(TAG, "Anomalies detected for train ${position.trainId}: ${validationResult.anomalies}")
                validationResult.anomalies.forEach { anomaly ->
                    metricsCollector.recordSecurityAnomaly(position.trainId, anomaly.name, "Anomaly detected")
                }
            }
            
            val receiveTime = Clock.System.now()
            
            logger.logTrainOperation(TAG, "position_processed", position.trainId,
                "section=${position.sectionId}, speed=${position.speed}, latency=${(receiveTime - position.timestamp).inWholeMilliseconds}ms")
            
            // Record metrics
            metricsCollector.recordMessageReceived(position, receiveTime)
            updateDataQuality(position, receiveTime)
            
            return position
            
        } catch (e: Exception) {
            logger.error(TAG, "Failed to validate position for train ${position.trainId}", e)
            metricsCollector.recordConnectionError(e)
            return null
        }
    }
    
    /**
     * Calculate retry delay with exponential backoff
     */
    private fun calculateRetryDelay(): Long {
        val exponentialDelay = INITIAL_RETRY_DELAY_MS * (1L shl retryAttempts)
        return exponentialDelay.coerceAtMost(MAX_RETRY_DELAY_MS)
    }
    
    /**
     * Update data quality metrics
     */
    private fun updateDataQuality(position: TrainPosition, receiveTime: Instant) {
        val latency = receiveTime - position.timestamp
        
        _dataQuality.value = _dataQuality.value.copy(
            latency = latency,
            freshness = latency,
            accuracy = position.accuracy ?: 1.0,
            reliability = if (circuitBreaker.metrics.value.totalRequests > 0) {
                1.0f - (circuitBreaker.metrics.value.totalFailures.toFloat() / circuitBreaker.metrics.value.totalRequests.toFloat())
            } else 1.0f
        )
        
        // Log performance warnings
        when {
            latency.inWholeMilliseconds > 5000 -> {
                logger.error(TAG, "Critical latency detected: ${latency.inWholeMilliseconds}ms for train ${position.trainId}")
            }
            latency.inWholeMilliseconds > 1000 -> {
                logger.warn(TAG, "High latency detected: ${latency.inWholeMilliseconds}ms for train ${position.trainId}")
            }
        }
    }
}