package com.example.reloop_technologies_mobile_app.backend.models

data class PickupModel(
    val pickupId: String = "",
    val userId: String = "",
    val agentId: String? = null,
    val scheduledDate: Long = System.currentTimeMillis(),
    val wasteType: String = "",
    val wasteTypes: List<String> = emptyList(),
    val address: String = "",
    val status: String = "Scheduled", // Scheduled, In Progress, Completed, Cancelled
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null
)
