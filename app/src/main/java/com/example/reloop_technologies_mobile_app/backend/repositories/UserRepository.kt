package com.example.reloop_technologies_mobile_app.backend.repositories

import android.content.Context
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.backend.models.UserProfileMapper
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService
import com.example.reloop_technologies_mobile_app.database.UserData
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.google.firebase.firestore.SetOptions

class UserRepository(
    private val firestoreService: FirestoreService = FirestoreService()
) {
    fun fetchUserProfile(
        userId: String,
        context: Context? = null,
        onResult: (ResultState<UserModel>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)
        firestoreService.db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val fallback = context?.let { UserManager.getUserProfile(it, userId) } ?: UserData(uid = userId)
                val user = UserProfileMapper.fromFields(userId, document.data ?: emptyMap(), fallback)
                context?.let { ctx ->
                    UserManager.saveUserProfile(ctx, UserData(userId, user.fullName, user.email, user.phoneNumber, user.address))
                }
                onResult(ResultState.Success(user))
            }
            .addOnFailureListener {
                val fallback = context?.let { UserManager.getUserProfile(it, userId) } ?: UserData(uid = userId)
                onResult(ResultState.Success(UserProfileMapper.fromFields(userId, emptyMap(), fallback)))
            }
    }

    fun saveUserProfile(
        user: UserModel,
        context: Context? = null,
        onResult: (ResultState<Boolean>) -> Unit
    ) {
        if (user.userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)

        firestoreService.db.collection("users").document(user.userId).set(user, SetOptions.merge())
            .addOnSuccessListener {
                context?.let { ctx ->
                    UserManager.saveUserProfile(ctx, UserData(user.userId, user.fullName, user.email, user.phoneNumber, user.address))
                }
                onResult(ResultState.Success(true))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }
}
