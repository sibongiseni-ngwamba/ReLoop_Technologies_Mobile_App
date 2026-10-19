package com.example.reloop_technologies_mobile_app

import com.example.reloop_technologies_mobile_app.backend.models.AgentModel
import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.models.RedemptionLog
import com.example.reloop_technologies_mobile_app.backend.models.RewardAccount
import com.example.reloop_technologies_mobile_app.backend.models.RewardItem
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.backend.models.WasteLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test
    fun testNotificationModel() {
        val notification = NotificationModel(
            notificationId = "notif_001",
            userId = "usr_100",
            message = "Your pickup is scheduled for tomorrow",
            type = "PICKUP_CONFIRMATION",
            isRead = false
        )

        assertEquals("notif_001", notification.notificationId)
        assertEquals("usr_100", notification.userId)
        assertEquals("Your pickup is scheduled for tomorrow", notification.message)
        assertEquals("PICKUP_CONFIRMATION", notification.type)
        assertFalse(notification.isRead)
    }

    @Test
    fun testPickupModel() {
        val pickup = PickupModel(
            pickupId = "pick_001",
            userId = "usr_100",
            address = "123 Green Ave",
            wasteType = "Plastic",
            status = "Scheduled"
        )

        assertEquals("pick_001", pickup.pickupId)
        assertEquals("usr_100", pickup.userId)
        assertEquals("123 Green Ave", pickup.address)
        assertEquals("Plastic", pickup.wasteType)
        assertEquals("Scheduled", pickup.status)
    }

    @Test
    fun testRewardModels() {
        val account = RewardAccount(userId = "usr_100", pointsBalance = 1500)
        assertEquals(1500, account.pointsBalance)

        val item = RewardItem(
            rewardItemId = "item_1",
            itemName = "$5 Coffee Voucher",
            pointsCost = 500,
            isActive = true
        )
        assertEquals(500, item.pointsCost)
        assertTrue(item.isActive)

        val redemption = RedemptionLog(
            redemptionId = "red_01",
            userId = "usr_100",
            pointsUsed = 500,
            voucherCode = "$5 Coffee Voucher",
            status = "Redeemed"
        )
        assertEquals(500, redemption.pointsUsed)
        assertEquals("$5 Coffee Voucher", redemption.voucherCode)
        assertEquals("Redeemed", redemption.status)
    }

    @Test
    fun testUserModelAndWasteLog() {
        val user = UserModel(
            userId = "usr_100",
            email = "user@test.com",
            firstName = "John",
            lastName = "Doe"
        )
        assertEquals("usr_100", user.userId)
        assertEquals("John Doe", user.fullName)

        val wasteLog = WasteLog(
            wasteLogId = "log_01",
            userId = "usr_100",
            weightKg = 0.5,
            modelLabel = "Plastic Bottle",
            confidenceScore = 0.95
        )
        assertEquals(0.5, wasteLog.weightKg, 0.001)
        assertEquals("Plastic Bottle", wasteLog.modelLabel)
        assertEquals(0.95, wasteLog.confidenceScore ?: 0.0, 0.001)
    }

    @Test
    fun testAgentModel() {
        val agent = AgentModel(
            agentId = "ag_001",
            userId = "usr_agent_100",
            name = "Sipho Dlamini",
            contactNumber = "0821234567",
            assignedZone = "Zone A",
            isAvailable = true,
            isActive = true
        )
        assertEquals("ag_001", agent.agentId)
        assertEquals("usr_agent_100", agent.userId)
        assertEquals("Sipho Dlamini", agent.name)
        assertEquals("0821234567", agent.contactNumber)
        assertEquals("Zone A", agent.assignedZone)
        assertTrue(agent.isAvailable)
        assertTrue(agent.isActive)
    }
}
