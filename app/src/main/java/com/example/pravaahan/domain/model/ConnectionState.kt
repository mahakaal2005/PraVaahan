package com.example.pravaahan.domain.model

/**
 * Represents the connection state for real-time data
 */
enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    FAILED,
    UNKNOWN
}

// Alias for backward compatibility
typealias ConnectionStatus = ConnectionState