package com.example.reloop_technologies_mobile_app.backend.models

import com.google.firebase.Timestamp
import java.util.Date

/** Converts Firestore reward documents without assuming how their date fields were encoded. */
object RewardItemMapper {
    fun fromFields(documentId: String, fields: Map<String, Any?>): RewardItem = RewardItem(
        rewardItemId = documentId,
        itemName = fields["itemName"] as? String ?: "",
        description = fields["description"] as? String ?: "",
        pointsCost = (fields["pointsCost"] as? Number)?.toInt() ?: 0,
        isActive = fields["isActive"] as? Boolean ?: true,
        imageUrl = fields["imageUrl"] as? String,
        stockQuantity = (fields["stockQuantity"] as? Number)?.toInt(),
        createdAt = fields["createdAt"].toEpochMillis() ?: 0L,
        updatedAt = fields["updatedAt"].toEpochMillis() ?: 0L
    )

    private fun Any?.toEpochMillis(): Long? = when (this) {
        is Timestamp -> toDate().time
        is Date -> time
        is Number -> toLong()
        else -> null
    }
}
