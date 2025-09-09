package com.example.pravaahan.domain.model

/**
 * Represents different types of anomaly flags for data validation
 */
enum class AnomalyFlag {
    SPEED_ANOMALY,
    POSITION_ANOMALY,
    TIMING_ANOMALY,
    SIGNAL_ANOMALY,
    DATA_CORRUPTION,
    GEOFENCE_VIOLATION,
    POSITION_JUMP,
    TEMPORAL_INCONSISTENCY,
    OUT_OF_SEQUENCE,
    DUPLICATE_DATA,
    SUSPICIOUS,
    SIGNAL_INTERFERENCE,
    NONE
}