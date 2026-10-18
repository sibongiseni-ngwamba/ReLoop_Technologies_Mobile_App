package com.example.reloop_technologies_mobile_app.backend.services

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class FirebaseAuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun isUserSignedIn(): Boolean = currentUser != null

    fun getUserId(): String = currentUser?.uid ?: ""

    fun signIn(email: String, password: String, onResult: (ResultState<FirebaseUser>) -> Unit) {
        onResult(ResultState.Loading)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    onResult(ResultState.Success(user))
                } else {
                    onResult(ResultState.Error(message = "Authentication succeeded but user session is null."))
                }
            }
            .addOnFailureListener { exception ->
                val msg = exception.localizedMessage ?: "Failed to sign in. Please check credentials."
                onResult(ResultState.Error(exception = exception, message = msg))
            }
    }

    fun signUp(email: String, password: String, displayName: String, onResult: (ResultState<FirebaseUser>) -> Unit) {
        onResult(ResultState.Loading)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates)
                    onResult(ResultState.Success(user))
                } else {
                    onResult(ResultState.Error(message = "Account created but user session is null."))
                }
            }
            .addOnFailureListener { exception ->
                val msg = exception.localizedMessage ?: "Failed to create account."
                onResult(ResultState.Error(exception = exception, message = msg))
            }
    }

    fun updatePassword(newPassword: String, onResult: (ResultState<Boolean>) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(ResultState.Error(message = "No authenticated user session found."))
            return
        }
        onResult(ResultState.Loading)
        user.updatePassword(newPassword)
            .addOnSuccessListener {
                onResult(ResultState.Success(true))
            }
            .addOnFailureListener { exception ->
                val msg = exception.localizedMessage ?: "Failed to update password. Re-authentication may be required."
                onResult(ResultState.Error(exception = exception, message = msg))
            }
    }

    fun sendPasswordResetEmail(email: String, onResult: (ResultState<Boolean>) -> Unit) {
        onResult(ResultState.Loading)
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onResult(ResultState.Success(true))
            }
            .addOnFailureListener { exception ->
                val msg = exception.localizedMessage ?: "Failed to send password reset email."
                onResult(ResultState.Error(exception = exception, message = msg))
            }
    }

    fun signOut() {
        auth.signOut()
    }
}
