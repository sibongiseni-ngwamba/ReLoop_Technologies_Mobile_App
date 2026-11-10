package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel

class NotificationRepository(private val restApiClient: RestApiClient = RestApiClient.instance) {
    fun fetchNotificationsForUser(userId: String, onResult: (ResultState<List<NotificationModel>>) -> Unit) {
        if (userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.getNotificationsForUser(userId, onResult)
    }

    fun addNotification(notification: NotificationModel, onResult: ((ResultState<String>) -> Unit)? = null) =
        restApiClient.addNotification(notification, onResult)

    fun markAsRead(notificationId: String, onResult: ((ResultState<Unit>) -> Unit)? = null) {
        if (notificationId.isBlank()) return
        restApiClient.markNotificationAsRead(notificationId) { result ->
            onResult?.invoke(when (result) {
                is ResultState.Success -> ResultState.Success(Unit)
                is ResultState.Error -> ResultState.Error(result.exception, result.message)
                ResultState.Loading -> ResultState.Loading
            })
        }
    }

    fun clearAllNotifications(userId: String, onResult: (ResultState<Unit>) -> Unit) {
        if (userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.clearNotificationsForUser(userId) { result ->
            onResult(when (result) {
                is ResultState.Success -> ResultState.Success(Unit)
                is ResultState.Error -> ResultState.Error(result.exception, result.message)
                ResultState.Loading -> ResultState.Loading
            })
        }
    }
}
