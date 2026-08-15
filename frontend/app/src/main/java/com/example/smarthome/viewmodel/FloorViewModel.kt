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

    fun getFloor(floorId: String, onResult: (Floor?) -> Unit) {
        repository.getFloor(floorId, onResult, {
            errorMessage.value = it.message
        })
    }

    fun addFloor(name: String, level: Int) {
        isLoading.value = true
        val newFloor = Floor(name = name, level = level)
        repository.addFloor(newFloor) { success ->
            isLoading.value = false
            if (!success) {
                errorMessage.value = "Failed to add floor"
            }
        }
    }

    fun deleteFloor(floorId: String) {
        isLoading.value = true
        repository.deleteFloor(floorId) { success ->
            isLoading.value = false
            if (!success) {
                errorMessage.value = "Failed to delete floor"
            }
        }
    }
}