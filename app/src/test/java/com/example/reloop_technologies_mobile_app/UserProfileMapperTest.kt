package com.example.reloop_technologies_mobile_app

import com.example.reloop_technologies_mobile_app.backend.models.UserProfileMapper
import com.example.reloop_technologies_mobile_app.database.UserData
import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileMapperTest {
    @Test
    fun legacyProfileUsesDocumentIdAndLegacyFields() {
        val user = UserProfileMapper.fromFields(
            "auth-uid",
            mapOf("uid" to "wrong-id", "name" to "Lebo Mokoena", "email" to "lebo@example.com", "phone" to "0712345678")
        )

        assertEquals("auth-uid", user.userId)
        assertEquals("Lebo", user.firstName)
        assertEquals("Mokoena", user.lastName)
        assertEquals("0712345678", user.phoneNumber)
    }

    @Test
    fun currentFieldsTakePriorityOverLegacyAndUseScopedFallback() {
        val user = UserProfileMapper.fromFields(
            "auth-uid",
            mapOf("name" to "Old Name", "firstName" to "New", "lastName" to "Name", "phoneNumber" to "0820000000"),
            UserData(uid = "auth-uid", email = "new@example.com", address = "Cape Town")
        )

        assertEquals("New Name", user.fullName)
        assertEquals("0820000000", user.phoneNumber)
        assertEquals("new@example.com", user.email)
        assertEquals("Cape Town", user.address)
    }
}
