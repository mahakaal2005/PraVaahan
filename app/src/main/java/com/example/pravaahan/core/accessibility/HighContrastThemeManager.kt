package com.example.pravaahan.core.accessibility

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.pravaahan.core.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages high contrast themes for outdoor visibility and accessibility compliance
 */
@Singleton
class HighContrastThemeManager @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "HighContrastThemeManager"
    }
    
    private var _isHighContrastEnabled = mutableStateOf(false)
    val isHighContrastEnabled: State<Boolean> = _isHighContrastEnabled
    
    private var _contrastLevel = mutableStateOf(ContrastLevel.NORMAL)
    val contrastLevel: State<ContrastLevel> = _contrastLevel
    
    /**
     * Enable or disable high contrast mode
     */
    fun setHighContrastEnabled(enabled: Boolean) {
        _isHighContrastEnabled.value = enabled
        logger.info(TAG, "High contrast mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Set contrast level
     */
    fun setContrastLevel(level: ContrastLevel) {
        _contrastLevel.value = level
        logger.info(TAG, "Contrast level set to: $level")
    }
    
    /**
     * Get high contrast light color scheme
     */
    fun getHighContrastLightColors(): ColorScheme {
        return when (_contrastLevel.value) {
            ContrastLevel.NORMAL -> lightColorScheme()
            ContrastLevel.MEDIUM -> mediumContrastLightColors()
            ContrastLevel.HIGH -> highContrastLightColors()
            ContrastLevel.MAXIMUM -> maximumContrastLightColors()
        }
    }
    
    /**
     * Get high contrast dark color scheme
     */
    fun getHighContrastDarkColors(): ColorScheme {
        return when (_contrastLevel.value) {
            ContrastLevel.NORMAL -> darkColorScheme()
            ContrastLevel.MEDIUM -> mediumContrastDarkColors()
            ContrastLevel.HIGH -> highContrastDarkColors()
            ContrastLevel.MAXIMUM -> maximumContrastDarkColors()
        }
    }
    
    /**
     * Medium contrast light colors
     */
    private fun mediumContrastLightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF003D82),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD6E3FF),
            onPrimaryContainer = Color(0xFF001C3B),
            secondary = Color(0xFF4A5F78),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD2E4FF),
            onSecondaryContainer = Color(0xFF051B2F),
            tertiary = Color(0xFF5F5B7D),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFE6DEFF),
            onTertiaryContainer = Color(0xFF1B1736),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFFDFBFF),
            onBackground = Color(0xFF1A1C1E),
            surface = Color(0xFFFDFBFF),
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = Color(0xFFE0E2EC),
            onSurfaceVariant = Color(0xFF43474E),
            outline = Color(0xFF74777F),
            outlineVariant = Color(0xFFC4C6D0),
            scrim = Color(0xFF000000)
        )
    }
    
    /**
     * High contrast light colors
     */
    private fun highContrastLightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF002171),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF0061A4),
            onPrimaryContainer = Color.White,
            secondary = Color(0xFF2A3F56),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF4A5F78),
            onSecondaryContainer = Color.White,
            tertiary = Color(0xFF3F3B5B),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFF5F5B7D),
            onTertiaryContainer = Color.White,
            error = Color(0xFF4E0002),
            onError = Color.White,
            errorContainer = Color(0xFF8C0009),
            onErrorContainer = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color(0xFFE0E2EC),
            onSurfaceVariant = Color(0xFF24282F),
            outline = Color(0xFF43474E),
            outlineVariant = Color(0xFF43474E),
            scrim = Color(0xFF000000)
        )
    }
    
    /**
     * Maximum contrast light colors (for extreme outdoor conditions)
     */
    private fun maximumContrastLightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color.Black,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF001D36),
            onPrimaryContainer = Color.White,
            secondary = Color.Black,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF1A2F45),
            onSecondaryContainer = Color.White,
            tertiary = Color.Black,
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFF2F2B4B),
            onTertiaryContainer = Color.White,
            error = Color.Black,
            onError = Color.White,
            errorContainer = Color(0xFF410002),
            onErrorContainer = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color(0xFFF5F5F5),
            onSurfaceVariant = Color.Black,
            outline = Color.Black,
            outlineVariant = Color.Black,
            scrim = Color(0xFF000000)
        )
    }
    
    /**
     * Medium contrast dark colors
     */
    private fun mediumContrastDarkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFFAAC7FF),
            onPrimary = Color(0xFF002E69),
            primaryContainer = Color(0xFF0061A4),
            onPrimaryContainer = Color.White,
            secondary = Color(0xFFB6C9E8),
            onSecondary = Color(0xFF203044),
            secondaryContainer = Color(0xFF4A5F78),
            onSecondaryContainer = Color.White,
            tertiary = Color(0xFFCAC2EA),
            onTertiary = Color(0xFF312C4C),
            tertiaryContainer = Color(0xFF5F5B7D),
            onTertiaryContainer = Color.White,
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color.White,
            background = Color(0xFF101418),
            onBackground = Color(0xFFE1E2E8),
            surface = Color(0xFF101418),
            onSurface = Color(0xFFE1E2E8),
            surfaceVariant = Color(0xFF43474E),
            onSurfaceVariant = Color(0xFFC7CAD4),
            outline = Color(0xFF91949C),
            outlineVariant = Color(0xFF43474E),
            scrim = Color(0xFF000000)
        )
    }
    
    /**
     * High contrast dark colors
     */
    private fun highContrastDarkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFFF6FAFE),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFFAAC7FF),
            onPrimaryContainer = Color.Black,
            secondary = Color(0xFFF6FAFE),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFFB6C9E8),
            onSecondaryContainer = Color.Black,
            tertiary = Color(0xFFFEF7FF),
            onTertiary = Color.Black,
            tertiaryContainer = Color(0xFFCAC2EA),
            onTertiaryContainer = Color.Black,
            error = Color(0xFFFFF9F9),
            onError = Color.Black,
            errorContainer = Color(0xFFFFB4AB),
            onErrorContainer = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF43474E),
            onSurfaceVariant = Color(0xFFF7FAFF),
            outline = Color(0xFFC7CAD4),
            outlineVariant = Color(0xFFC7CAD4),
            scrim = Color(0xFF000000)
        )
    }
    
    /**
     * Maximum contrast dark colors
     */
    private fun maximumContrastDarkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            primaryContainer = Color.White,
            onPrimaryContainer = Color.Black,
            secondary = Color.White,
            onSecondary = Color.Black,
            secondaryContainer = Color.White,
            onSecondaryContainer = Color.Black,
            tertiary = Color.White,
            onTertiary = Color.Black,
            tertiaryContainer = Color.White,
            onTertiaryContainer = Color.Black,
            error = Color.White,
            onError = Color.Black,
            errorContainer = Color.White,
            onErrorContainer = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1A1A1A),
            onSurfaceVariant = Color.White,
            outline = Color.White,
            outlineVariant = Color.White,
            scrim = Color(0xFF000000)
        )
    }
    
    /**
     * Get railway-specific colors for high contrast mode
     */
    fun getRailwayColors(isDarkTheme: Boolean): RailwayColors {
        return if (_isHighContrastEnabled.value) {
            when (_contrastLevel.value) {
                ContrastLevel.NORMAL -> getRailwayColorsNormal(isDarkTheme)
                ContrastLevel.MEDIUM -> getRailwayColorsMedium(isDarkTheme)
                ContrastLevel.HIGH -> getRailwayColorsHigh(isDarkTheme)
                ContrastLevel.MAXIMUM -> getRailwayColorsMaximum(isDarkTheme)
            }
        } else {
            getRailwayColorsNormal(isDarkTheme)
        }
    }
    
    private fun getRailwayColorsNormal(isDarkTheme: Boolean): RailwayColors {
        return if (isDarkTheme) {
            RailwayColors(
                trackColor = Color(0xFF6B7280),
                stationColor = Color(0xFF3B82F6),
                signalGreen = Color(0xFF10B981),
                signalYellow = Color(0xFFF59E0B),
                signalRed = Color(0xFFEF4444),
                trainOnTime = Color(0xFF10B981),
                trainDelayed = Color(0xFFF59E0B),
                trainStopped = Color(0xFFEF4444)
            )
        } else {
            RailwayColors(
                trackColor = Color(0xFF4B5563),
                stationColor = Color(0xFF2563EB),
                signalGreen = Color(0xFF059669),
                signalYellow = Color(0xFFD97706),
                signalRed = Color(0xFFDC2626),
                trainOnTime = Color(0xFF059669),
                trainDelayed = Color(0xFFD97706),
                trainStopped = Color(0xFFDC2626)
            )
        }
    }
    
    private fun getRailwayColorsMedium(isDarkTheme: Boolean): RailwayColors {
        return if (isDarkTheme) {
            RailwayColors(
                trackColor = Color(0xFF9CA3AF),
                stationColor = Color(0xFF60A5FA),
                signalGreen = Color(0xFF34D399),
                signalYellow = Color(0xFFFBBF24),
                signalRed = Color(0xFFF87171),
                trainOnTime = Color(0xFF34D399),
                trainDelayed = Color(0xFFFBBF24),
                trainStopped = Color(0xFFF87171)
            )
        } else {
            RailwayColors(
                trackColor = Color(0xFF374151),
                stationColor = Color(0xFF1D4ED8),
                signalGreen = Color(0xFF047857),
                signalYellow = Color(0xFFB45309),
                signalRed = Color(0xFFB91C1C),
                trainOnTime = Color(0xFF047857),
                trainDelayed = Color(0xFFB45309),
                trainStopped = Color(0xFFB91C1C)
            )
        }
    }
    
    private fun getRailwayColorsHigh(isDarkTheme: Boolean): RailwayColors {
        return if (isDarkTheme) {
            RailwayColors(
                trackColor = Color.White,
                stationColor = Color(0xFF93C5FD),
                signalGreen = Color(0xFF6EE7B7),
                signalYellow = Color(0xFFFDE047),
                signalRed = Color(0xFFFCA5A5),
                trainOnTime = Color(0xFF6EE7B7),
                trainDelayed = Color(0xFFFDE047),
                trainStopped = Color(0xFFFCA5A5)
            )
        } else {
            RailwayColors(
                trackColor = Color.Black,
                stationColor = Color(0xFF1E40AF),
                signalGreen = Color(0xFF065F46),
                signalYellow = Color(0xFF92400E),
                signalRed = Color(0xFF991B1B),
                trainOnTime = Color(0xFF065F46),
                trainDelayed = Color(0xFF92400E),
                trainStopped = Color(0xFF991B1B)
            )
        }
    }
    
    private fun getRailwayColorsMaximum(isDarkTheme: Boolean): RailwayColors {
        return if (isDarkTheme) {
            RailwayColors(
                trackColor = Color.White,
                stationColor = Color.White,
                signalGreen = Color.White,
                signalYellow = Color.White,
                signalRed = Color.White,
                trainOnTime = Color.White,
                trainDelayed = Color.White,
                trainStopped = Color.White
            )
        } else {
            RailwayColors(
                trackColor = Color.Black,
                stationColor = Color.Black,
                signalGreen = Color.Black,
                signalYellow = Color.Black,
                signalRed = Color.Black,
                trainOnTime = Color.Black,
                trainDelayed = Color.Black,
                trainStopped = Color.Black
            )
        }
    }
}

/**
 * Contrast levels for accessibility
 */
enum class ContrastLevel {
    NORMAL,
    MEDIUM,
    HIGH,
    MAXIMUM
}

/**
 * Railway-specific colors for different contrast levels
 */
data class RailwayColors(
    val trackColor: Color,
    val stationColor: Color,
    val signalGreen: Color,
    val signalYellow: Color,
    val signalRed: Color,
    val trainOnTime: Color,
    val trainDelayed: Color,
    val trainStopped: Color
)

/**
 * Composable to provide high contrast theme
 */
@Composable
fun PravahanHighContrastTheme(
    highContrastManager: HighContrastThemeManager,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isHighContrast by highContrastManager.isHighContrastEnabled
    
    val colorScheme = if (isHighContrast) {
        if (darkTheme) {
            highContrastManager.getHighContrastDarkColors()
        } else {
            highContrastManager.getHighContrastLightColors()
        }
    } else {
        if (darkTheme) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}