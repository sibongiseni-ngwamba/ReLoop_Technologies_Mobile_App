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
                try {
                    val seededIds = setOf("pickup_completed_1", "pickup_scheduled_1", "pickup_in_progress_1")
                    onResult(ResultState.Success(snapshot.toObjects(PickupModel::class.java)
                        .filterNot { it.pickupId in seededIds }))
                } catch (exception: RuntimeException) {
                    onResult(ResultState.Error(exception = exception, message = "Could not read pickup records."))
                }
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun getDefaultPickups(userId: String): List<PickupModel> {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        return listOf(
            PickupModel(
                pickupId = "pickup_completed_1",
                userId = userId,
                wasteType = "Glass Bottles, Metals",
                wasteTypes = listOf("Glass Bottles", "Metals"),
                address = "123 Eco Green St, Silicon Valley",
                status = "Completed",
                scheduledDate = now - (2 * dayMs),
                completedAt = now - (2 * dayMs)
            ),
            PickupModel(
                pickupId = "pickup_scheduled_1",
                userId = userId,
                wasteType = "Recyclables (Plastic, Paper)",
                wasteTypes = listOf("Plastic", "Paper"),
                address = "123 Eco Green St, Silicon Valley",
                status = "Scheduled",
                scheduledDate = now + (3 * dayMs)
            ),
            PickupModel(
                pickupId = "pickup_in_progress_1",
                userId = userId,
                wasteType = "Electronics, Cardboard",
                wasteTypes = listOf("Electronics", "Cardboard"),
                address = "123 Eco Green St, Silicon Valley",
                status = "In Progress",
                scheduledDate = now + (1 * dayMs)
            )
        )
    }

    fun schedulePickup(pickup: PickupModel, onResult: (ResultState<String>) -> Unit = {}) {
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

    fun completePickup(pickupId: String, userId: String = "", onResult: (ResultState<Boolean>) -> Unit) {
        if (pickupId.isEmpty()) {
            onResult(ResultState.Error(message = "Pickup ID is empty."))
            return
        }
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any>(
            "status" to "Completed",
            "completedAt" to now,
            "updatedAt" to now
        )
        if (userId.isNotEmpty()) {
            updates["userId"] = userId
        }
        firestoreService.db.collection("pickups").document(pickupId)
            .update(updates)
            .addOnSuccessListener {
                onResult(ResultState.Success(data = true))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun cancelPickup(
        pickupId: String,
        cancellationReason: String = "User requested cancellation",
        onResult: (ResultState<Boolean>) -> Unit
    ) {
        if (pickupId.isEmpty()) {
            onResult(ResultState.Error(message = "Pickup ID is empty."))
            return
        }
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "status" to "Cancelled",
            "cancelledAt" to now,
            "cancellationReason" to cancellationReason,
            "updatedAt" to now
        )
        firestoreService.db.collection("pickups").document(pickupId)
            .update(updates)
            .addOnSuccessListener {
                onResult(ResultState.Success(data = true))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }
}
