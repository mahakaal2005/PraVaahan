package com.example.pravaahan.presentation.ui.components.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.pravaahan.data.sample.SampleRailwayData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapStateTest {
    
    private lateinit var mapState: MapState
    
    @BeforeEach
    fun setup() {
        val config = SampleRailwayData.createSampleSection()
        mapState = MapState(config = config)
        mapState.canvasSize = Size(800f, 600f)
    }
    
    @Test
    fun `updateZoom should constrain zoom within limits`() {
        // Test minimum zoom constraint
        mapState.updateZoom(0.1f)
        assertEquals(0.5f, mapState.zoom)
        
        // Test maximum zoom constraint
        mapState.updateZoom(10f)
        assertEquals(5.0f, mapState.zoom)
        
        // Test valid zoom
        mapState.updateZoom(2.0f)
        assertEquals(2.0f, mapState.zoom)
    }
    
    @Test
    fun `railwayToScreen should convert coordinates correctly`() {
        // Given
        val railwayPosition = Offset(500f, 200f) // Middle of railway bounds
        
        // When
        val screenPosition = mapState.railwayToScreen(railwayPosition)
        
        // Then
        assertTrue(screenPosition.x > 0)
        assertTrue(screenPosition.y > 0)
        assertTrue(screenPosition.x <= mapState.canvasSize.width)
        assertTrue(screenPosition.y <= mapState.canvasSize.height)
    }
    
    @Test
    fun `screenToRailway should convert coordinates correctly`() {
        // Given
        val screenPosition = Offset(400f, 300f) // Middle of screen
        
        // When
        val railwayPosition = mapState.screenToRailway(screenPosition)
        
        // Then
        assertTrue(railwayPosition.x >= 0f)
        assertTrue(railwayPosition.y >= 0f)
    }
    
    @Test
    fun `coordinate conversion should be reversible`() {
        // Given
        val originalRailwayPosition = Offset(250f, 150f)
        
        // When
        val screenPosition = mapState.railwayToScreen(originalRailwayPosition)
        val convertedBackPosition = mapState.screenToRailway(screenPosition)
        
        // Then
        assertEquals(originalRailwayPosition.x, convertedBackPosition.x, 1f)
        assertEquals(originalRailwayPosition.y, convertedBackPosition.y, 1f)
    }
    
    @Test
    fun `isPositionInBounds should work correctly`() {
        // Given
        val validScreenPosition = Offset(400f, 300f)
        val invalidScreenPosition = Offset(-100f, -100f)
        
        // When & Then
        assertTrue(mapState.isPositionInBounds(validScreenPosition))
        // Note: Invalid position test depends on pan offset and zoom
    }
    
    @Test
    fun `updatePanOffset should respect bounds`() {
        // Given
        val initialOffset = mapState.panOffset
        val largeOffset = Offset(10000f, 10000f)
        
        // When
        mapState.updatePanOffset(largeOffset)
        
        // Then
        // Pan offset should be constrained (exact values depend on zoom and canvas size)
        assertTrue(mapState.panOffset.x < largeOffset.x)
        assertTrue(mapState.panOffset.y < largeOffset.y)
    }
    
    @Test
    fun `reset should restore initial state`() {
        // Given
        mapState.updateZoom(3f)
        mapState.updatePanOffset(Offset(100f, 100f))
        
        // When
        mapState.reset()
        
        // Then
        assertEquals(1f, mapState.zoom)
        assertEquals(Offset.Zero, mapState.panOffset)
    }
    
    @Test
    fun `fitToContent should adjust zoom appropriately`() {
        // Given
        val initialZoom = mapState.zoom
        
        // When
        mapState.fitToContent()
        
        // Then
        assertTrue(mapState.zoom > 0)
        assertTrue(mapState.zoom <= 5.0f) // Within max zoom
        assertEquals(Offset.Zero, mapState.panOffset)
    }
    
    @Test
    fun `zoom changes should affect coordinate conversion`() {
        // Given
        val railwayPosition = Offset(500f, 200f)
        val screenPos1 = mapState.railwayToScreen(railwayPosition)
        
        // When
        mapState.updateZoom(2f)
        val screenPos2 = mapState.railwayToScreen(railwayPosition)
        
        // Then
        // At higher zoom, the same railway position should appear at different screen coordinates
        assertTrue(screenPos1 != screenPos2)
    }
}