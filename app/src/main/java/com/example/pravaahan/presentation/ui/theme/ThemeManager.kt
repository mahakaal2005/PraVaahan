package com.example.pravaahan.presentation.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Theme preference options for PraVaahan
 */
enum class ThemePreference {
    SYSTEM_DEFAULT,
    LIGHT,
    DARK
}

/**
 * Manages theme preferences for the PraVaahan application
 * Provides persistent storage and reactive updates for theme changes
 */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val useSystemThemeKey = booleanPreferencesKey("use_system_theme")

    /**
     * Flow of current theme preference
     */
    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { preferences ->
        val useSystemTheme = preferences[useSystemThemeKey] ?: true
        val isDarkTheme = preferences[isDarkThemeKey] ?: false
        
        when {
            useSystemTheme -> ThemePreference.SYSTEM_DEFAULT
            isDarkTheme -> ThemePreference.DARK
            else -> ThemePreference.LIGHT
        }
    }

    /**
     * Flow indicating if dark theme should be used
     */
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[isDarkThemeKey] ?: false
    }

    /**
     * Flow indicating if system theme should be followed
     */
    val useSystemTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[useSystemThemeKey] ?: true
    }

    /**
     * Set theme preference
     */
    suspend fun setThemePreference(preference: ThemePreference) {
        context.dataStore.edit { preferences ->
            when (preference) {
                ThemePreference.SYSTEM_DEFAULT -> {
                    preferences[useSystemThemeKey] = true
                }
                ThemePreference.LIGHT -> {
                    preferences[useSystemThemeKey] = false
                    preferences[isDarkThemeKey] = false
                }
                ThemePreference.DARK -> {
                    preferences[useSystemThemeKey] = false
                    preferences[isDarkThemeKey] = true
                }
            }
        }
    }

    /**
     * Toggle between light and dark theme (disables system theme)
     */
    suspend fun toggleTheme() {
        context.dataStore.edit { preferences ->
            val currentDarkTheme = preferences[isDarkThemeKey] ?: false
            preferences[useSystemThemeKey] = false
            preferences[isDarkThemeKey] = !currentDarkTheme
        }
    }
}

/**
 * Composable function to get current theme preference
 */
@Composable
fun rememberThemePreference(themeManager: ThemeManager): ThemePreference {
    val themePreference by themeManager.themePreference.collectAsState(initial = ThemePreference.SYSTEM_DEFAULT)
    return themePreference
}