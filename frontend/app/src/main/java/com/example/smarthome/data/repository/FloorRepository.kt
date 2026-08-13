package com.example.smarthome.data.repository

import com.example.smarthome.data.model.Floor
import com.google.firebase.firestore.FirebaseFirestore

class FloorRepository {

    private val firestore =
        FirebaseFirestore.getInstance()

    private val floorsCollection =
        firestore.collection("floors")

    fun observeFloors(
        onResult: (List<Floor>) -> Unit,
        onError: (Exception) -> Unit
    ) {

        floorsCollection
            .orderBy("level")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {

                    val floors =
                        snapshot.toObjects(Floor::class.java)

                    onResult(floors)
                }
            }
    }
}