package com.example.reloop_technologies_mobile_app.backend.repositories

import android.content.Context
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService
import com.example.reloop_technologies_mobile_app.database.UserData
import com.example.reloop_technologies_mobile_app.database.UserManager

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
                if (document.exists()) {
                    val user = document.toObject(UserModel::class.java) ?: UserModel(userId = userId)
                    context?.let { ctx ->
                        UserManager.saveUserProfile(
                            ctx,
                            UserData(
                                uid = user.userId,
                                name = user.fullName,
                                email = user.email,
                                phone = user.phoneNumber,
                                address = user.address
                            )
                        )
                    }
                    onResult(ResultState.Success(user))
                } else {
                    val fallbackData = context?.let { UserManager.getUserProfile(it) }
                    val nameParts = (fallbackData?.name ?: "Resident").split(" ")
                    val first = nameParts.firstOrNull() ?: "Resident"
                    val last = if (nameParts.size > 1) nameParts.subList(1, nameParts.size).joinToString(" ") else ""
                    val defaultUser = UserModel(
                        userId = userId,
                        firstName = first,
                        lastName = last,
                        email = fallbackData?.email ?: "",
                        phoneNumber = fallbackData?.phone ?: "",
                        address = fallbackData?.address ?: ""
                    )
                    onResult(ResultState.Success(defaultUser))
                }
            }
            .addOnFailureListener {
                val fallbackData = context?.let { UserManager.getUserProfile(it) }
                val nameParts = (fallbackData?.name ?: "Resident").split(" ")
                val first = nameParts.firstOrNull() ?: "Resident"
                val last = if (nameParts.size > 1) nameParts.subList(1, nameParts.size).joinToString(" ") else ""
                val defaultUser = UserModel(
                    userId = userId,
                    firstName = first,
                    lastName = last,
                    email = fallbackData?.email ?: "",
                    phoneNumber = fallbackData?.phone ?: "",
                    address = fallbackData?.address ?: ""
                )
                onResult(ResultState.Success(defaultUser))
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

        context?.let { ctx ->
            UserManager.saveUserProfile(
                ctx,
                UserData(
                    uid = user.userId,
                    name = user.fullName,
                    email = user.email,
                    phone = user.phoneNumber,
                    address = user.address
                )
            )
        }

        firestoreService.db.collection("users").document(user.userId).set(user)
            .addOnSuccessListener {
                onResult(ResultState.Success(true))
            }
            .addOnFailureListener {
                onResult(ResultState.Success(true))
            }
    }
}
