package com.example.pravaahan.core.logging

/**
 * Extension functions to make logging easier throughout the app
 */

/**
 * Extension function for any class to get a logger with automatic tag
 */
inline fun <reified T> T.logger(): Logger = AppLogger()

/**
 * Extension function to get class name as tag
 */
inline fun <reified T> T.logTag(): String = T::class.java.simpleName

/**
 * Convenience functions for common logging patterns
 */
fun Logger.logMethodEntry(tag: String, methodName: String, vararg params: Any?) {
    debug(tag, "→ $methodName(${params.joinToString(", ")})")
}

fun Logger.logMethodExit(tag: String, methodName: String, result: Any? = null) {
    debug(tag, "← $methodName${result?.let { " = $it" } ?: ""}")
}

fun Logger.logApiCall(tag: String, endpoint: String, params: Map<String, Any?> = emptyMap()) {
    info(tag, "API Call: $endpoint ${if (params.isNotEmpty()) "with $params" else ""}")
}

fun Logger.logApiResponse(tag: String, endpoint: String, success: Boolean, responseTime: Long? = null) {
    val status = if (success) "SUCCESS" else "FAILED"
    val timing = responseTime?.let { " (${it}ms)" } ?: ""
    info(tag, "API Response: $endpoint - $status$timing")
}

fun Logger.logTrainOperation(tag: String, operation: String, trainId: String, details: String = "") {
    info(tag, "Train Operation: $operation for train $trainId${if (details.isNotEmpty()) " - $details" else ""}")
}

fun Logger.logConflictDetected(tag: String, conflictId: String, trainsInvolved: List<String>, severity: String) {
    warn(tag, "Conflict Detected: $conflictId involving trains ${trainsInvolved.joinToString(", ")} - Severity: $severity")
}

fun Logger.logCriticalTrainEvent(tag: String, event: String, trainId: String, details: String) {
    critical(tag, "Critical Train Event: $event for train $trainId - $details")
}

fun Logger.logPerformanceMetric(tag: String, metricName: String, value: Float, unit: String = "") {
    debug(tag, "Performance Metric: $metricName = $value${if (unit.isNotEmpty()) " $unit" else ""}")
}

fun Logger.logSecurityEvent(tag: String, event: String, details: String = "") {
    warn(tag, "Security Event: $event${if (details.isNotEmpty()) " - $details" else ""}")
}