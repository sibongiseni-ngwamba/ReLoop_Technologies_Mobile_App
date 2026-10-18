package com.example.reloop_technologies_mobile_app.backend.services

import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel
import com.example.reloop_technologies_mobile_app.backend.repositories.NotificationRepository
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ReLoopMessagingService : FirebaseMessagingService() {

    private val notificationRepository by lazy { NotificationRepository() }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "ReLoop Activity Alert"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "You have a new recycling update."

        val type = remoteMessage.data["type"] ?: NotificationModel.TYPE_INFO
        val pickupId = remoteMessage.data["relatedPickupId"]

        // Show local system notification
        NotificationHelper.showNotification(applicationContext, title, body)

        // Save into repository for current user if logged in
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: UserManager.getUserProfile(applicationContext).uid

        if (currentUid.isNotEmpty()) {
            val notification = NotificationModel(
                userId = currentUid,
                title = title,
                message = body,
                type = type,
                relatedPickupId = pickupId,
                sentAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                isRead = false,
            )
            notificationRepository.addNotification(notification)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: UserManager.getUserProfile(applicationContext).uid

        if (currentUid.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUid)
                .update("fcmToken", token)
        }
    }
}
