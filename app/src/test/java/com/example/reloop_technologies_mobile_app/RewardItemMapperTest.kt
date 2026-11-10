package com.example.reloop_technologies_mobile_app

import com.example.reloop_technologies_mobile_app.backend.models.RewardItemMapper
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class RewardItemMapperTest {
    @Test
    fun mapsFirestoreTimestampFieldsAndUsesDocumentId() {
        val createdAt = 1_700_000_000_000L
        val updatedAt = createdAt + 5_000L
        val item = RewardItemMapper.fromFields(
            "catalogue-doc-id",
            mapOf(
                "rewardItemId" to "stale-field-id",
                "itemName" to "Coffee voucher",
                "description" to "One drink",
                "pointsCost" to 500L,
                "isActive" to true,
                "stockQuantity" to 8L,
                "createdAt" to Timestamp(Date(createdAt)),
                "updatedAt" to Timestamp(Date(updatedAt))
            )
        )

        assertEquals("catalogue-doc-id", item.rewardItemId)
        assertEquals("Coffee voucher", item.itemName)
        assertEquals(500, item.pointsCost)
        assertEquals(8, item.stockQuantity)
        assertEquals(createdAt, item.createdAt)
        assertEquals(updatedAt, item.updatedAt)
    }

    @Test
    fun mapsLegacyNumericDatesAndOptionalFields() {
        val item = RewardItemMapper.fromFields(
            "legacy-item",
            mapOf("pointsCost" to 125, "isActive" to false, "createdAt" to 1234L)
        )

        assertEquals(1234L, item.createdAt)
        assertEquals(0L, item.updatedAt)
        assertFalse(item.isActive)
        assertTrue(item.stockQuantity == null)
    }
}
