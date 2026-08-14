package com.securewol.app

import com.securewol.app.core.security.SessionManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    @Before
    fun setUp() {
        SessionManager.invalidateSession()
    }

    @Test
    fun testInitialState_unauthenticated() {
        assertFalse(SessionManager.isSessionValid())
        assertFalse(SessionManager.isAuthenticated.value)
    }

    @Test
    fun testCreateSession_becomesAuthenticated() {
        val token = SessionManager.createSession()
        assertNotNull(token)
        assertTrue(SessionManager.isSessionValid())
        assertTrue(SessionManager.isAuthenticated.value)

        // validateSessionOrThrow should succeed
        val validatedToken = SessionManager.validateSessionOrThrow()
        assertEquals(token.id, validatedToken.id)
    }

    @Test(expected = SecurityException::class)
    fun testValidateSessionOrThrow_throwsWhenInvalidated() {
        SessionManager.createSession()
        SessionManager.invalidateSession()

        assertFalse(SessionManager.isSessionValid())
        SessionManager.validateSessionOrThrow()
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
