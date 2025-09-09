package com.example.pravaahan.core.accessibility

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.runtime.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.pravaahan.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages battery optimization for continuous real-time railway operations
 */
@Singleton
class BatteryOptimizationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) : DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "BatteryOptimizationManager"
        
        // Battery level thresholds
        private const val LOW_BATTERY_THRESHOLD = 20
        private const val CRITICAL_BATTERY_THRESHOLD = 10
        
        // Performance adjustment factors
        private const val NORMAL_UPDATE_INTERVAL_MS = 1000L
        private const val POWER_SAVE_UPDATE_INTERVAL_MS = 2000L
        private const val CRITICAL_UPDATE_INTERVAL_MS = 5000L
        
        private const val NORMAL_ANIMATION_DURATION_MS = 300
        private const val POWER_SAVE_ANIMATION_DURATION_MS = 150
        private const val CRITICAL_ANIMATION_DURATION_MS = 0
    }
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()
    
    private val _optimizationMode = MutableStateFlow(OptimizationMode.NORMAL)
    val optimizationMode: StateFlow<OptimizationMode> = _optimizationMode.asStateFlow()
    
    private var batteryMonitoringJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        startBatteryMonitoring()
    }
    
    /**
     * Start monitoring battery status
     */
    private fun startBatteryMonitoring() {
        batteryMonitoringJob = coroutineScope.launch {
            while (isActive) {
                updateBatteryState()
                delay(10000) // Check every 10 seconds
            }
        }
        
        logger.info(TAG, "Battery monitoring started")
    }
    
    /**
     * Update current battery state
     */
    private fun updateBatteryState() {
        try {
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
            val isPowerSaveMode = powerManager.isPowerSaveMode
            
            val newState = BatteryState(
                level = batteryLevel,
                isCharging = isCharging,
                isPowerSaveMode = isPowerSaveMode,
                temperature = 25.0f // Default temperature since BATTERY_PROPERTY_TEMPERATURE is not available
            )
            
            _batteryState.value = newState
            
            // Update optimization mode based on battery state
            updateOptimizationMode(newState)
            
            logger.debug(TAG, "Battery state updated: level=$batteryLevel%, charging=$isCharging, powerSave=$isPowerSaveMode")
            
        } catch (e: Exception) {
            logger.error(TAG, "Failed to update battery state", e)
        }
    }
    
    /**
     * Update optimization mode based on battery state
     */
    private fun updateOptimizationMode(batteryState: BatteryState) {
        val newMode = when {
            batteryState.level <= CRITICAL_BATTERY_THRESHOLD && !batteryState.isCharging -> {
                OptimizationMode.CRITICAL_POWER_SAVE
            }
            batteryState.level <= LOW_BATTERY_THRESHOLD && !batteryState.isCharging -> {
                OptimizationMode.POWER_SAVE
            }
            batteryState.isPowerSaveMode -> {
                OptimizationMode.POWER_SAVE
            }
            else -> {
                OptimizationMode.NORMAL
            }
        }
        
        if (newMode != _optimizationMode.value) {
            _optimizationMode.value = newMode
            logger.info(TAG, "Optimization mode changed to: $newMode")
        }
    }
    
    /**
     * Get optimized update interval based on current mode
     */
    fun getOptimizedUpdateInterval(): Long {
        return when (_optimizationMode.value) {
            OptimizationMode.NORMAL -> NORMAL_UPDATE_INTERVAL_MS
            OptimizationMode.POWER_SAVE -> POWER_SAVE_UPDATE_INTERVAL_MS
            OptimizationMode.CRITICAL_POWER_SAVE -> CRITICAL_UPDATE_INTERVAL_MS
        }
    }
    
    /**
     * Get optimized animation duration based on current mode
     */
    fun getOptimizedAnimationDuration(): Int {
        return when (_optimizationMode.value) {
            OptimizationMode.NORMAL -> NORMAL_ANIMATION_DURATION_MS
            OptimizationMode.POWER_SAVE -> POWER_SAVE_ANIMATION_DURATION_MS
            OptimizationMode.CRITICAL_POWER_SAVE -> CRITICAL_ANIMATION_DURATION_MS
        }
    }
    
    /**
     * Check if feature should be disabled for power saving
     */
    fun shouldDisableFeature(feature: PowerIntensiveFeature): Boolean {
        return when (_optimizationMode.value) {
            OptimizationMode.NORMAL -> false
            OptimizationMode.POWER_SAVE -> when (feature) {
                PowerIntensiveFeature.BACKGROUND_ANIMATIONS -> true
                PowerIntensiveFeature.HAPTIC_FEEDBACK -> false
                PowerIntensiveFeature.REAL_TIME_UPDATES -> false
                PowerIntensiveFeature.MAP_ANIMATIONS -> true
                PowerIntensiveFeature.VISUAL_EFFECTS -> true
            }
            OptimizationMode.CRITICAL_POWER_SAVE -> when (feature) {
                PowerIntensiveFeature.BACKGROUND_ANIMATIONS -> true
                PowerIntensiveFeature.HAPTIC_FEEDBACK -> true
                PowerIntensiveFeature.REAL_TIME_UPDATES -> false // Keep critical functionality
                PowerIntensiveFeature.MAP_ANIMATIONS -> true
                PowerIntensiveFeature.VISUAL_EFFECTS -> true
            }
        }
    }
    
    /**
     * Get optimized frame rate for animations
     */
    fun getOptimizedFrameRate(): Int {
        return when (_optimizationMode.value) {
            OptimizationMode.NORMAL -> 60
            OptimizationMode.POWER_SAVE -> 30
            OptimizationMode.CRITICAL_POWER_SAVE -> 15
        }
    }
    
    /**
     * Check if background processing should be reduced
     */
    fun shouldReduceBackgroundProcessing(): Boolean {
        return _optimizationMode.value != OptimizationMode.NORMAL
    }
    
    /**
     * Get CPU usage optimization factor
     */
    fun getCpuOptimizationFactor(): Float {
        return when (_optimizationMode.value) {
            OptimizationMode.NORMAL -> 1.0f
            OptimizationMode.POWER_SAVE -> 0.7f
            OptimizationMode.CRITICAL_POWER_SAVE -> 0.5f
        }
    }
    
    /**
     * Check if device is overheating
     */
    fun isDeviceOverheating(): Boolean {
        val temperature = _batteryState.value.temperature
        return temperature > 40.0f // Celsius
    }
    
    /**
     * Get thermal throttling recommendations
     */
    fun getThermalThrottlingRecommendations(): List<ThermalRecommendation> {
        val temperature = _batteryState.value.temperature
        
        return when {
            temperature > 45.0f -> listOf(
                ThermalRecommendation.DISABLE_ANIMATIONS,
                ThermalRecommendation.REDUCE_UPDATE_FREQUENCY,
                ThermalRecommendation.LOWER_SCREEN_BRIGHTNESS,
                ThermalRecommendation.DISABLE_HAPTICS
            )
            temperature > 40.0f -> listOf(
                ThermalRecommendation.REDUCE_ANIMATIONS,
                ThermalRecommendation.REDUCE_UPDATE_FREQUENCY
            )
            else -> emptyList()
        }
    }
    
    /**
     * Apply power optimization settings
     */
    fun applyPowerOptimizations() {
        val mode = _optimizationMode.value
        logger.info(TAG, "Applying power optimizations for mode: $mode")
        
        when (mode) {
            OptimizationMode.POWER_SAVE -> {
                logger.info(TAG, "Power save mode activated - reducing update frequency and animations")
            }
            OptimizationMode.CRITICAL_POWER_SAVE -> {
                logger.warn(TAG, "Critical power save mode activated - disabling non-essential features")
            }
            OptimizationMode.NORMAL -> {
                logger.info(TAG, "Normal power mode - all features enabled")
            }
        }
    }
    
    /**
     * Get battery optimization recommendations
     */
    fun getBatteryOptimizationRecommendations(): List<BatteryRecommendation> {
        val state = _batteryState.value
        val recommendations = mutableListOf<BatteryRecommendation>()
        
        if (state.level <= CRITICAL_BATTERY_THRESHOLD && !state.isCharging) {
            recommendations.add(BatteryRecommendation.ENABLE_CRITICAL_POWER_SAVE)
            recommendations.add(BatteryRecommendation.REDUCE_SCREEN_BRIGHTNESS)
            recommendations.add(BatteryRecommendation.DISABLE_NON_ESSENTIAL_FEATURES)
        } else if (state.level <= LOW_BATTERY_THRESHOLD && !state.isCharging) {
            recommendations.add(BatteryRecommendation.ENABLE_POWER_SAVE)
            recommendations.add(BatteryRecommendation.REDUCE_UPDATE_FREQUENCY)
        }
        
        if (isDeviceOverheating()) {
            recommendations.add(BatteryRecommendation.REDUCE_CPU_USAGE)
            recommendations.add(BatteryRecommendation.DISABLE_ANIMATIONS)
        }
        
        return recommendations
    }
    
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (batteryMonitoringJob?.isActive != true) {
            startBatteryMonitoring()
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        batteryMonitoringJob?.cancel()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        batteryMonitoringJob?.cancel()
        coroutineScope.cancel()
    }
}

