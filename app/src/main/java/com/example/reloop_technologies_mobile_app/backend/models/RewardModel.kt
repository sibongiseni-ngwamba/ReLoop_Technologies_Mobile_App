package com.example.reloop_technologies_mobile_app.backend.models

data class RewardAccount(
    val userId: String = "",
    val pointsBalance: Int = 1200,
    val totalEarned: Int = 1200,
    val totalRedeemed: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class RewardItem(
    val rewardItemId: String = "",
    val itemName: String = "",
    val description: String = "",
    val pointsCost: Int = 0,
    val isActive: Boolean = true,
    val imageUrl: String? = null,
    val stockQuantity: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class RedemptionLog(
    val redemptionId: String = "",
    val userId: String = "",
    val rewardItemId: String = "",
    val pointsUsed: Int = 0,
    val voucherCode: String? = null,
    val status: String = "Redeemed",
    val redeemedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

data class PointsTransaction(
    val transactionId: String = "",
    val userId: String = "",
    val transactionType: String = "earn", // earn, redeem, adjust
    val points: Int = 0,
    val relatedId: String? = null,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
