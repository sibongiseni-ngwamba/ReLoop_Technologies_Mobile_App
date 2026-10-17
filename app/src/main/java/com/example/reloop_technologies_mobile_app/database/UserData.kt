package com.example.reloop_technologies_mobile_app.database

data class UserData(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
