package com.example.pravaahan.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for PraVaahan theme system
 */
class ThemeTest {

    @Test
    fun `light color scheme has correct primary color`() {
        val lightScheme = lightColorScheme(
            primary = PrimaryLight,
            onPrimary = OnPrimaryLight,
            primaryContainer = PrimaryContainerLight,
            onPrimaryContainer = OnPrimaryContainerLight
        )
        
        assertEquals(Color(0xFF005AC1), lightScheme.primary)
        assertEquals(Color(0xFFFFFFFF), lightScheme.onPrimary)
    }

    @Test
    fun `dark color scheme has correct primary color`() {
        val darkScheme = darkColorScheme(
            primary = PrimaryDark,
            onPrimary = OnPrimaryDark,
            primaryContainer = PrimaryContainerDark,
            onPrimaryContainer = OnPrimaryContainerDark
        )
        
        assertEquals(Color(0xFFA8C7FA), darkScheme.primary)
        assertEquals(Color(0xFF002E69), darkScheme.onPrimary)
    }

    @Test
    fun `status colors are properly defined`() {
        // Test that status colors are distinct and appropriate
        assertNotEquals(OnTimeGreen, DelayedAmber)
        assertNotEquals(DelayedAmber, ConflictRed)
        assertNotEquals(ConflictRed, OnTimeGreen)
        
        // Test that container colors are lighter versions
        assertNotEquals(OnTimeGreen, OnTimeGreenContainer)
        assertNotEquals(DelayedAmber, DelayedAmberContainer)
        assertNotEquals(ConflictRed, ConflictRedContainer)
    }

    @Test
    fun `priority colors are properly defined`() {
        // Test that priority colors are distinct
        assertNotEquals(ExpressPurple, HighPriorityRed)
        assertNotEquals(HighPriorityRed, MediumPriorityOrange)
        assertNotEquals(MediumPriorityOrange, LowPriorityGreen)
        
        // Test that container colors exist
        assertNotEquals(ExpressPurple, ExpressPurpleContainer)
        assertNotEquals(HighPriorityRed, HighPriorityRedContainer)
    }
}