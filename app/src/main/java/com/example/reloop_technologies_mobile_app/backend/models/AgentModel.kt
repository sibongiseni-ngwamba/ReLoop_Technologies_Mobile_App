package com.example.reloop_technologies_mobile_app.backend.models

data class AgentModel(
    val agentId: String = "",
    val userId: String = "",
    val name: String = "",
    val contactNumber: String = "",
    val assignedZone: String = "",
    val isAvailable: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
