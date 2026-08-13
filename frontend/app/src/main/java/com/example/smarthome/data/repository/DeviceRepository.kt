package com.example.smarthome.data.repository

import com.example.smarthome.data.model.Device
import com.google.firebase.firestore.FirebaseFirestore

class DeviceRepository {

    private val firestore =
        FirebaseFirestore.getInstance()

    private val devicesCollection =
        firestore.collection("devices")


    // Observe all devices
    fun observeDevices(
        onResult: (List<Device>) -> Unit,
        onError: (Exception) -> Unit
    ) {

        devicesCollection.addSnapshotListener { snapshot, error ->

            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {

                val devices =
                    snapshot.documents.mapNotNull { document ->

                        try {

                            document
                                .toObject(Device::class.java)
                                ?.copy(
                                    id = document.id
                                )

                        } catch (e: Exception) {

                            null
                        }
                    }

                onResult(devices)
            }
        }
    }


    // Observe devices belonging to one floor
    fun observeDevicesByFloor(
        floorId: String,
        onResult: (List<Device>) -> Unit,
        onError: (Exception) -> Unit
    ) {

        if (floorId.isBlank()) {

            onResult(emptyList())
            return
        }

        devicesCollection
            .whereEqualTo("floorId", floorId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {

                    val devices =
                        snapshot.documents.mapNotNull { document ->

                            try {

                                document
                                    .toObject(Device::class.java)
                                    ?.copy(
                                        id = document.id
                                    )

                            } catch (e: Exception) {

                                null
                            }
                        }

                    onResult(devices)
                }
            }
    }


    // Update device status
    fun updateDeviceStatus(
        deviceId: String,
        status: String,
        onComplete: (Boolean) -> Unit
    ) {

        if (deviceId.isBlank()) {

            onComplete(false)
            return
        }

        devicesCollection
            .document(deviceId)
            .update("status", status)
            .addOnSuccessListener {

                onComplete(true)
            }
            .addOnFailureListener {

                onComplete(false)
            }
    }
}