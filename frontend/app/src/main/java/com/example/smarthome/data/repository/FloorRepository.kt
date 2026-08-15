package com.example.smarthome.data.repository

import com.example.smarthome.data.model.Floor
import com.google.firebase.firestore.FirebaseFirestore

class FloorRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val floorsCollection = firestore.collection("floors")

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
                    val floors = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Floor::class.java)?.copy(id = doc.id)
                    }
                    onResult(floors)
                }
            }
    }

    fun getFloor(
        floorId: String,
        onResult: (Floor?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        floorsCollection.document(floorId)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.toObject(Floor::class.java)?.copy(id = snapshot.id))
            }
            .addOnFailureListener { onError(it) }
    }

    fun addFloor(
        floor: Floor,
        onComplete: (Boolean) -> Unit
    ) {
        val docRef = floorsCollection.document()
        val floorWithId = floor.copy(id = docRef.id)

        docRef.set(floorWithId)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun deleteFloor(
        floorId: String,
        onComplete: (Boolean) -> Unit
    ) {
        floorsCollection.document(floorId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
