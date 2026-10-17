package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService

class PickupRepository(
    private val firestoreService: FirestoreService = FirestoreService()
) {
    fun fetchPickupsForUser(userId: String, onResult: (ResultState<List<PickupModel>>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)
        firestoreService.db.collection("pickups")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val pickups = snapshot.toObjects(PickupModel::class.java)
                onResult(ResultState.Success(pickups))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun schedulePickup(pickup: PickupModel, onResult: (ResultState<String>) -> Unit) {
        onResult(ResultState.Loading)
        val docRef = if (pickup.pickupId.isNotEmpty()) {
            firestoreService.db.collection("pickups").document(pickup.pickupId)
        } else {
            firestoreService.db.collection("pickups").document()
        }

        val newPickup = pickup.copy(pickupId = docRef.id)

        docRef.set(newPickup)
            .addOnSuccessListener {
                onResult(ResultState.Success(docRef.id))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }
}
