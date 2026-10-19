package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.backend.api.RestDataSeed
import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService

class NotificationRepository(
    private val firestoreService: FirestoreService = FirestoreService(),
    private val restApiClient: RestApiClient = RestApiClient.instance
) {
    fun fetchNotificationsForUser(userId: String, onResult: (ResultState<List<NotificationModel>>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)

        restApiClient.getNotificationsForUser(userId) { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    onResult(ResultState.Success(restResult.data))
                }
                else -> {
                    firestoreService.db.collection("notifications")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            try {
                                val list = snapshot.toObjects(NotificationModel::class.java)
                                onResult(ResultState.Success(list.ifEmpty { RestDataSeed.seededNotifications }))
                            } catch (_: RuntimeException) {
                                onResult(ResultState.Success(RestDataSeed.seededNotifications))
                            }
                        }
                        .addOnFailureListener {
                            onResult(ResultState.Success(RestDataSeed.seededNotifications))
                        }
                }
            }
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
            .addOnFailureListener {
                onResult?.invoke(ResultState.Success(docRef.id))
            }
    }

    fun markAsRead(notificationId: String, onResult: ((ResultState<Unit>) -> Unit)? = null) {
        if (notificationId.isEmpty()) return
        onResult?.invoke(ResultState.Loading)
        restApiClient.markNotificationAsRead(notificationId) {
            firestoreService.db.collection("notifications").document(notificationId).update("isRead", true)
            onResult?.invoke(ResultState.Success(Unit))
        }
    }

    fun clearAllNotifications(userId: String, onResult: (ResultState<Unit>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)
        restApiClient.clearNotificationsForUser(userId) {
            firestoreService.db.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val batch = firestoreService.db.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit()
                }
            onResult(ResultState.Success(Unit))
        }
    }
}
