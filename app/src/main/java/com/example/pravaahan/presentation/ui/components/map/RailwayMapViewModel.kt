package com.example.pravaahan.presentation.ui.components.map

import androidx.lifecycle.ViewModel
import com.example.pravaahan.core.logging.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for railway map components providing dependencies
 */
@HiltViewModel
class RailwayMapViewModel @Inject constructor(
    val renderer: RailwayRenderer,
    val interactionHandler: MapInteractionHandler,
    val logger: Logger
) : ViewModel() {
    
    companion object {
        private const val TAG = "RailwayMapViewModel"
    }
    
    init {
        logger.debug(TAG, "Railway map ViewModel initialized")
    }
    
    override fun onCleared() {
        super.onCleared()
        logger.debug(TAG, "Railway map ViewModel cleared")
    }
}