/**
 * Battery state information
 */
data class BatteryState(
    val level: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val temperature: Float = 25.0f
)

/**
 * Power optimization modes
 */
enum class OptimizationMode {
    NORMAL,
    POWER_SAVE,
    CRITICAL_POWER_SAVE
}

/**
 * Power intensive features that can be disabled
 */
enum class PowerIntensiveFeature {
    BACKGROUND_ANIMATIONS,
    HAPTIC_FEEDBACK,
    REAL_TIME_UPDATES,
    MAP_ANIMATIONS,
    VISUAL_EFFECTS
}

/**
 * Thermal throttling recommendations
 */
enum class ThermalRecommendation {
    DISABLE_ANIMATIONS,
    REDUCE_ANIMATIONS,
    REDUCE_UPDATE_FREQUENCY,
    LOWER_SCREEN_BRIGHTNESS,
    DISABLE_HAPTICS
}

/**
 * Battery optimization recommendations
 */
enum class BatteryRecommendation {
    ENABLE_POWER_SAVE,
    ENABLE_CRITICAL_POWER_SAVE,
    REDUCE_SCREEN_BRIGHTNESS,
    DISABLE_NON_ESSENTIAL_FEATURES,
    REDUCE_UPDATE_FREQUENCY,
    REDUCE_CPU_USAGE,
    DISABLE_ANIMATIONS
}

/**
 * Composable to remember battery optimization state
 */
@Composable
fun rememberBatteryOptimizationState(
    batteryManager: BatteryOptimizationManager
): State<OptimizationMode> {
    return batteryManager.optimizationMode.collectAsState()
}