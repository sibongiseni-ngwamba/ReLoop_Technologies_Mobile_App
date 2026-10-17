package com.example.reloop_technologies_mobile_app.backend.models

data class NotificationModel(
    val notificationId: String = "",
    val userId: String = "",
    val message: String = "",
    val type: String = "info",
    val sentAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedPickupId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
