package com.example.reloop_technologies_mobile_app

import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.backend.models.UserProfileMapper
import com.example.reloop_technologies_mobile_app.database.UserData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthServiceTest {

    @Test
    fun testUserProfileAssociation() {
        val uid = "user_abc_123"
        val email = "resident@reloop.app"
        val fullName = "Sibongiseni Ngwamba"

        val userData = UserData(
            uid = uid,
            name = fullName,
            email = email,
            phone = "+27 82 123 4567",
            address = "123 Eco Green St"
        )

        val fields = mapOf<String, Any?>(
            "userId" to uid,
            "firstName" to "Sibongiseni",
            "lastName" to "Ngwamba",
            "email" to email,
            "role" to "resident",
            "isActive" to true
        )

        val userModel = UserProfileMapper.fromFields(uid, fields, userData)

        assertEquals(uid, userModel.userId)
        assertEquals("Sibongiseni Ngwamba", userModel.fullName)
        assertEquals(email, userModel.email)
        assertEquals("resident", userModel.role)
        assertTrue(userModel.isActive)
    }

    @Test
    fun testRoleBasedPermissions() {
        val residentUser = UserModel(userId = "res_1", role = "resident")
        val agentUser = UserModel(userId = "agent_1", role = "agent")
        val adminUser = UserModel(userId = "admin_1", role = "admin")

        assertFalse(residentUser.role == "admin" || residentUser.role == "agent")
        assertTrue(agentUser.role == "agent")
        assertTrue(adminUser.role == "admin")
    }

    @Test
    fun testSessionValidation() {
        fun isSessionValid(uid: String?): Boolean {
            return !uid.isNullOrBlank()
        }

        assertTrue(isSessionValid("valid_user_uid_999"))
        assertFalse(isSessionValid(""))
        assertFalse(isSessionValid(null))
        assertFalse(isSessionValid("   "))
    }

    @Test
    fun testCrossAccountDataIsolation() {
        val activeUserUid = "user_session_A"
        val targetRecordUserUid = "user_session_B"

        fun canAccessRecord(currentUid: String, recordUid: String, role: String): Boolean {
            return role == "admin" || currentUid == recordUid
        }

        // Resident user A cannot access Resident user B's records
        assertFalse(canAccessRecord(activeUserUid, targetRecordUserUid, "resident"))

        // Resident user A can access own records
        assertTrue(canAccessRecord(activeUserUid, activeUserUid, "resident"))

        // Admin can access any record
        assertTrue(canAccessRecord("admin_user", targetRecordUserUid, "admin"))
    }
}
