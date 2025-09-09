package com.example.pravaahan.core.accessibility

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pravaahan.core.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages responsive layout adaptations for different screen sizes and orientations
 */
@Singleton
class ResponsiveLayoutManager @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "ResponsiveLayoutManager"
        
        // Breakpoints based on Material Design guidelines
        private const val COMPACT_WIDTH_DP = 600
        private const val MEDIUM_WIDTH_DP = 840
        private const val EXPANDED_WIDTH_DP = 1200
        
        private const val COMPACT_HEIGHT_DP = 480
        private const val MEDIUM_HEIGHT_DP = 900
        
        // Spacing values for different screen sizes
        private val COMPACT_SPACING = 8.dp
        private val MEDIUM_SPACING = 16.dp
        private val EXPANDED_SPACING = 24.dp
        
        // Content padding for different screen sizes
        private val COMPACT_PADDING = 16.dp
        private val MEDIUM_PADDING = 24.dp
        private val EXPANDED_PADDING = 32.dp
    }
    
    /**
     * Get current screen size class
     */
    @Composable
    fun getScreenSizeClass(): ScreenSizeClass {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        
        val screenWidthDp = configuration.screenWidthDp
        val screenHeightDp = configuration.screenHeightDp
        
        val widthClass = when {
            screenWidthDp < COMPACT_WIDTH_DP -> WindowSizeClass.COMPACT
            screenWidthDp < MEDIUM_WIDTH_DP -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }
        
        val heightClass = when {
            screenHeightDp < COMPACT_HEIGHT_DP -> WindowSizeClass.COMPACT
            screenHeightDp < MEDIUM_HEIGHT_DP -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }
        
        val sizeClass = ScreenSizeClass(
            widthClass = widthClass,
            heightClass = heightClass,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            isLandscape = screenWidthDp > screenHeightDp
        )
        
        logger.debug(TAG, "Screen size class: $sizeClass")
        return sizeClass
    }
    
    /**
     * Get appropriate spacing for current screen size
     */
    @Composable
    fun getSpacing(): Dp {
        val sizeClass = getScreenSizeClass()
        return when (sizeClass.widthClass) {
            WindowSizeClass.COMPACT -> COMPACT_SPACING
            WindowSizeClass.MEDIUM -> MEDIUM_SPACING
            WindowSizeClass.EXPANDED -> EXPANDED_SPACING
        }
    }
    
    /**
     * Get appropriate content padding for current screen size
     */
    @Composable
    fun getContentPadding(): Dp {
        val sizeClass = getScreenSizeClass()
        return when (sizeClass.widthClass) {
            WindowSizeClass.COMPACT -> COMPACT_PADDING
            WindowSizeClass.MEDIUM -> MEDIUM_PADDING
            WindowSizeClass.EXPANDED -> EXPANDED_PADDING
        }
    }
    
    /**
     * Get column count for grid layouts based on screen size
     */
    @Composable
    fun getGridColumnCount(): Int {
        val sizeClass = getScreenSizeClass()
        return when (sizeClass.widthClass) {
            WindowSizeClass.COMPACT -> if (sizeClass.isLandscape) 2 else 1
            WindowSizeClass.MEDIUM -> if (sizeClass.isLandscape) 3 else 2
            WindowSizeClass.EXPANDED -> if (sizeClass.isLandscape) 4 else 3
        }
    }
    
    /**
     * Determine if layout should use navigation rail instead of bottom navigation
     */
    @Composable
    fun shouldUseNavigationRail(): Boolean {
        val sizeClass = getScreenSizeClass()
        return sizeClass.widthClass >= WindowSizeClass.MEDIUM
    }
    
    /**
     * Determine if layout should show side panel
     */
    @Composable
    fun shouldShowSidePanel(): Boolean {
        val sizeClass = getScreenSizeClass()
        return sizeClass.widthClass >= WindowSizeClass.EXPANDED
    }
    
    /**
     * Get appropriate map height based on screen size and orientation
     */
    @Composable
    fun getMapHeight(): Dp {
        val sizeClass = getScreenSizeClass()
        val screenHeight = sizeClass.screenHeightDp.dp
        
        return when {
            sizeClass.isLandscape -> screenHeight * 0.7f
            sizeClass.heightClass == WindowSizeClass.COMPACT -> screenHeight * 0.5f
            else -> screenHeight * 0.4f
        }
    }
    
    /**
     * Get train card width based on screen size
     */
    @Composable
    fun getTrainCardWidth(): Dp {
        val sizeClass = getScreenSizeClass()
        val screenWidth = sizeClass.screenWidthDp.dp
        
        return when (sizeClass.widthClass) {
            WindowSizeClass.COMPACT -> screenWidth - (getContentPadding() * 2)
            WindowSizeClass.MEDIUM -> (screenWidth - (getContentPadding() * 3)) / 2
            WindowSizeClass.EXPANDED -> (screenWidth - (getContentPadding() * 4)) / 3
        }
    }
    
    /**
     * Adaptive layout modifier that adjusts based on screen size
     */
    @Composable
    fun Modifier.adaptiveLayout(): Modifier {
        val sizeClass = getScreenSizeClass()
        val padding = getContentPadding()
        
        return this.then(
            when (sizeClass.widthClass) {
                WindowSizeClass.COMPACT -> Modifier.padding(horizontal = padding)
                WindowSizeClass.MEDIUM -> Modifier.padding(horizontal = padding * 1.5f)
                WindowSizeClass.EXPANDED -> Modifier.padding(horizontal = padding * 2f)
            }
        )
    }
    
    /**
     * Adaptive arrangement for rows and columns
     */
    @Composable
    fun getAdaptiveArrangement(): Arrangement.HorizontalOrVertical {
        val spacing = getSpacing()
        return Arrangement.spacedBy(spacing)
    }
    
    /**
     * Get appropriate text scaling for accessibility
     */
    @Composable
    fun getTextScaling(): Float {
        val sizeClass = getScreenSizeClass()
        
        // Increase text size on smaller screens for better readability
        return when (sizeClass.widthClass) {
            WindowSizeClass.COMPACT -> 1.1f
            WindowSizeClass.MEDIUM -> 1.0f
            WindowSizeClass.EXPANDED -> 0.9f
        }
    }
    
    /**
     * Determine layout strategy for dashboard
     */
    @Composable
    fun getDashboardLayoutStrategy(): DashboardLayoutStrategy {
        val sizeClass = getScreenSizeClass()
        
        return when {
            sizeClass.widthClass == WindowSizeClass.COMPACT -> {
                DashboardLayoutStrategy.SINGLE_COLUMN
            }
            sizeClass.widthClass == WindowSizeClass.MEDIUM -> {
                if (sizeClass.isLandscape) {
                    DashboardLayoutStrategy.TWO_COLUMN_HORIZONTAL
                } else {
                    DashboardLayoutStrategy.SINGLE_COLUMN_WIDE
                }
            }
            else -> {
                if (sizeClass.isLandscape) {
                    DashboardLayoutStrategy.THREE_COLUMN_HORIZONTAL
                } else {
                    DashboardLayoutStrategy.TWO_COLUMN_VERTICAL
                }
            }
        }
    }
    
    /**
     * Get touch target size based on accessibility needs
     */
    @Composable
    fun getTouchTargetSize(isAccessibilityEnabled: Boolean = false): Dp {
        val sizeClass = getScreenSizeClass()
        val baseSize = when (sizeClass.widthClass) {
            WindowSizeClass.COMPACT -> 48.dp
            WindowSizeClass.MEDIUM -> 44.dp
            WindowSizeClass.EXPANDED -> 40.dp
        }
        
        return if (isAccessibilityEnabled) baseSize * 1.2f else baseSize
    }
}

