package com.example.pravaahan.presentation.ui.components.map

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pravaahan.data.sample.SampleRailwayData
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RailwaySectionMapTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun railwaySectionMap_displaysCorrectly() {
        // Given
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val sectionConfig = SampleRailwayData.createSampleSection()
        var selectedTrainId: String? = null
        
        // When
        composeTestRule.setContent {
            RailwaySectionMap(
                trainStates = trainStates,
                sectionConfig = sectionConfig,
                onTrainSelected = { trainId ->
                    selectedTrainId = trainId
                }
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Railway section map showing ${trainStates.size} trains on ${sectionConfig.name}")
            .assertIsDisplayed()
    }
    
    @Test
    fun railwaySectionMap_handlesTrainSelection() {
        // Given
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val sectionConfig = SampleRailwayData.createSampleSection()
        var selectedTrainId: String? = null
        
        // When
        composeTestRule.setContent {
            RailwaySectionMap(
                trainStates = trainStates,
                sectionConfig = sectionConfig,
                onTrainSelected = { trainId ->
                    selectedTrainId = trainId
                }
            )
        }
        
        // Find and click on a train marker
        composeTestRule
            .onNodeWithContentDescription(
                "Train ${trainStates.first().train.name}, " +
                "status: ${trainStates.first().train.status}, " +
                "speed: ${trainStates.first().currentPosition?.speed ?: 0} km/h, " +
                "last update: ${trainStates.first().lastUpdateTime}",
                substring = true
            )
            .performClick()
        
        // Then
        composeTestRule.waitForIdle()
        assert(selectedTrainId == trainStates.first().train.id)
    }
    
    @Test
    fun railwaySectionMap_displaysMapControls() {
        // Given
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val sectionConfig = SampleRailwayData.createSampleSection()
        
        // When
        composeTestRule.setContent {
            RailwaySectionMap(
                trainStates = trainStates,
                sectionConfig = sectionConfig,
                onTrainSelected = { }
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Zoom in on railway map")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Zoom out on railway map")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Fit railway section to view")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Reset map view to default")
            .assertIsDisplayed()
    }
    
    @Test
    fun railwaySectionMap_zoomControlsWork() {
        // Given
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val sectionConfig = SampleRailwayData.createSampleSection()
        
        // When
        composeTestRule.setContent {
            RailwaySectionMap(
                trainStates = trainStates,
                sectionConfig = sectionConfig,
                onTrainSelected = { }
            )
        }
        
        // Test zoom in
        composeTestRule
            .onNodeWithContentDescription("Zoom in on railway map")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Test zoom out
        composeTestRule
            .onNodeWithContentDescription("Zoom out on railway map")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Test fit to content
        composeTestRule
            .onNodeWithContentDescription("Fit railway section to view")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Test reset
        composeTestRule
            .onNodeWithContentDescription("Reset map view to default")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then - no crashes should occur
    }
    
    @Test
    fun railwaySectionMap_handlesHighContrastMode() {
        // Given
        val trainStates = SampleRailwayData.createSampleTrainStates()
        val sectionConfig = SampleRailwayData.createSampleSection()
        
        // When
        composeTestRule.setContent {
            RailwaySectionMap(
                trainStates = trainStates,
                sectionConfig = sectionConfig,
                onTrainSelected = { },
                isHighContrast = true
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Railway section map showing ${trainStates.size} trains on ${sectionConfig.name}")
            .assertIsDisplayed()
    }
    
    @Test
    fun railwaySectionMap_handlesEmptyTrainList() {
        // Given
        val sectionConfig = SampleRailwayData.createSampleSection()
        
        // When
        composeTestRule.setContent {
            RailwaySectionMap(
                trainStates = emptyList(),
                sectionConfig = sectionConfig,
                onTrainSelected = { }
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Railway section map showing 0 trains on ${sectionConfig.name}")
            .assertIsDisplayed()
    }
}