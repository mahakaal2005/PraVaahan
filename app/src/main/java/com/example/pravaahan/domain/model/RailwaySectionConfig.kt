package com.example.pravaahan.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Configuration for a railway section map layout
 */
data class RailwaySectionConfig(
    val sectionId: String,
    val name: String,
    val tracks: List<Track>,
    val stations: List<Station>,
    val signals: List<Signal>,
    val bounds: MapBounds
)

/**
 * Railway station representation
 */
data class Station(
    val id: String,
    val name: String,
    val position: Offset,
    val type: StationType = StationType.REGULAR
)

/**
 * Railway signal representation
 */
data class Signal(
    val id: String,
    val position: Offset,
    val type: SignalType,
    val status: RailwaySignalStatus = RailwaySignalStatus.GREEN
)

/**
 * Map coordinate bounds
 */
data class MapBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
}

/**
 * Station types following railway industry standards
 */
enum class StationType {
    MAJOR,      // Major junction or terminal
    REGULAR,    // Regular passenger station
    JUNCTION,   // Track junction point
    DEPOT       // Maintenance depot
}

/**
 * Signal types following railway industry standards
 */
enum class SignalType {
    HOME,       // Home signal
    DISTANT,    // Distant signal
    SHUNT,      // Shunting signal
    AUTOMATIC   // Automatic block signal
}

/**
 * Railway signal status following railway color coding
 */
enum class RailwaySignalStatus(val color: Color) {
    GREEN(Color(0xFF00C853)),    // Proceed
    YELLOW(Color(0xFFFFD600)),   // Caution
    RED(Color(0xFFD32F2F)),      // Stop
    DOUBLE_YELLOW(Color(0xFFFFD600)), // Preliminary caution
    OFF(Color(0xFF757575))       // Signal off/dark
}