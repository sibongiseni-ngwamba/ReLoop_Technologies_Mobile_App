package com.example.reloop_technologies_mobile_app.backend.models

data class WasteLog(
    val wasteLogId: String = "",
    val userId: String = "",
    val pickupId: String? = null,
    val wasteCategoryId: String = "",
    val weightKg: Double = 0.2,
    val loggedAt: Long = System.currentTimeMillis(),
    val source: String = "Mobile AI Scan",
    val modelLabel: String? = "Plastic Bottle",
    val confidenceScore: Double? = 0.95,
    val imageUrl: String? = null
)

data class WasteCategory(
    val wasteCategoryId: String = "",
    val categoryName: String = "",
    val description: String = "",
    val modelLabel: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class GuidanceContent(
    val guidanceId: String = "",
    val wasteCategoryId: String = "",
    val title: String = "",
    val content: String = "",
    val imagePath: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
