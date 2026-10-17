package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService

class UserRepository(
    private val firestoreService: FirestoreService = FirestoreService()
) {
    fun fetchUserProfile(userId: String, onResult: (ResultState<UserModel>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)
        firestoreService.db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(UserModel::class.java) ?: UserModel(userId = userId)
                    onResult(ResultState.Success(user))
                } else {
                    onResult(ResultState.Success(UserModel(userId = userId)))
                }
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun saveUserProfile(user: UserModel, onResult: (ResultState<Boolean>) -> Unit) {
        if (user.userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)
        firestoreService.db.collection("users").document(user.userId).set(user)
            .addOnSuccessListener {
                onResult(ResultState.Success(true))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }
}
