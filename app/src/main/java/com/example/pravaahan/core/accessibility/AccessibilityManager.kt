package com.example.pravaahan.core.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityManager as AndroidAccessibilityManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.*
import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.RealTimeTrainState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages comprehensive accessibility support for railway control interface
 */
@Singleton
class AccessibilityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "AccessibilityManager"
    }
    
    private val androidAccessibilityManager = 
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AndroidAccessibilityManager
    
    /**
     * Check if accessibility services are enabled
     */
    fun isAccessibilityEnabled(): Boolean {
        return androidAccessibilityManager.isEnabled
    }
    
    /**
     * Check if TalkBack or similar screen reader is active
     */
    fun isTalkBackEnabled(): Boolean {
        return androidAccessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * Generate comprehensive content description for trains
     */
    fun generateTrainDescription(train: Train): String {
        val statusDescription = when (train.status) {
            TrainStatus.ON_TIME -> "on time"
            TrainStatus.DELAYED -> "delayed"
            TrainStatus.STOPPED -> "stopped"
            TrainStatus.CANCELLED -> "cancelled"
            TrainStatus.MAINTENANCE -> "under maintenance"
            TrainStatus.EMERGENCY -> "emergency"
        }
        
        val priorityDescription = when (train.priority) {
            com.example.pravaahan.domain.model.TrainPriority.HIGH -> "high priority"
            com.example.pravaahan.domain.model.TrainPriority.MEDIUM -> "medium priority"
            com.example.pravaahan.domain.model.TrainPriority.LOW -> "low priority"
            com.example.pravaahan.domain.model.TrainPriority.EXPRESS -> "express priority"
        }
        
        return "Train ${train.name}, $statusDescription, $priorityDescription, " +
                "from ${train.currentLocation.name} to ${train.destination.name}"
    }
    
    /**
     * Generate content description for real-time train state
     */
    fun generateRealTimeTrainDescription(trainState: RealTimeTrainState): String {
        val baseDescription = trainState.train?.let { generateTrainDescription(it) } ?: "Unknown train"
        
        val positionInfo = trainState.currentPosition?.let { position ->
            ", current speed ${position.speed.toInt()} kilometers per hour"
        } ?: ""
        
        val connectionInfo = when (trainState.connectionStatus) {
            com.example.pravaahan.domain.model.ConnectionState.CONNECTED -> ", real-time data active"
            com.example.pravaahan.domain.model.ConnectionState.DISCONNECTED -> ", real-time data unavailable"
            com.example.pravaahan.domain.model.ConnectionState.RECONNECTING -> ", reconnecting to real-time data"
            else -> ""
        }
        
        return baseDescription + positionInfo + connectionInfo
    }
    
    /**
     * Generate recommendation description for accessibility
     */
    fun generateRecommendationDescription(recommendation: Any): String {
        return "AI recommendation available. Tap for details."
    }

    /**
     * Log accessibility event
     */
    fun logAccessibilityEvent(event: AccessibilityEvent, message: String) {
        logger.logAccessibilityEvent("AccessibilityManager", event.name, message)
    }

    /**
     * Generate content description for conflict alerts
     */
    fun generateConflictDescription(conflict: ConflictAlert): String {
        val severityDescription = when (conflict.severity) {
            com.example.pravaahan.domain.model.ConflictSeverity.LOW -> "low severity"
            com.example.pravaahan.domain.model.ConflictSeverity.MEDIUM -> "medium severity"
            com.example.pravaahan.domain.model.ConflictSeverity.HIGH -> "high severity"
            com.example.pravaahan.domain.model.ConflictSeverity.CRITICAL -> "critical severity"
        }
        
        val typeDescription = when (conflict.type) {
            com.example.pravaahan.domain.model.ConflictType.POTENTIAL_COLLISION -> "collision risk"
            com.example.pravaahan.domain.model.ConflictType.SCHEDULE_CONFLICT -> "schedule conflict"
            com.example.pravaahan.domain.model.ConflictType.TRACK_CONGESTION -> "track congestion"
            com.example.pravaahan.domain.model.ConflictType.SIGNAL_FAILURE -> "signal failure"
            com.example.pravaahan.domain.model.ConflictType.MAINTENANCE_WINDOW -> "maintenance window"
        }
        
        return "Conflict alert: $typeDescription, $severityDescription, " +
                "involving trains ${conflict.involvedTrains.joinToString(", ")}"
    }
    
    /**
     * Generate map accessibility description
     */
    fun generateMapDescription(
        trainStates: List<RealTimeTrainState>,
        sectionName: String
    ): String {
        val trainCount = trainStates.size
        val activeTrains = trainStates.count { 
            it.train?.status == TrainStatus.ON_TIME || it.train?.status == TrainStatus.DELAYED 
        }
        
        return "Railway section map for $sectionName showing $trainCount trains, " +
                "$activeTrains currently active. Use explore by touch to select trains."
    }
    
    /**
     * Generate connection status description
     */
    fun generateConnectionStatusDescription(
        connectionState: com.example.pravaahan.domain.model.ConnectionState,
        dataQuality: com.example.pravaahan.domain.model.DataQualityIndicators?
    ): String {
        val connectionDescription = when (connectionState) {
            com.example.pravaahan.domain.model.ConnectionState.CONNECTED -> "connected"
            com.example.pravaahan.domain.model.ConnectionState.DISCONNECTED -> "disconnected"
            com.example.pravaahan.domain.model.ConnectionState.RECONNECTING -> "reconnecting"
            com.example.pravaahan.domain.model.ConnectionState.FAILED -> "connection failed"
            com.example.pravaahan.domain.model.ConnectionState.UNKNOWN -> "connection status unknown"
        }
        
        val qualityDescription = dataQuality?.let { quality ->
            val qualityLevel = when {
                quality.overallScore >= 0.8 -> "excellent"
                quality.overallScore >= 0.6 -> "good"
                quality.overallScore >= 0.4 -> "fair"
                else -> "poor"
            }
            ", data quality $qualityLevel"
        } ?: ""
        
        return "Real-time connection $connectionDescription$qualityDescription"
    }
    
    /**
     * Create semantic properties for train cards
     */
    fun createTrainCardSemantics(train: Train): SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = generateTrainDescription(train)
        role = Role.Button
        
        // Add custom actions for train operations
        customActions = listOf(
            CustomAccessibilityAction(
                label = "View train details",
                action = { true }
            ),
            CustomAccessibilityAction(
                label = "Update train status",
                action = { true }
            )
        )
    }
    
    /**
     * Create semantic properties for map elements
     */
    fun createMapSemantics(
        trainStates: List<RealTimeTrainState>,
        sectionName: String
    ): SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = generateMapDescription(trainStates, sectionName)
        role = Role.Image
        
        // Add navigation hints
        customActions = listOf(
            CustomAccessibilityAction(
                label = "Zoom in",
                action = { true }
            ),
            CustomAccessibilityAction(
                label = "Zoom out", 
                action = { true }
            ),
            CustomAccessibilityAction(
                label = "Reset view",
                action = { true }
            )
        )
    }
    
    /**
     * Create semantic properties for conflict alerts
     */
    fun createConflictSemantics(conflict: ConflictAlert): SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = generateConflictDescription(conflict)
        role = Role.Button
        
        // Mark as important for screen readers
        liveRegion = LiveRegionMode.Assertive
        
        customActions = listOf(
            CustomAccessibilityAction(
                label = "Accept recommendation",
                action = { true }
            ),
            CustomAccessibilityAction(
                label = "Override with manual action",
                action = { true }
            ),
            CustomAccessibilityAction(
                label = "View conflict details",
                action = { true }
            )
        )
    }
    
    /**
     * Create semantic properties for connection status
     */
    fun createConnectionStatusSemantics(
        connectionState: com.example.pravaahan.domain.model.ConnectionState,
        dataQuality: com.example.pravaahan.domain.model.DataQualityIndicators?
    ): SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = generateConnectionStatusDescription(connectionState, dataQuality)
        
        // Use live region for connection changes
        liveRegion = LiveRegionMode.Polite
        
        // Add state description
        stateDescription = when (connectionState) {
            com.example.pravaahan.domain.model.ConnectionState.CONNECTED -> "Connected"
            com.example.pravaahan.domain.model.ConnectionState.DISCONNECTED -> "Disconnected"
            com.example.pravaahan.domain.model.ConnectionState.RECONNECTING -> "Reconnecting"
            com.example.pravaahan.domain.model.ConnectionState.FAILED -> "Failed"
            com.example.pravaahan.domain.model.ConnectionState.UNKNOWN -> "Unknown"
        }
    }
    

    
    /**
     * Get accessibility settings
     */
    fun getAccessibilitySettings(): AccessibilitySettings {
        return AccessibilitySettings(
            isEnabled = isAccessibilityEnabled(),
            isTalkBackEnabled = isTalkBackEnabled(),
            touchExplorationEnabled = androidAccessibilityManager.isTouchExplorationEnabled,
            highTextContrastEnabled = false // Would need additional system checks
        )
    }
}



/**
 * Accessibility settings data class
 */
data class AccessibilitySettings(
    val isEnabled: Boolean,
    val isTalkBackEnabled: Boolean,
    val touchExplorationEnabled: Boolean,
    val highTextContrastEnabled: Boolean
)

/**
 * Composable to check accessibility state
 */
@Composable
fun rememberAccessibilityState(): AccessibilitySettings {
    val context = LocalContext.current
    
    return remember {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AndroidAccessibilityManager
        AccessibilitySettings(
            isEnabled = accessibilityManager.isEnabled,
            isTalkBackEnabled = accessibilityManager.isTouchExplorationEnabled,
            touchExplorationEnabled = accessibilityManager.isTouchExplorationEnabled,
            highTextContrastEnabled = false
        )
    }
}