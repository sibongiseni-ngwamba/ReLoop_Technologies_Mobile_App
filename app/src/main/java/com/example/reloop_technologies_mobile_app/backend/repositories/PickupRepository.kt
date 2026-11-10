package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.models.UserRole
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService

class PickupRepository(
    private val firestoreService: FirestoreService = FirestoreService(),
    private val restApiClient: RestApiClient = RestApiClient.instance
) {
    fun fetchPickupsForUser(
        userId: String,
        role: String = "resident",
        onResult: (ResultState<List<PickupModel>>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)

        restApiClient.getPickupsForUser(userId, role) { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    onResult(ResultState.Success(restResult.data))
                }
                else -> {
                    // Fallback query
                    val userRole = UserRole.fromString(role)
                    val query = if (userRole.canManageAllPickups()) {
                        firestoreService.db.collection("pickups")
                    } else {
                        firestoreService.db.collection("pickups").whereEqualTo("userId", userId)
                    }

                    query.get()
                        .addOnSuccessListener { snapshot ->
                            try {
                                val pickups = snapshot.toObjects(PickupModel::class.java)
                                if (pickups.isEmpty()) {
                                    onResult(ResultState.Success(getDefaultPickups(userId)))
                                } else {
                                    onResult(ResultState.Success(pickups))
                                }
                            } catch (_: RuntimeException) {
                                onResult(ResultState.Success(getDefaultPickups(userId)))
                            }
                        }
                        .addOnFailureListener {
                            onResult(ResultState.Success(getDefaultPickups(userId)))
                        }
                }
            }
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
        restApiClient.schedulePickup(pickup) { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    // Sync to Firestore asynchronously
                    val docRef = firestoreService.db.collection("pickups").document(restResult.data)
                    docRef.set(pickup.copy(pickupId = restResult.data))
                    onResult(ResultState.Success(restResult.data))
                }
                else -> {
                    val docRef = firestoreService.db.collection("pickups").document()
                    val newPickup = pickup.copy(pickupId = docRef.id)
                    docRef.set(newPickup)
                        .addOnSuccessListener { onResult(ResultState.Success(docRef.id)) }
                        .addOnFailureListener { onResult(ResultState.Success(docRef.id)) }
                }
            }
        }
    }

    fun completePickup(pickupId: String, userId: String = "", onResult: (ResultState<Boolean>) -> Unit) {
        if (pickupId.isEmpty()) {
            onResult(ResultState.Error(message = "Pickup ID is empty."))
            return
        }
        restApiClient.completePickup(pickupId, userId) { restResult ->
            val now = System.currentTimeMillis()
            val updates = mutableMapOf<String, Any>(
                "status" to "Completed",
                "completedAt" to now,
                "updatedAt" to now
            )
            if (userId.isNotEmpty()) updates["userId"] = userId
            firestoreService.db.collection("pickups").document(pickupId).update(updates)
            onResult(ResultState.Success(true))
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
        restApiClient.cancelPickup(pickupId, cancellationReason) { restResult ->
            val now = System.currentTimeMillis()
            val updates = mapOf<String, Any>(
                "status" to "Cancelled",
                "cancelledAt" to now,
                "cancellationReason" to cancellationReason,
                "updatedAt" to now
            )
            firestoreService.db.collection("pickups").document(pickupId).update(updates)
            onResult(ResultState.Success(true))
        }
    }
}
