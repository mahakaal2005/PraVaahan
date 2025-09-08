package com.example.pravaahan.domain.model

/**
 * Represents the current operational status of a train
 */
enum class TrainStatus {
    ON_TIME,
    DELAYED,
    STOPPED,
    MAINTENANCE,
    EMERGENCY
}