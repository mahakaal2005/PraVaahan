package com.example.pravaahan.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.pravaahan.PravahanApp
import com.example.pravaahan.core.health.AppHealthStatus
import com.example.pravaahan.core.health.HealthStatusUtils
import com.example.pravaahan.presentation.ui.theme.PravahanTheme
import com.example.pravaahan.presentation.ui.theme.ThemeManager
import com.example.pravaahan.presentation.ui.theme.ThemePreference
import kotlinx.coroutines.launch

/**
 * Settings screen - App configuration and preferences
 * Provides access to theme settings, notifications, and app information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    themeManager: ThemeManager? = null // Optional for preview
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    
    // Theme state
    val currentTheme by (themeManager?.themePreference?.collectAsState(initial = ThemePreference.SYSTEM_DEFAULT)
        ?: remember { mutableStateOf(ThemePreference.SYSTEM_DEFAULT) })
    
    // Local settings state (TODO: Move to ViewModel)
    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var autoRefreshEnabled by remember { mutableStateOf(true) }
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Settings
            item {
                SettingsSection(
                    title = "Appearance",
                    icon = Icons.Default.Settings
                ) {
                    ThemeSettingItem(
                        currentTheme = currentTheme,
                        onThemeChange = { newTheme ->
                            themeManager?.let { manager ->
                                scope.launch {
                                    manager.setThemePreference(newTheme)
                                }
                            }
                        }
                    )
                }
            }
            
            // Notification Settings
            item {
                SettingsSection(
                    title = "Notifications",
                    icon = Icons.Default.Notifications
                ) {
                    SettingsSwitchItem(
                        title = "Enable Notifications",
                        subtitle = "Receive alerts for conflicts and updates",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                    
                    SettingsSwitchItem(
                        title = "Sound Alerts",
                        subtitle = "Play sound for critical alerts",
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        enabled = notificationsEnabled
                    )
                    
                    SettingsSwitchItem(
                        title = "Vibration",
                        subtitle = "Vibrate for urgent notifications",
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it },
                        enabled = notificationsEnabled
                    )
                }
            }
            
            // System Settings
            item {
                SettingsSection(
                    title = "System",
                    icon = Icons.Default.Settings
                ) {
                    SettingsSwitchItem(
                        title = "Auto Refresh",
                        subtitle = "Automatically refresh train data",
                        checked = autoRefreshEnabled,
                        onCheckedChange = { autoRefreshEnabled = it }
                    )
                }
            }
            
            // System Health Status
            item {
                SystemHealthSection()
            }
            
            // Security Settings
            item {
                SettingsSection(
                    title = "Security",
                    icon = Icons.Default.Lock
                ) {
                    SettingsInfoItem(
                        title = "Connection Status",
                        subtitle = "Supabase: Connected",
                        value = "Secure"
                    )
                    
                    SettingsInfoItem(
                        title = "Data Encryption",
                        subtitle = "All data is encrypted in transit",
                        value = "Enabled"
                    )
                }
            }
            
            // App Information
            item {
                SettingsSection(
                    title = "About",
                    icon = Icons.Default.Info
                ) {
                    SettingsInfoItem(
                        title = "Version",
                        subtitle = "PraVaahan Railway Control System",
                        value = "1.0.0"
                    )
                    
                    SettingsInfoItem(
                        title = "Build",
                        subtitle = "Debug build for development",
                        value = "DEBUG"
                    )
                    
                    SettingsInfoItem(
                        title = "Last Updated",
                        subtitle = "Application last updated",
                        value = "Today"
                    )
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            content()
        }
    }
}

@Composable
private fun ThemeSettingItem(
    currentTheme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ThemeOption(
            title = "System Default",
            subtitle = "Follow system theme setting",
            selected = currentTheme == ThemePreference.SYSTEM_DEFAULT,
            onClick = { onThemeChange(ThemePreference.SYSTEM_DEFAULT) }
        )
        
        ThemeOption(
            title = "Light Theme",
            subtitle = "Always use light theme",
            selected = currentTheme == ThemePreference.LIGHT,
            onClick = { onThemeChange(ThemePreference.LIGHT) }
        )
        
        ThemeOption(
            title = "Dark Theme",
            subtitle = "Always use dark theme",
            selected = currentTheme == ThemePreference.DARK,
            onClick = { onThemeChange(ThemePreference.DARK) }
        )
    }
}

@Composable
private fun ThemeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) 
                    MaterialTheme.colorScheme.onSurfaceVariant 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    subtitle: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SystemHealthSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PravahanApp
    var healthStatus by remember { mutableStateOf(app.appHealthStatus) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    SettingsSection(
        title = "System Health",
        icon = Icons.Default.Favorite,
        modifier = modifier
    ) {
        healthStatus?.let { status ->
            // Overall Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = HealthStatusUtils.getStatusIcon(status),
                        contentDescription = "Health status",
                        tint = HealthStatusUtils.getStatusColor(status),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = HealthStatusUtils.getShortStatusText(status),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = HealthStatusUtils.getStatusColor(status)
                        )
                        
                        Text(
                            text = HealthStatusUtils.getStatusMessage(status),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                IconButton(
                    onClick = {
                        isRefreshing = true
                        scope.launch {
                            app.performRuntimeHealthCheck { newStatus ->
                                healthStatus = newStatus
                                isRefreshing = false
                            }
                        }
                    },
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh health status",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Execution Summary
            Text(
                text = HealthStatusUtils.getExecutionSummary(status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            // Component Status Summary
            val detailedResults = HealthStatusUtils.getDetailedResults(status)
            val successCount = detailedResults.count { it.isSuccess }
            val warningCount = detailedResults.count { it.isWarning }
            val errorCount = detailedResults.count { it.isError }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthStatusBadge(
                    label = "Passed",
                    count = successCount,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (warningCount > 0) {
                    HealthStatusBadge(
                        label = "Warnings",
                        count = warningCount,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                if (errorCount > 0) {
                    HealthStatusBadge(
                        label = "Failed",
                        count = errorCount,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
        } ?: run {
            Text(
                text = if (isRefreshing) "Checking system health..." else "Health status not available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HealthStatusBadge(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PravahanTheme {
        SettingsScreen(
            onBackClick = { }
        )
    }
}