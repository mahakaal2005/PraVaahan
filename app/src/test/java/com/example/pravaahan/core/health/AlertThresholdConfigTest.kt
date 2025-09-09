package com.example.pravaahan.core.health

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AlertThresholdConfigTest {
    
    private lateinit var alertThresholdConfig: AlertThresholdConfig
    
    @Before
    fun setup() {
        alertThresholdConfig = AlertThresholdConfig()
    }
    
    @Test
    fun `should start with normal operational mode`() {
        assertEquals(AlertThresholdConfig.OperationalMode.NORMAL, alertThresholdConfig.currentMode.value)
    }
    
    @Test
    fun `should return correct alert level for latency`() {
        // Normal latency
        assertEquals(PerformanceAlertLevel.NORMAL, alertThresholdConfig.getLatencyAlertLevel(500L))
        
        // Warning latency
        assertEquals(PerformanceAlertLevel.WARNING, alertThresholdConfig.getLatencyAlertLevel(1500L))
        
        // Critical latency
        assertEquals(PerformanceAlertLevel.CRITICAL, alertThresholdConfig.getLatencyAlertLevel(3500L))
        
        // Emergency latency
        assertEquals(PerformanceAlertLevel.EMERGENCY, alertThresholdConfig.getLatencyAlertLevel(6000L))
    }
    
    @Test
    fun `should return correct alert level for error rate`() {
        // Normal error rate
        assertEquals(PerformanceAlertLevel.NORMAL, alertThresholdConfig.getErrorRateAlertLevel(0.01))
        
        // Warning error rate
        assertEquals(PerformanceAlertLevel.WARNING, alertThresholdConfig.getErrorRateAlertLevel(0.03))
        
        // Critical error rate
        assertEquals(PerformanceAlertLevel.CRITICAL, alertThresholdConfig.getErrorRateAlertLevel(0.07))
        
        // Emergency error rate
        assertEquals(PerformanceAlertLevel.EMERGENCY, alertThresholdConfig.getErrorRateAlertLevel(0.12))
    }
    
    @Test
    fun `should return correct alert level for memory usage`() {
        // Normal memory usage
        assertEquals(PerformanceAlertLevel.NORMAL, alertThresholdConfig.getMemoryAlertLevel(50L))
        
        // Warning memory usage
        assertEquals(PerformanceAlertLevel.WARNING, alertThresholdConfig.getMemoryAlertLevel(150L))
        
        // Critical memory usage
        assertEquals(PerformanceAlertLevel.CRITICAL, alertThresholdConfig.getMemoryAlertLevel(250L))
        
        // Emergency memory usage
        assertEquals(PerformanceAlertLevel.EMERGENCY, alertThresholdConfig.getMemoryAlertLevel(350L))
    }
    
    @Test
    fun `should return correct alert level for data quality`() {
        // Excellent data quality
        assertEquals(PerformanceAlertLevel.NORMAL, alertThresholdConfig.getDataQualityAlertLevel(0.98))
        
        // Warning data quality
        assertEquals(PerformanceAlertLevel.WARNING, alertThresholdConfig.getDataQualityAlertLevel(0.88))
        
        // Critical data quality
        assertEquals(PerformanceAlertLevel.CRITICAL, alertThresholdConfig.getDataQualityAlertLevel(0.83))
        
        // Emergency data quality
        assertEquals(PerformanceAlertLevel.EMERGENCY, alertThresholdConfig.getDataQualityAlertLevel(0.75))
    }
    
    @Test
    fun `should change operational mode and thresholds`() {
        // Change to high traffic mode
        alertThresholdConfig.setOperationalMode(AlertThresholdConfig.OperationalMode.HIGH_TRAFFIC)
        assertEquals(AlertThresholdConfig.OperationalMode.HIGH_TRAFFIC, alertThresholdConfig.currentMode.value)
        
        // High traffic mode should have higher latency thresholds
        val highTrafficThresholds = alertThresholdConfig.getCurrentThresholds()
        assertEquals(1500L, highTrafficThresholds.warningLatencyMs)
        assertEquals(4000L, highTrafficThresholds.criticalLatencyMs)
        assertEquals(6000L, highTrafficThresholds.emergencyLatencyMs)
    }
    
    @Test
    fun `should allow custom threshold updates`() {
        val customThresholds = AlertThresholdConfig.ThresholdSet(
            warningLatencyMs = 2000L,
            criticalLatencyMs = 5000L,
            emergencyLatencyMs = 8000L
        )
        
        alertThresholdConfig.updateThresholds(AlertThresholdConfig.OperationalMode.NORMAL, customThresholds)
        
        val updatedThresholds = alertThresholdConfig.getCurrentThresholds()
        assertEquals(2000L, updatedThresholds.warningLatencyMs)
        assertEquals(5000L, updatedThresholds.criticalLatencyMs)
        assertEquals(8000L, updatedThresholds.emergencyLatencyMs)
    }
    
    @Test
    fun `should reset to defaults`() {
        // Update with custom thresholds
        val customThresholds = AlertThresholdConfig.ThresholdSet(warningLatencyMs = 2000L)
        alertThresholdConfig.updateThresholds(AlertThresholdConfig.OperationalMode.NORMAL, customThresholds)
        
        // Reset to defaults
        alertThresholdConfig.resetToDefaults(AlertThresholdConfig.OperationalMode.NORMAL)
        
        // Should be back to default values
        val thresholds = alertThresholdConfig.getCurrentThresholds()
        assertEquals(1000L, thresholds.warningLatencyMs)
    }
    
    @Test
    fun `should provide recommended actions for different alert levels`() {
        val emergencyAction = alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.EMERGENCY, "latency")
        assertEquals("Immediate intervention required - check network connectivity and server load", emergencyAction)
        
        val criticalAction = alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.CRITICAL, "error_rate")
        assertEquals("Investigate error patterns and implement additional error handling", criticalAction)
        
        val warningAction = alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.WARNING, "memory_usage")
        assertEquals("Monitor memory usage patterns and optimize if needed", warningAction)
        
        val normalAction = alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.NORMAL, "latency")
        assertEquals(null, normalAction)
    }
    
    @Test
    fun `emergency mode should have stricter thresholds`() {
        alertThresholdConfig.setOperationalMode(AlertThresholdConfig.OperationalMode.EMERGENCY)
        
        val emergencyThresholds = alertThresholdConfig.getCurrentThresholds()
        
        // Emergency mode should have much stricter thresholds
        assertEquals(500L, emergencyThresholds.warningLatencyMs)
        assertEquals(1500L, emergencyThresholds.criticalLatencyMs)
        assertEquals(3000L, emergencyThresholds.emergencyLatencyMs)
        
        assertEquals(0.01, emergencyThresholds.warningErrorRate, 0.001)
        assertEquals(0.03, emergencyThresholds.criticalErrorRate, 0.001)
        assertEquals(0.05, emergencyThresholds.emergencyErrorRate, 0.001)
    }
}