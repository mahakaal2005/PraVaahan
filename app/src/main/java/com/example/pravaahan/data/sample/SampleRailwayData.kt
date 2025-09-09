package com.example.pravaahan.data.sample

import androidx.compose.ui.geometry.Offset
import com.example.pravaahan.domain.model.*

/**
 * Sample railway section data for testing and demonstration
 */
object SampleRailwayData {
    
    /**
     * Create a sample railway section configuration
     */
    fun createSampleSection(): RailwaySectionConfig {
        return RailwaySectionConfig(
            sectionId = "section_1",
            name = "Delhi-Gurgaon Section",
            tracks = createSampleTracks(),
            stations = createSampleStations(),
            signals = createSampleSignals(),
            bounds = MapBounds(
                minX = 77.0f,
                maxX = 77.3f,
                minY = 28.4f,
                maxY = 28.7f
            )
        )
    }
    
    private fun createSampleTracks(): List<Track> {
        return listOf(
            Track(
                id = "track_1",
                sectionId = "section_1",
                sectionName = "Main Line 1",
                capacity = 1,
                isActive = true,
                signalStatus = SignalStatus.GREEN,
                startLatitude = 28.6139,
                startLongitude = 77.2090,
                endLatitude = 28.4595,
                endLongitude = 77.0266
            ),
            Track(
                id = "track_2",
                sectionId = "section_1",
                sectionName = "Main Line 2",
                capacity = 1,
                isActive = true,
                signalStatus = SignalStatus.GREEN,
                startLatitude = 28.6100,
                startLongitude = 77.2100,
                endLatitude = 28.4600,
                endLongitude = 77.0300
            ),
            Track(
                id = "track_3",
                sectionId = "section_1",
                sectionName = "Siding Track",
                capacity = 1,
                isActive = true,
                signalStatus = SignalStatus.YELLOW,
                startLatitude = 28.5800,
                startLongitude = 77.1500,
                endLatitude = 28.5000,
                endLongitude = 77.1000
            )
        )
    }
    
    private fun createSampleStations(): List<Station> {
        return listOf(
            Station(
                id = "station_1",
                name = "Delhi Junction",
                position = Offset(77.21f, 28.61f),
                type = StationType.MAJOR
            ),
            Station(
                id = "station_2",
                name = "Gurgaon Central",
                position = Offset(77.03f, 28.46f),
                type = StationType.MAJOR
            ),
            Station(
                id = "station_3",
                name = "Intermediate Station",
                position = Offset(77.12f, 28.54f),
                type = StationType.REGULAR
            ),
            Station(
                id = "junction_1",
                name = "Track Junction",
                position = Offset(77.15f, 28.58f),
                type = StationType.JUNCTION
            ),
            Station(
                id = "depot_1",
                name = "Maintenance Depot",
                position = Offset(77.08f, 28.50f),
                type = StationType.DEPOT
            )
        )
    }
    
    private fun createSampleSignals(): List<Signal> {
        return listOf(
            Signal(
                id = "signal_1",
                position = Offset(77.20f, 28.60f),
                type = SignalType.HOME,
                status = RailwaySignalStatus.GREEN
            ),
            Signal(
                id = "signal_2",
                position = Offset(77.18f, 28.58f),
                type = SignalType.DISTANT,
                status = RailwaySignalStatus.GREEN
            ),
            Signal(
                id = "signal_3",
                position = Offset(77.12f, 28.54f),
                type = SignalType.AUTOMATIC,
                status = RailwaySignalStatus.YELLOW
            ),
            Signal(
                id = "signal_4",
                position = Offset(77.05f, 28.48f),
                type = SignalType.HOME,
                status = RailwaySignalStatus.GREEN
            ),
            Signal(
                id = "signal_5",
                position = Offset(77.15f, 28.56f),
                type = SignalType.SHUNT,
                status = RailwaySignalStatus.RED
            )
        )
    }
    