/**
 * Screen size classification
 */
data class ScreenSizeClass(
    val widthClass: WindowSizeClass,
    val heightClass: WindowSizeClass,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isLandscape: Boolean
)

/**
 * Window size classes based on Material Design
 */
enum class WindowSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/**
 * Dashboard layout strategies for different screen sizes
 */
enum class DashboardLayoutStrategy {
    SINGLE_COLUMN,
    SINGLE_COLUMN_WIDE,
    TWO_COLUMN_HORIZONTAL,
    TWO_COLUMN_VERTICAL,
    THREE_COLUMN_HORIZONTAL
}

/**
 * Composable to remember screen size class
 */
@Composable
fun rememberScreenSizeClass(): ScreenSizeClass {
    val configuration = LocalConfiguration.current
    
    return remember(configuration.screenWidthDp, configuration.screenHeightDp, configuration.orientation) {
        val screenWidthDp = configuration.screenWidthDp
        val screenHeightDp = configuration.screenHeightDp
        
        val widthClass = when {
            screenWidthDp < 600 -> WindowSizeClass.COMPACT
            screenWidthDp < 840 -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }
        
        val heightClass = when {
            screenHeightDp < 480 -> WindowSizeClass.COMPACT
            screenHeightDp < 900 -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }
        
        ScreenSizeClass(
            widthClass = widthClass,
            heightClass = heightClass,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            isLandscape = screenWidthDp > screenHeightDp
        )
    }
}