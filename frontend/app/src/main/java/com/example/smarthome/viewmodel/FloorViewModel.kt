package com.example.smarthome.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.smarthome.data.model.Floor
import com.example.smarthome.data.repository.FloorRepository

class FloorViewModel : ViewModel() {

    private val repository =
        FloorRepository()

    var floors =
        mutableStateOf<List<Floor>>(emptyList())
        private set

    var isLoading =
        mutableStateOf(true)
        private set

    var errorMessage =
        mutableStateOf<String?>(null)
        private set

    init {
        observeFloors()
    }

    private fun observeFloors() {

        isLoading.value = true

        repository.observeFloors(

            onResult = { floorList ->

                floors.value = floorList
                isLoading.value = false
                errorMessage.value = null
            },

            onError = { exception ->

                isLoading.value = false

                errorMessage.value =
                    exception.message
                        ?: "Unable to load floors"
            }
        )
    }
}