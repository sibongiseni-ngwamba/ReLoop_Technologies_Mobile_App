package com.example.reloop_technologies_mobile_app

import com.example.reloop_technologies_mobile_app.database.UserData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UserManagerTest {

    @Test
    fun testUserDataModel_creationAndDefaults() {
        val userData = UserData(
            uid = "user_123",
            name = "Alex Morgan",
            email = "alex.morgan@example.com",
            phone = "+1 555-0192",
            address = "123 Eco Green St, Silicon Valley"
        )

        assertNotNull(userData)
        assertEquals("user_123", userData.uid)
        assertEquals("Alex Morgan", userData.name)
        assertEquals("alex.morgan@example.com", userData.email)
        assertEquals("+1 555-0192", userData.phone)
        assertEquals("123 Eco Green St, Silicon Valley", userData.address)
    }

    @Test
    fun testUserDataModel_emptyDefaults() {
        val emptyUserData = UserData()
        assertEquals("", emptyUserData.uid)
        assertEquals("", emptyUserData.name)
        assertEquals("", emptyUserData.email)
        assertEquals("", emptyUserData.phone)
        assertEquals("", emptyUserData.address)
    }
}
