package com.example.reloop_technologies_mobile_app.backend.repositories

import android.content.Context
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.database.UserData
import com.example.reloop_technologies_mobile_app.database.UserManager

class UserRepository(private val restApiClient: RestApiClient = RestApiClient.instance) {
    fun fetchUserProfile(userId: String, context: Context? = null, onResult: (ResultState<UserModel>) -> Unit) {
        if (userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.getUserProfile(userId) { result ->
            if (result is ResultState.Success) {
                val user = result.data
                context?.let { UserManager.saveUserProfile(it, UserData(user.userId, user.fullName, user.email, user.phoneNumber, user.address, user.role)) }
                onResult(result)
            } else if (result is ResultState.Error && context != null) {
                val cached = UserManager.getUserProfile(context, userId)
                onResult(ResultState.Success(UserModel(
                    userId = userId,
                    firstName = cached.name.trim().split(Regex("\\s+"), limit = 2).firstOrNull().orEmpty(),
                    lastName = cached.name.trim().split(Regex("\\s+"), limit = 2).getOrElse(1) { "" },
                    email = cached.email,
                    phoneNumber = cached.phone,
                    address = cached.address,
                    role = cached.role
                )))
            } else onResult(result)
        }
    }

    fun saveUserProfile(user: UserModel, context: Context? = null, onResult: (ResultState<Boolean>) -> Unit) {
        if (user.userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.updateUserProfile(user) { result ->
            if (result is ResultState.Success) context?.let {
                UserManager.saveUserProfile(it, UserData(user.userId, user.fullName, user.email, user.phoneNumber, user.address, user.role))
            }
            onResult(result)
        }
    }
}
