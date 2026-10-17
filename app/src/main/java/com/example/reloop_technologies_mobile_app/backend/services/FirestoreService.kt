package com.example.reloop_technologies_mobile_app.backend.services

import com.example.reloop_technologies_mobile_app.backend.BackendConfig
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class FirestoreService(
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun <T> executeTaskWithTimeout(
        taskName: String,
        action: () -> Unit
    ) {
        action()
    }

    fun mapError(exception: Exception?): String {
        return when {
            exception == null -> "An unknown error occurred."
            exception.message?.contains("UNAVAILABLE") == true -> "Network connection unavailable. Operating in cached mode."
            exception.message?.contains("PERMISSION_DENIED") == true -> "Access denied. Insufficient permissions."
            else -> exception.localizedMessage ?: "Database operation failed."
        }
    }
}
