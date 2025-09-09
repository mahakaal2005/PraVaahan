package com.example.pravaahan.data.mapper

import com.example.pravaahan.core.monitoring.ConnectionStatus as MonitoringConnectionStatus
import com.example.pravaahan.core.monitoring.DataQuality as MonitoringDataQuality
// Removed unused import
import com.example.pravaahan.domain.model.ConnectionState
import com.example.pravaahan.domain.model.DataQualityIndicators
import com.example.pravaahan.domain.model.NetworkQuality
import com.example.pravaahan.domain.model.ValidationStatus
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Mapper functions to convert between monitoring and domain model types
 */
object RealTimeStateMapper {
    
    /**
     * Convert monitoring ConnectionStatus to domain ConnectionStatus
     */
    fun toDomainConnectionStatus(monitoringStatus: MonitoringConnectionStatus): ConnectionState {
        val connectionState = when (monitoringStatus) {
            MonitoringConnectionStatus.CONNECTED -> ConnectionState.CONNECTED
            MonitoringConnectionStatus.DISCONNECTED -> ConnectionState.DISCONNECTED
            MonitoringConnectionStatus.RECONNECTING -> ConnectionState.RECONNECTING
            MonitoringConnectionStatus.ERROR -> ConnectionState.FAILED
        }
        
        return connectionState
    }
    
    /**
     * Convert monitoring DataQuality to domain DataQualityIndicators
     */
    fun toDomainDataQuality(monitoringQuality: MonitoringDataQuality): DataQualityIndicators {
        // Convert latency to freshness score (0.0 to 1.0)
        val freshnessScore = when {
            monitoringQuality.latency < 5.seconds -> 1.0
            monitoringQuality.latency < 30.seconds -> 0.8
            monitoringQuality.latency < 60.seconds -> 0.6
            monitoringQuality.latency < 300.seconds -> 0.4
            else -> 0.2
        }
        
        return DataQualityIndicators(
            latency = monitoringQuality.latency.inWholeMilliseconds,
            accuracy = monitoringQuality.accuracy,
            completeness = freshnessScore,
            signalStrength = 0.8, // Default value since not available in monitoring quality
            gpsAccuracy = monitoringQuality.accuracy,
            dataFreshness = monitoringQuality.freshness.inWholeMilliseconds,
            validationStatus = ValidationStatus.VALID,
            sourceReliability = monitoringQuality.reliability.toDouble(),
            anomalyFlags = emptyList() // Convert from monitoring if needed
        )
    }
    
    /**
     * Convert domain ConnectionStatus to monitoring ConnectionStatus
     */
    fun toMonitoringConnectionStatus(domainStatus: ConnectionState): MonitoringConnectionStatus {
        return when (domainStatus) {
            ConnectionState.CONNECTED -> MonitoringConnectionStatus.CONNECTED
            ConnectionState.DISCONNECTED -> MonitoringConnectionStatus.DISCONNECTED
            ConnectionState.RECONNECTING -> MonitoringConnectionStatus.RECONNECTING
            ConnectionState.FAILED -> MonitoringConnectionStatus.ERROR
            ConnectionState.UNKNOWN -> MonitoringConnectionStatus.DISCONNECTED
        }
    }
    
    /**
     * Convert domain DataQualityIndicators to monitoring DataQuality
     */
    fun toMonitoringDataQuality(domainQuality: DataQualityIndicators): MonitoringDataQuality {
        // Convert freshness timestamp back to latency (approximate)
        val latency = when {
            domainQuality.dataFreshness < 2000 -> 2.seconds
            domainQuality.dataFreshness < 15000 -> 15.seconds
            domainQuality.dataFreshness < 45000 -> 45.seconds
            domainQuality.dataFreshness < 150000 -> 150.seconds
            else -> 300.seconds
        }
        
        return MonitoringDataQuality(
            latency = latency,
            accuracy = domainQuality.gpsAccuracy,
            freshness = latency,
            reliability = domainQuality.sourceReliability.toFloat()
        )
    }
}