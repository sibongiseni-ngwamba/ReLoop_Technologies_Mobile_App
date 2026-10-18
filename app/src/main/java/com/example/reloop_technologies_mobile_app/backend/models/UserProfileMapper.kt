package com.example.reloop_technologies_mobile_app.backend.models

import com.example.reloop_technologies_mobile_app.database.UserData

/** Reads both the current profile fields and profiles saved by older versions of the app. */
object UserProfileMapper {
    fun fromFields(userId: String, fields: Map<String, Any?>, fallback: UserData = UserData()): UserModel {
        val legacyName = (fields["name"] as? String).orEmpty().trim()
        val nameParts = legacyName.split(Regex("\\s+"))
        val fallbackParts = fallback.name.trim().split(Regex("\\s+"))
        val currentFirst = (fields["firstName"] as? String)?.trim().orEmpty()
        val currentLast = (fields["lastName"] as? String)?.trim().orEmpty()
        val hasCurrentName = currentFirst.isNotEmpty() || currentLast.isNotEmpty()
        val firstName = if (hasCurrentName) currentFirst else if (legacyName.isNotEmpty()) nameParts.first() else fallbackParts.firstOrNull().orEmpty()
        val lastName = if (hasCurrentName) currentLast else if (legacyName.isNotEmpty()) nameParts.drop(1).joinToString(" ") else fallbackParts.drop(1).joinToString(" ")
        return UserModel(
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            email = (fields["email"] as? String).orEmpty().ifEmpty { fallback.email },
            phoneNumber = (fields["phoneNumber"] as? String).orEmpty()
                .ifEmpty { (fields["phone"] as? String).orEmpty().ifEmpty { fallback.phone } },
            address = (fields["address"] as? String).orEmpty().ifEmpty { fallback.address },
            role = (fields["role"] as? String).orEmpty().ifEmpty { "resident" },
            isActive = fields["isActive"] as? Boolean ?: true,
            createdAt = (fields["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (fields["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            profileImageUrl = fields["profileImageUrl"] as? String
        )
    }
}
