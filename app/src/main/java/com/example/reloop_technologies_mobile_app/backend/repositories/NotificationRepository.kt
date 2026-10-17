package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService

class NotificationRepository(
    private val firestoreService: FirestoreService = FirestoreService()
) {
    fun fetchNotificationsForUser(userId: String, onResult: (ResultState<List<NotificationModel>>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)
        firestoreService.db.collection("notifications")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.toObjects(NotificationModel::class.java)
                onResult(ResultState.Success(list))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }
}
