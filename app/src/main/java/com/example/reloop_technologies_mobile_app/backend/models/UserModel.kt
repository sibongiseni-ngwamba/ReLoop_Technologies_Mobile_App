package com.example.reloop_technologies_mobile_app.backend.models

data class UserModel(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val role: String = "resident",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val profileImageUrl: String? = null
) {
    val fullName: String
        get() = "$firstName $lastName".trim().ifEmpty { "Resident" }
}
