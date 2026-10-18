package com.example.reloop_technologies_mobile_app.backend.services

import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings

class FirestoreService(
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    init {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
        } catch (e: Exception) {
            // Firestore settings already set or exception ignored safely
        }
    }

    fun mapError(exception: Exception?): String {
        if (exception == null) return "An unknown error occurred. Please try again."

        val message = exception.message ?: ""

        if (exception is FirebaseFirestoreException) {
            return when (exception.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE -> "Network connection unavailable. Operating in offline cached mode."
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED, FirebaseFirestoreException.Code.CANCELLED -> "The request timed out. Please check your internet connection and retry."
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Access denied or session expired. Please log in again."
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> "User session expired. Please re-authenticate."
                FirebaseFirestoreException.Code.ALREADY_EXISTS -> "This record already exists."
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> "Server quota exceeded. Please try again later."
                else -> exception.localizedMessage ?: "Database operation failed."
            }
        }

        return when {
            message.contains("UNAVAILABLE", ignoreCase = true) -> "Network connection unavailable. Operating in offline cached mode."
            message.contains("PERMISSION_DENIED", ignoreCase = true) -> "Access denied or session expired. Please log in again."
            message.contains("network", ignoreCase = true) || message.contains("connection", ignoreCase = true) -> "Network error. Please check your internet connection and try again."
            else -> exception.localizedMessage ?: "Operation failed. Please try again."
        }
    }
}
