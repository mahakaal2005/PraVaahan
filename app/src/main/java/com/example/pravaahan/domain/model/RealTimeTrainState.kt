package com.example.pravaahan.domain.model

import kotlinx.datetime.Instant

/**
 * Represents the real-time state of a train
 */
data class RealTimeTrainState(
    val train: Train,
    val currentPosition: TrainPosition?,
    val connectionStatus: ConnectionState,
    val dataQuality: DataQualityIndicators?,
    val lastUpdate: Instant,
    val state: ConnectionState = connectionStatus,
    val lastUpdateTime: Instant = lastUpdate
) {
    val trainId: String get() = train.id
}

/**
 * Data quality indicators for real-time data
 */
data class DataQualityIndicators(
    val latency: Long,
    val accuracy: Double,
    val completeness: Double,
    val signalStrength: Double = 0.0,
    val gpsAccuracy: Double = 0.0,
    val dataFreshness: Long = 0L,
    val validationStatus: ValidationStatus = ValidationStatus.UNKNOWN,
    val sourceReliability: Double = 0.0,
    val anomalyFlags: List<AnomalyFlag> = emptyList()
) {
    val overallScore: Double
        get() = (accuracy + completeness + signalStrength + gpsAccuracy + sourceReliability) / 5.0
}