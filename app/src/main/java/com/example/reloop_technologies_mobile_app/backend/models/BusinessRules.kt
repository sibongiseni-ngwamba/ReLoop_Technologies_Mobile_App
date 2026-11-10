package com.example.reloop_technologies_mobile_app.backend.models

/** Pure domain rules shared by repositories and unit tests. */
object BusinessRules {
    fun validateRedemption(account: RewardAccount, item: RewardItem): String? = when {
        !item.isActive -> "This reward item is currently inactive or unavailable."
        item.pointsCost <= 0 -> "This reward item has an invalid points cost."
        item.stockQuantity != null && item.stockQuantity <= 0 -> "This reward item is currently out of stock."
        account.pointsBalance < 0 -> "The reward balance is invalid. Please refresh and try again."
        account.pointsBalance < item.pointsCost ->
            "Insufficient points balance! You need ${item.pointsCost - account.pointsBalance} more points."
        else -> null
    }

    fun canTransitionPickup(currentStatus: String, nextStatus: String): Boolean {
        val current = currentStatus.trim().lowercase()
        val next = nextStatus.trim().lowercase()
        return when (current) {
            "scheduled" -> next == "in progress" || next == "cancelled"
            "in progress" -> next == "completed"
            else -> false
        }
    }
}
