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
                    .sortedByDescending { it.createdAt.coerceAtLeast(it.sentAt) }
                onResult(ResultState.Success(list))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun addNotification(notification: NotificationModel, onResult: ((ResultState<String>) -> Unit)? = null) {
        onResult?.invoke(ResultState.Loading)
        val docRef = if (notification.notificationId.isNotEmpty()) {
            firestoreService.db.collection("notifications").document(notification.notificationId)
        } else {
            firestoreService.db.collection("notifications").document()
        }

        val itemToSave = notification.copy(
            notificationId = docRef.id,
            createdAt = if (notification.createdAt == 0L) System.currentTimeMillis() else notification.createdAt,
            sentAt = if (notification.sentAt == 0L) System.currentTimeMillis() else notification.sentAt
        )

        docRef.set(itemToSave)
            .addOnSuccessListener {
                onResult?.invoke(ResultState.Success(docRef.id))
            }
            .addOnFailureListener { exception ->
                onResult?.invoke(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun markAsRead(notificationId: String, onResult: ((ResultState<Unit>) -> Unit)? = null) {
        if (notificationId.isEmpty()) return
        onResult?.invoke(ResultState.Loading)
        firestoreService.db.collection("notifications").document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                onResult?.invoke(ResultState.Success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult?.invoke(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun clearAllNotifications(userId: String, onResult: (ResultState<Unit>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)
        firestoreService.db.collection("notifications")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestoreService.db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        onResult(ResultState.Success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
                    }
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }
}
