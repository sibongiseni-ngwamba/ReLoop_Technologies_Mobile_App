package com.example.reloop_technologies_mobile_app

import com.example.reloop_technologies_mobile_app.backend.models.BusinessRules
import com.example.reloop_technologies_mobile_app.backend.models.RewardAccount
import com.example.reloop_technologies_mobile_app.backend.models.RewardItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessRulesTest {
    @Test
    fun redemptionRequiresActiveItemPositiveCostAndAvailableStock() {
        val account = RewardAccount(userId = "u1", pointsBalance = 500)
        assertNull(BusinessRules.validateRedemption(account, RewardItem(pointsCost = 500)))
        assertEquals("This reward item has an invalid points cost.", BusinessRules.validateRedemption(account, RewardItem(pointsCost = 0)))
        assertEquals("This reward item is currently inactive or unavailable.", BusinessRules.validateRedemption(account, RewardItem(pointsCost = 100, isActive = false)))
        assertEquals("This reward item is currently out of stock.", BusinessRules.validateRedemption(account, RewardItem(pointsCost = 100, stockQuantity = 0)))
    }

    @Test
    fun redemptionRejectsInvalidAndInsufficientBalances() {
        assertEquals(
            "The reward balance is invalid. Please refresh and try again.",
            BusinessRules.validateRedemption(RewardAccount(pointsBalance = -1), RewardItem(pointsCost = 1))
        )
        assertEquals(
            "Insufficient points balance! You need 1 more points.",
            BusinessRules.validateRedemption(RewardAccount(pointsBalance = 99), RewardItem(pointsCost = 100))
        )
    }

    @Test
    fun pickupTransitionsDoNotReopenTerminalRequests() {
        assertTrue(BusinessRules.canTransitionPickup("Scheduled", "In Progress"))
        assertTrue(BusinessRules.canTransitionPickup("scheduled", "cancelled"))
        assertTrue(BusinessRules.canTransitionPickup("In Progress", "Completed"))
        assertFalse(BusinessRules.canTransitionPickup("Completed", "Cancelled"))
        assertFalse(BusinessRules.canTransitionPickup("Cancelled", "Completed"))
        assertFalse(BusinessRules.canTransitionPickup("Scheduled", "Completed"))
    }
}
