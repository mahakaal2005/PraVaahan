package com.example.pravaahan.core.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.pravaahan.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages haptic feedback for critical train events and user interactions
 */
@Singleton
class HapticFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "HapticFeedbackManager"
        
        // Vibration patterns (in milliseconds)
        private val LIGHT_PATTERN = longArrayOf(0, 50)
        private val MEDIUM_PATTERN = longArrayOf(0, 100)
        private val HEAVY_PATTERN = longArrayOf(0, 200)
        private val SELECTION_PATTERN = longArrayOf(0, 25, 50, 25)
        private val CONFLICT_ALERT_PATTERN = longArrayOf(0, 100, 100, 100, 100, 100)
        private val EMERGENCY_PATTERN = longArrayOf(0, 200, 100, 200, 100, 200)
        
        // Vibration amplitudes (0-255)
        private const val LIGHT_AMPLITUDE = 50
        private const val MEDIUM_AMPLITUDE = 128
        private const val HEAVY_AMPLITUDE = 255
    }
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    private var isHapticEnabled = true
    private var hapticIntensity = HapticIntensity.MEDIUM
    
    /**
     * Light impact for general UI interactions
     */
    fun performLightImpact() {
        if (!isHapticEnabled) return
        
        logger.debug(TAG, "Performing light haptic feedback")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(50, LIGHT_AMPLITUDE)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }
    
    /**
     * Medium impact for important interactions
     */
    fun performMediumImpact() {
        if (!isHapticEnabled) return
        
        logger.debug(TAG, "Performing medium haptic feedback")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(100, MEDIUM_AMPLITUDE)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    }
    
    /**
     * Heavy impact for critical interactions
     */
    fun performHeavyImpact() {
        if (!isHapticEnabled) return
        
        logger.debug(TAG, "Performing heavy haptic feedback")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(200, HEAVY_AMPLITUDE)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(200)
        }
    }
    
    /**
     * Selection feedback for train/element selection
     */
    fun performSelectionFeedback() {
        if (!isHapticEnabled) return
        
        logger.debug(TAG, "Performing selection haptic feedback")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = intArrayOf(0, LIGHT_AMPLITUDE, 0, LIGHT_AMPLITUDE)
            val effect = VibrationEffect.createWaveform(SELECTION_PATTERN, amplitudes, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(SELECTION_PATTERN, -1)
        }
    }
    
    /**
     * Conflict alert feedback for train conflicts
     */
    fun performConflictAlert() {
        if (!isHapticEnabled) return
        
        logger.info(TAG, "Performing conflict alert haptic feedback")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = intArrayOf(0, HEAVY_AMPLITUDE, 0, HEAVY_AMPLITUDE, 0, HEAVY_AMPLITUDE)
            val effect = VibrationEffect.createWaveform(CONFLICT_ALERT_PATTERN, amplitudes, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(CONFLICT_ALERT_PATTERN, -1)
        }
    }
    
    /**
     * Emergency alert feedback for critical situations
     */
    fun performEmergencyAlert() {
        if (!isHapticEnabled) return
        
        logger.warn(TAG, "Performing emergency alert haptic feedback")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = intArrayOf(0, HEAVY_AMPLITUDE, 0, HEAVY_AMPLITUDE, 0, HEAVY_AMPLITUDE)
            val effect = VibrationEffect.createWaveform(EMERGENCY_PATTERN, amplitudes, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(EMERGENCY_PATTERN, -1)
        }
    }
    
    /**
     * Success feedback for completed actions
     */
    fun performSuccessFeedback() {
        if (!isHapticEnabled) return
        
        logger.debug(TAG, "Performing success haptic feedback")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 50, 50, 100)
            val amplitudes = intArrayOf(0, LIGHT_AMPLITUDE, 0, MEDIUM_AMPLITUDE)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 50, 100), -1)
        }
    }
    
    /**
     * Error feedback for failed actions
     */
    fun performErrorFeedback() {
        if (!isHapticEnabled) return
        
        logger.debug(TAG, "Performing error haptic feedback")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 50, 100, 50, 100)
            val amplitudes = intArrayOf(0, HEAVY_AMPLITUDE, 0, HEAVY_AMPLITUDE, 0, HEAVY_AMPLITUDE)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 100, 50, 100, 50, 100), -1)
        }
    }
    
    /**
     * Compose haptic feedback integration
     */
    fun performComposeHaptic(hapticFeedback: HapticFeedback, type: HapticFeedbackType) {
        if (!isHapticEnabled) return
        
        logger.debug(TAG, "Performing Compose haptic feedback: $type")
        hapticFeedback.performHapticFeedback(type)
    }
    
    /**
     * Train event specific feedback
     */
    fun performTrainEventFeedback(event: TrainEvent) {
        when (event) {
            TrainEvent.TRAIN_SELECTED -> performSelectionFeedback()
            TrainEvent.TRAIN_CONFLICT -> performConflictAlert()
            TrainEvent.TRAIN_EMERGENCY -> performEmergencyAlert()
            TrainEvent.TRAIN_ARRIVED -> performSuccessFeedback()
            TrainEvent.TRAIN_DELAYED -> performMediumImpact()
            TrainEvent.CONNECTION_LOST -> performErrorFeedback()
            TrainEvent.CONNECTION_RESTORED -> performSuccessFeedback()
        }
        
        logger.info(TAG, "Haptic feedback performed for train event: $event")
    }
    
    /**
     * Configure haptic settings
     */
    fun setHapticEnabled(enabled: Boolean) {
        isHapticEnabled = enabled
        logger.info(TAG, "Haptic feedback ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setHapticIntensity(intensity: HapticIntensity) {
        hapticIntensity = intensity
        logger.info(TAG, "Haptic intensity set to: $intensity")
    }
    
    /**
     * Check if haptic feedback is available
     */
    fun isHapticAvailable(): Boolean {
        return vibrator?.hasVibrator() == true
    }
    
    /**
     * Get current haptic settings
     */
    fun getHapticSettings(): HapticSettings {
        return HapticSettings(
            isEnabled = isHapticEnabled,
            intensity = hapticIntensity,
            isAvailable = isHapticAvailable()
        )
    }
}

/**
 * Train events that trigger haptic feedback
 */
enum class TrainEvent {
    TRAIN_SELECTED,
    TRAIN_CONFLICT,
    TRAIN_EMERGENCY,
    TRAIN_ARRIVED,
    TRAIN_DELAYED,
    CONNECTION_LOST,
    CONNECTION_RESTORED
}

/**
 * Haptic intensity levels
 */
enum class HapticIntensity {
    LIGHT,
    MEDIUM,
    HEAVY
}

/**
 * Haptic feedback settings
 */
data class HapticSettings(
    val isEnabled: Boolean,
    val intensity: HapticIntensity,
    val isAvailable: Boolean
)