    /**
     * Create sample train states for testing
     */
    fun createSampleTrainStates(): List<RealTimeTrainState> {
        return listOf(
            RealTimeTrainState(
                train = Train(
                    id = "train_1",
                    name = "Express 101",
                    trainNumber = "12345",
                    currentLocation = Location(28.6139, 77.2090, "section_1", "New Delhi"),
                    destination = Location(28.4595, 77.0266, "section_1", "Gurgaon"),
                    status = TrainStatus.ON_TIME,
                    priority = TrainPriority.HIGH,
                    speed = 85.0,
                    estimatedArrival = kotlinx.datetime.Clock.System.now().plus(kotlin.time.Duration.parse("2h")),
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now()
                ),
                currentPosition = TrainPosition(
                    trainId = "train_1",
                    latitude = 28.6139,
                    longitude = 77.2090,
                    speed = 85.0,
                    heading = 45.0,
                    timestamp = kotlinx.datetime.Clock.System.now(),
                    sectionId = "section_1",
                    accuracy = 0.95
                ),
                connectionStatus = ConnectionState.CONNECTED,
                dataQuality = DataQualityIndicators(
                    latency = 50L,
                    accuracy = 0.95,
                    completeness = 0.92,
                    signalStrength = 0.9,
                    gpsAccuracy = 5.0,
                    dataFreshness = 1000L,
                    validationStatus = ValidationStatus.VALID,
                    sourceReliability = 0.95,
                    anomalyFlags = emptyList()
                ),
                lastUpdate = kotlinx.datetime.Clock.System.now()
            ),
            RealTimeTrainState(
                train = Train(
                    id = "train_2",
                    name = "Passenger 202",
                    trainNumber = "67890",
                    currentLocation = Location(19.0760, 72.8777, "section_1", "Mumbai Central"),
                    destination = Location(18.5204, 73.8567, "section_1", "Pune Junction"),
                    status = TrainStatus.ON_TIME,
                    priority = TrainPriority.MEDIUM,
                    speed = 65.0,
                    estimatedArrival = kotlinx.datetime.Clock.System.now().plus(kotlin.time.Duration.parse("3h")),
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now()
                ),
                currentPosition = TrainPosition(
                    trainId = "train_2",
                    latitude = 19.0760,
                    longitude = 72.8777,
                    speed = 65.0,
                    heading = 225.0,
                    timestamp = kotlinx.datetime.Clock.System.now(),
                    sectionId = "section_1",
                    accuracy = 0.88
                ),
                connectionStatus = ConnectionState.CONNECTED,
                dataQuality = DataQualityIndicators(
                    latency = 80L,
                    accuracy = 0.88,
                    completeness = 0.85,
                    signalStrength = 0.7,
                    gpsAccuracy = 8.0,
                    dataFreshness = 1500L,
                    validationStatus = ValidationStatus.VALID,
                    sourceReliability = 0.88,
                    anomalyFlags = emptyList()
                ),
                lastUpdate = kotlinx.datetime.Clock.System.now()
            ),
            RealTimeTrainState(
                train = Train(
                    id = "train_3",
                    name = "Freight 303",
                    trainNumber = "11111",
                    currentLocation = Location(13.0827, 80.2707, "section_1", "Chennai Central"),
                    destination = Location(12.9716, 77.5946, "section_1", "Bangalore City"),
                    status = TrainStatus.STOPPED,
                    priority = TrainPriority.LOW,
                    speed = 0.0,
                    estimatedArrival = kotlinx.datetime.Clock.System.now().plus(kotlin.time.Duration.parse("4h")),
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now()
                ),
                currentPosition = TrainPosition(
                    trainId = "train_3",
                    latitude = 13.0827,
                    longitude = 80.2707,
                    speed = 0.0,
                    heading = 0.0,
                    timestamp = kotlinx.datetime.Clock.System.now(),
                    sectionId = "section_1",
                    accuracy = 1.0
                ),
                connectionStatus = ConnectionState.CONNECTED,
                dataQuality = DataQualityIndicators(
                    latency = 30L,
                    accuracy = 1.0,
                    completeness = 0.95,
                    signalStrength = 0.8,
                    gpsAccuracy = 3.0,
                    dataFreshness = 500L,
                    validationStatus = ValidationStatus.VALID,
                    sourceReliability = 1.0,
                    anomalyFlags = emptyList()
                ),
                lastUpdate = kotlinx.datetime.Clock.System.now()
            )
        )
    }
}