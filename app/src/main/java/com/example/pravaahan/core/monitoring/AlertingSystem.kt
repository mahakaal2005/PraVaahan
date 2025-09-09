package com.example.pravaahan.core.monitoring

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.logging.logSecurityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Intelligent alerting system with escalation procedures for railway monitoring.
 * Manages alert priorities, escalation chains, and notification delivery.
 */
@Singleton
class AlertingSystem @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "AlertingSystem"
        private const val MAX_ALERTS_HISTORY = 1000
        private const val ESCALATION_DELAY_MINUTES = 5L
        private const val ALERT_SUPPRESSION_MINUTES = 2L
        private const val CRITICAL_ALERT_IMMEDIATE_ESCALATION = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _activeAlerts = MutableStateFlow<List<Alert>>(emptyList())
    val activeAlerts: StateFlow<List<Alert>> = _activeAlerts.asStateFlow()
    
    private val _alertHistory = MutableStateFlow<List<Alert>>(emptyList())
    val alertHistory: StateFlow<List<Alert>> = _alertHistory.asStateFlow()
    
    private val _alertStatistics = MutableStateFlow(AlertStatistics())
    val alertStatistics: StateFlow<AlertStatistics> = _alertStatistics.asStateFlow()
    
    // Alert suppression to prevent spam
    private val suppressedAlerts = mutableMapOf<String, Instant>()
    
    // Escalation tracking
    private val escalationTracking = mutableMapOf<String, EscalationState>()
    
    init {
        logger.info(TAG, "AlertingSystem initialized with intelligent escalation")
        startEscalationMonitoring()
    }

    /**
     * Raise an alert with automatic escalation handling
     */
    fun raiseAlert(
        source: String,
        type: AlertType,
        severity: AlertSeverity,
        title: String,
        description: String,
        metadata: Map<String, String> = emptyMap(),
        requiresAcknowledgment: Boolean = severity >= AlertSeverity.HIGH
    ) {
        scope.launch {
            try {
                // Check if alert should be suppressed
                val suppressionKey = generateSuppressionKey(source, type, title)
                if (isAlertSuppressed(suppressionKey)) {
                    logger.debug(TAG, "Alert suppressed: $title")
                    return@launch
                }
                
                val alert = Alert(
                    id = generateAlertId(),
                    source = source,
                    type = type,
                    severity = severity,
                    title = title,
                    description = description,
                    metadata = metadata,
                    timestamp = Clock.System.now(),
                    status = AlertStatus.ACTIVE,
                    requiresAcknowledgment = requiresAcknowledgment,
                    escalationLevel = 0
                )
                
                processAlert(alert)
                
                // Add to suppression if needed
                if (shouldSuppressAlert(type, severity)) {
                    suppressedAlerts[suppressionKey] = Clock.System.now()
                }
                
            } catch (e: Exception) {
                logger.error(TAG, "Error raising alert: $title", e)
            }
        }
    }
    
    /**
     * Process and route an alert through the system
     */
    private fun processAlert(alert: Alert) {
        // Add to active alerts
        addToActiveAlerts(alert)
        
        // Add to history
        addToHistory(alert)
        
        // Log the alert
        logAlert(alert)
        
        // Start escalation if needed
        if (alert.requiresAcknowledgment) {
            startEscalation(alert)
        }
        
        // Update statistics
        updateStatistics()
        
        // Send notifications based on severity
        sendNotifications(alert)
    }
    
    /**
     * Add alert to active alerts list
     */
    private fun addToActiveAlerts(alert: Alert) {
        val currentAlerts = _activeAlerts.value.toMutableList()
        currentAlerts.add(alert)
        _activeAlerts.value = currentAlerts
    }
    
    /**
     * Add alert to history
     */
    private fun addToHistory(alert: Alert) {
        val currentHistory = _alertHistory.value.toMutableList()
        currentHistory.add(alert)
        
        // Keep only recent history
        if (currentHistory.size > MAX_ALERTS_HISTORY) {
            currentHistory.removeAt(0)
        }
        
        _alertHistory.value = currentHistory
    }
    
    /**
     * Log alert with appropriate level
     */
    private fun logAlert(alert: Alert) {
        val logMessage = "${alert.title}: ${alert.description}"
        
        when (alert.severity) {
            AlertSeverity.CRITICAL -> {
                logger.error(TAG, "CRITICAL ALERT: $logMessage")
                logger.logSecurityEvent(TAG, "critical_alert", "Source: ${alert.source}, Message: $logMessage")
            }
            AlertSeverity.HIGH -> {
                logger.error(TAG, "HIGH ALERT: $logMessage")
            }
            AlertSeverity.MEDIUM -> {
                logger.warn(TAG, "MEDIUM ALERT: $logMessage")
            }
            AlertSeverity.LOW -> {
                logger.info(TAG, "LOW ALERT: $logMessage")
            }
            AlertSeverity.INFO -> {
                logger.info(TAG, "INFO ALERT: $logMessage")
            }
        }
    }
    
    /**
     * Start escalation process for an alert
     */
    private fun startEscalation(alert: Alert) {
        val escalationState = EscalationState(
            alertId = alert.id,
            startTime = Clock.System.now(),
            currentLevel = 0,
            maxLevel = getMaxEscalationLevel(alert.severity),
            isActive = true
        )
        
        escalationTracking[alert.id] = escalationState
        
        logger.info(TAG, "Started escalation for alert: ${alert.title}")
        
        // For critical alerts, escalate immediately
        if (alert.severity == AlertSeverity.CRITICAL && CRITICAL_ALERT_IMMEDIATE_ESCALATION) {
            escalateAlert(alert.id)
        }
    }
    
    /**
     * Escalate an alert to the next level
     */
    private fun escalateAlert(alertId: String) {
        val escalationState = escalationTracking[alertId] ?: return
        val alert = findAlertById(alertId) ?: return
        
        if (!escalationState.isActive || escalationState.currentLevel >= escalationState.maxLevel) {
            return
        }
        
        val newLevel = escalationState.currentLevel + 1
        escalationTracking[alertId] = escalationState.copy(
            currentLevel = newLevel,
            lastEscalationTime = Clock.System.now()
        )
        
        // Update alert escalation level
        updateAlertEscalationLevel(alertId, newLevel)
        
        logger.warn(TAG, "Alert escalated to level $newLevel: ${alert.title}")
        
        // Send escalated notifications
        sendEscalatedNotifications(alert, newLevel)
        
        // Log escalation
        logger.logSecurityEvent(TAG, "alert_escalated", "Source: ${alert.source}, Alert '${alert.title}' escalated to level $newLevel")
    }
    
    /**
     * Acknowledge an alert to stop escalation
     */
    fun acknowledgeAlert(alertId: String, acknowledgedBy: String, notes: String = "") {
        scope.launch {
            try {
                val alert = findAlertById(alertId)
                if (alert != null) {
                    // Update alert status
                    updateAlertStatus(alertId, AlertStatus.ACKNOWLEDGED)
                    
                    // Stop escalation
                    escalationTracking[alertId]?.let { escalation ->
                        escalationTracking[alertId] = escalation.copy(
                            isActive = false,
                            acknowledgedBy = acknowledgedBy,
                            acknowledgedAt = Clock.System.now(),
                            acknowledgmentNotes = notes
                        )
                    }
                    
                    logger.info(TAG, "Alert acknowledged by $acknowledgedBy: ${alert.title}")
                    logger.logSecurityEvent(TAG, "alert_acknowledged", "Source: ${alert.source}, Alert '${alert.title}' acknowledged by $acknowledgedBy")
                    
                    updateStatistics()
                }
            } catch (e: Exception) {
                logger.error(TAG, "Error acknowledging alert: $alertId", e)
            }
        }
    }
    
    /**
     * Resolve an alert
     */
    fun resolveAlert(alertId: String, resolvedBy: String, resolution: String = "") {
        scope.launch {
            try {
                val alert = findAlertById(alertId)
                if (alert != null) {
                    // Update alert status
                    updateAlertStatus(alertId, AlertStatus.RESOLVED)
                    
                    // Remove from active alerts
                    removeFromActiveAlerts(alertId)
                    
                    // Stop escalation
                    escalationTracking[alertId]?.let { escalation ->
                        escalationTracking[alertId] = escalation.copy(
                            isActive = false,
                            resolvedBy = resolvedBy,
                            resolvedAt = Clock.System.now(),
                            resolution = resolution
                        )
                    }
                    
                    logger.info(TAG, "Alert resolved by $resolvedBy: ${alert.title}")
                    logger.logSecurityEvent(TAG, "alert_resolved", "Source: ${alert.source}, Alert '${alert.title}' resolved by $resolvedBy")
                    
                    updateStatistics()
                }
            } catch (e: Exception) {
                logger.error(TAG, "Error resolving alert: $alertId", e)
            }
        }
    }
    
    /**
     * Start monitoring for escalation timeouts
     */
    private fun startEscalationMonitoring() {
        scope.launch {
            while (true) {
                try {
                    checkEscalationTimeouts()
                    delay(30.seconds) // Check every 30 seconds
                } catch (e: Exception) {
                    logger.error(TAG, "Error in escalation monitoring", e)
                    delay(30.seconds)
                }
            }
        }
    }
    
    /**
     * Check for alerts that need escalation
     */
    private fun checkEscalationTimeouts() {
        val now = Clock.System.now()
        
        escalationTracking.values.forEach { escalation ->
            if (escalation.isActive) {
                val timeSinceStart = now - escalation.startTime
                val timeSinceLastEscalation = escalation.lastEscalationTime?.let { now - it } 
                    ?: timeSinceStart
                
                val shouldEscalate = when (escalation.currentLevel) {
                    0 -> timeSinceStart >= ESCALATION_DELAY_MINUTES.minutes
                    else -> timeSinceLastEscalation >= ESCALATION_DELAY_MINUTES.minutes
                }
                
                if (shouldEscalate && escalation.currentLevel < escalation.maxLevel) {
                    escalateAlert(escalation.alertId)
                }
            }
        }
    }
    
    /**
     * Send notifications based on alert severity
     */
    private fun sendNotifications(alert: Alert) {
        when (alert.severity) {
            AlertSeverity.CRITICAL -> {
                // Send to all notification channels immediately
                logger.error(TAG, "CRITICAL NOTIFICATION: ${alert.title}")
                // In a real implementation, this would send SMS, email, push notifications, etc.
            }
            AlertSeverity.HIGH -> {
                // Send to primary notification channels
                logger.warn(TAG, "HIGH PRIORITY NOTIFICATION: ${alert.title}")
            }
            AlertSeverity.MEDIUM -> {
                // Send to standard notification channels
                logger.info(TAG, "MEDIUM PRIORITY NOTIFICATION: ${alert.title}")
            }
            AlertSeverity.LOW, AlertSeverity.INFO -> {
                // Log only or send to low-priority channels
                logger.debug(TAG, "LOW PRIORITY NOTIFICATION: ${alert.title}")
            }
        }
    }
    
    /**
     * Send escalated notifications
     */
    private fun sendEscalatedNotifications(alert: Alert, escalationLevel: Int) {
        logger.error(TAG, "ESCALATED NOTIFICATION (Level $escalationLevel): ${alert.title}")
        // In a real implementation, this would notify higher-level personnel
    }
    
    /**
     * Check if alert should be suppressed
     */
    private fun isAlertSuppressed(suppressionKey: String): Boolean {
        val suppressedAt = suppressedAlerts[suppressionKey] ?: return false
        val now = Clock.System.now()
        
        return (now - suppressedAt) < ALERT_SUPPRESSION_MINUTES.minutes
    }
    
    /**
     * Determine if alert type should be suppressed
     */
    private fun shouldSuppressAlert(type: AlertType, severity: AlertSeverity): Boolean {
        return when (type) {
            AlertType.PERFORMANCE_DEGRADATION, 
            AlertType.MEMORY_USAGE,
            AlertType.CONNECTION_ISSUE -> severity < AlertSeverity.HIGH
            else -> false
        }
    }
    
    /**
     * Update alert statistics
     */
    private fun updateStatistics() {
        val activeAlerts = _activeAlerts.value
        val history = _alertHistory.value
        val now = Clock.System.now()
        
        val recentAlerts = history.filter { it.timestamp > now.minus(1.minutes) }
        val acknowledgedAlerts = history.count { it.status == AlertStatus.ACKNOWLEDGED }
        val resolvedAlerts = history.count { it.status == AlertStatus.RESOLVED }
        
        val stats = AlertStatistics(
            totalActiveAlerts = activeAlerts.size,
            criticalAlerts = activeAlerts.count { it.severity == AlertSeverity.CRITICAL },
            highSeverityAlerts = activeAlerts.count { it.severity == AlertSeverity.HIGH },
            totalAlertsToday = history.count { 
                (now - it.timestamp).inWholeDays == 0L 
            },
            recentAlerts = recentAlerts.size,
            acknowledgedAlerts = acknowledgedAlerts,
            resolvedAlerts = resolvedAlerts,
            averageResolutionTimeMinutes = calculateAverageResolutionTime(),
            escalatedAlerts = escalationTracking.values.count { it.currentLevel > 0 }
        )
        
        _alertStatistics.value = stats
    }
    
    /**
     * Calculate average resolution time for resolved alerts
     */
    private fun calculateAverageResolutionTime(): Long {
        val resolvedAlerts = _alertHistory.value.filter { it.status == AlertStatus.RESOLVED }
        if (resolvedAlerts.isEmpty()) return 0
        
        val totalResolutionTime = resolvedAlerts.mapNotNull { alert ->
            escalationTracking[alert.id]?.resolvedAt?.let { resolvedAt ->
                (resolvedAt - alert.timestamp).inWholeMinutes
            }
        }.sum()
        
        return if (resolvedAlerts.isNotEmpty()) totalResolutionTime / resolvedAlerts.size else 0
    }
    
    // Helper methods
    private fun findAlertById(alertId: String): Alert? {
        return _activeAlerts.value.find { it.id == alertId } 
            ?: _alertHistory.value.find { it.id == alertId }
    }
    
    private fun updateAlertStatus(alertId: String, status: AlertStatus) {
        // Update in active alerts
        val activeAlerts = _activeAlerts.value.toMutableList()
        val activeIndex = activeAlerts.indexOfFirst { it.id == alertId }
        if (activeIndex >= 0) {
            activeAlerts[activeIndex] = activeAlerts[activeIndex].copy(status = status)
            _activeAlerts.value = activeAlerts
        }
        
        // Update in history
        val history = _alertHistory.value.toMutableList()
        val historyIndex = history.indexOfFirst { it.id == alertId }
        if (historyIndex >= 0) {
            history[historyIndex] = history[historyIndex].copy(status = status)
            _alertHistory.value = history
        }
    }
    
    private fun updateAlertEscalationLevel(alertId: String, level: Int) {
        // Update in active alerts
        val activeAlerts = _activeAlerts.value.toMutableList()
        val activeIndex = activeAlerts.indexOfFirst { it.id == alertId }
        if (activeIndex >= 0) {
            activeAlerts[activeIndex] = activeAlerts[activeIndex].copy(escalationLevel = level)
            _activeAlerts.value = activeAlerts
        }
    }
    
    private fun removeFromActiveAlerts(alertId: String) {
        val activeAlerts = _activeAlerts.value.toMutableList()
        activeAlerts.removeAll { it.id == alertId }
        _activeAlerts.value = activeAlerts
    }
    
    private fun getMaxEscalationLevel(severity: AlertSeverity): Int {
        return when (severity) {
            AlertSeverity.CRITICAL -> 3
            AlertSeverity.HIGH -> 2
            AlertSeverity.MEDIUM -> 1
            else -> 0
        }
    }
    
    private fun generateAlertId(): String = "ALERT_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    
    private fun generateSuppressionKey(source: String, type: AlertType, title: String): String {
        return "${source}_${type}_${title.hashCode()}"
    }
    
    /**
     * Clean up old data
     */
    fun cleanupOldData(olderThan: Instant) {
        // Clean up suppressed alerts
        suppressedAlerts.entries.removeAll { (_, timestamp) -> timestamp < olderThan }
        
        // Clean up escalation tracking for resolved alerts
        escalationTracking.entries.removeAll { (_, escalation) -> 
            !escalation.isActive && (escalation.resolvedAt ?: escalation.acknowledgedAt)?.let { it < olderThan } == true
        }
        
        logger.info(TAG, "Cleaned up old alert data")
    }
}

