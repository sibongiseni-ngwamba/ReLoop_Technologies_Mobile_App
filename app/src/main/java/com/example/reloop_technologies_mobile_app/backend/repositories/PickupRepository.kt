package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel

class PickupRepository(private val restApiClient: RestApiClient = RestApiClient.instance) {
    fun fetchPickupsForUser(userId: String, role: String = "resident", onResult: (ResultState<List<PickupModel>>) -> Unit) {
        if (userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.getPickupsForUser(userId, role, onResult)
    }

    fun schedulePickup(pickup: PickupModel, onResult: (ResultState<String>) -> Unit = {}) {
        if (pickup.userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.schedulePickup(pickup, onResult)
    }

    fun completePickup(pickupId: String, userId: String = "", onResult: (ResultState<Boolean>) -> Unit) {
        if (pickupId.isBlank()) return onResult(ResultState.Error(message = "Pickup ID is empty."))
        restApiClient.completePickup(pickupId, userId, onResult)
    }

    fun cancelPickup(pickupId: String, cancellationReason: String = "User requested cancellation", onResult: (ResultState<Boolean>) -> Unit) {
        if (pickupId.isBlank()) return onResult(ResultState.Error(message = "Pickup ID is empty."))
        restApiClient.cancelPickup(pickupId, cancellationReason, onResult)
    }

    fun setPickupStatus(pickupId: String, status: String, onResult: (ResultState<Boolean>) -> Unit) =
        restApiClient.setPickupStatus(pickupId, status, onResult)
}
