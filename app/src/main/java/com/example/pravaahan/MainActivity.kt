package com.example.pravaahan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.pravaahan.presentation.navigation.PravahanNavigation
import com.example.pravaahan.presentation.ui.theme.PravahanTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for PraVaahan railway control application
 * Implements single-activity architecture with Jetpack Navigation Compose
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d(TAG, "🚀 MainActivity.onCreate() called")
        
        try {
            super.onCreate(savedInstanceState)
            android.util.Log.d(TAG, "✅ super.onCreate() completed")
            
            enableEdgeToEdge()
            android.util.Log.d(TAG, "✅ enableEdgeToEdge() completed")
            
            android.util.Log.d(TAG, "🎨 About to call setContent...")
            
            android.util.Log.d(TAG, "🧪 Testing minimal Compose UI...")
            
            setContent {
                android.util.Log.d(TAG, "🎨 setContent block executing...")
                
                // TEST: Now test Navigation specifically
                android.util.Log.d(TAG, "🧪 Testing Navigation...")
                PravahanTheme {
                    android.util.Log.d(TAG, "✅ PravahanTheme loaded successfully!")
                    
                    android.util.Log.d(TAG, "🧪 Creating NavController...")
                    val navController = androidx.navigation.compose.rememberNavController()
                    android.util.Log.d(TAG, "✅ NavController created successfully!")
                    
                    android.util.Log.d(TAG, "🧪 Calling PravahanNavigation...")
                    PravahanNavigation(
                        navController = navController,
                        modifier = androidx.compose.ui.Modifier.fillMaxSize()
                    )
                    android.util.Log.d(TAG, "✅ PravahanNavigation called successfully!")
                }
            }
            
            android.util.Log.d(TAG, "✅ Minimal test UI setup completed")
            
            android.util.Log.d(TAG, "✅ setContent completed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "🚨 CRITICAL ERROR in MainActivity.onCreate(): ${e.message}", e)
            e.printStackTrace()
        }
    }
}

/**
 * Root composable for the PraVaahan application
 * Sets up theme and navigation structure
 */
@Composable
fun PravahanApp(modifier: Modifier = Modifier) {
    // Debug logging
    android.util.Log.d("MainActivity", "PravahanApp composable called")
    
    PravahanTheme {
        android.util.Log.d("MainActivity", "PravahanTheme applied")
        
        val navController = rememberNavController()
        android.util.Log.d("MainActivity", "NavController created")
        
        PravahanNavigation(
            navController = navController,
            modifier = modifier.fillMaxSize()
        )
        android.util.Log.d("MainActivity", "PravahanNavigation called")
    }
}

@Preview(showBackground = true)
@Composable
private fun PravahanAppPreview() {
    PravahanApp()
}