/**
 * Alert data class
 */
data class Alert(
    val id: String,
    val source: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val description: String,
    val metadata: Map<String, String>,
    val timestamp: Instant,
    val status: AlertStatus = AlertStatus.ACTIVE,
    val requiresAcknowledgment: Boolean = false,
    val escalationLevel: Int = 0
)

/**
 * Alert types
 */
enum class AlertType {
    SECURITY_THREAT,
    PERFORMANCE_DEGRADATION,
    MEMORY_USAGE,
    CONNECTION_ISSUE,
    DATA_QUALITY,
    SYSTEM_ERROR,
    TRAIN_ANOMALY,
    NETWORK_ISSUE
}

/**
 * Alert severity levels
 */
enum class AlertSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Alert status
 */
enum class AlertStatus {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED,
    SUPPRESSED
}

/**
 * Escalation state tracking
 */
data class EscalationState(
    val alertId: String,
    val startTime: Instant,
    val currentLevel: Int,
    val maxLevel: Int,
    val isActive: Boolean,
    val lastEscalationTime: Instant? = null,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Instant? = null,
    val acknowledgmentNotes: String? = null,
    val resolvedBy: String? = null,
    val resolvedAt: Instant? = null,
    val resolution: String? = null
)

/**
 * Alert statistics for dashboard
 */
data class AlertStatistics(
    val totalActiveAlerts: Int = 0,
    val criticalAlerts: Int = 0,
    val highSeverityAlerts: Int = 0,
    val totalAlertsToday: Int = 0,
    val recentAlerts: Int = 0,
    val acknowledgedAlerts: Int = 0,
    val resolvedAlerts: Int = 0,
    val averageResolutionTimeMinutes: Long = 0,
    val escalatedAlerts: Int = 0
)