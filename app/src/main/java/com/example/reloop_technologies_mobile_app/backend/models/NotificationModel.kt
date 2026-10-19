package com.example.reloop_technologies_mobile_app.backend.models

data class NotificationModel(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = TYPE_INFO,
    val sentAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedPickupId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_PICKUP_CONFIRMATION = "PICKUP_CONFIRMATION"
        const val TYPE_PICKUP_REMINDER = "PICKUP_REMINDER"
        const val TYPE_STATUS_CHANGE = "STATUS_CHANGE"
        const val TYPE_REWARD = "REWARD"
        const val TYPE_INFO = "INFO"
    }
}
