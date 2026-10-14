package com.pulse.budget.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests for login/registration and money input validation. */
class ValidationTest {

    @Test
    fun `username is normalised and mapped to a private login address`() {
        assertEquals("sufyaan", AuthValidation.normalizeUsername("  Sufyaan "))
        assertEquals("sufyaan@${AuthValidation.USERNAME_EMAIL_DOMAIN}", AuthValidation.usernameToEmail("Sufyaan"))
    }

    @Test
    fun `username rules`() {
        assertNull(AuthValidation.validateUsername("pulse_user.1"))
        assertNotNull(AuthValidation.validateUsername("ab"))
        assertNotNull(AuthValidation.validateUsername("has space"))
        assertNotNull(AuthValidation.validateUsername("me@mail"))
        assertNotNull(AuthValidation.validateUsername("a".repeat(21)))
    }

    @Test
    fun `registration needs a strong password that matches`() {
        assertNull(AuthValidation.validateRegistration("sufyaan", "Pulse@2026", "Pulse@2026"))
        assertNotNull(AuthValidation.validateRegistration("sufyaan", "Pul@1", "Pul@1"))           // too short
        assertEquals("Passwords do not match.", AuthValidation.validateRegistration("sufyaan", "Pulse@2026", "Pulse@2027"))
    }

    @Test
    fun `strong password policy rejects weak passwords`() {
        assertNotNull(AuthValidation.validatePassword("pulse@2026"))       // no upper case
        assertNotNull(AuthValidation.validatePassword("PULSE@2026"))       // no lower case
        assertNotNull(AuthValidation.validatePassword("Pulse@pulse"))      // no number
        assertNotNull(AuthValidation.validatePassword("Pulse2026x"))       // no symbol
        assertNotNull(AuthValidation.validatePassword("Pulse @2026"))      // space
        assertNotNull(AuthValidation.validatePassword("P@ssw0rd"))         // common password
        assertNotNull(AuthValidation.validatePassword("Sufyaan@2026", "sufyaan")) // contains username
        assertNull(AuthValidation.validatePassword("Budget#Win7"))
    }

    @Test
    fun `password strength meter`() {
        assertEquals(0, AuthValidation.passwordStrength(""))
        assertEquals(1, AuthValidation.passwordStrength("abc"))
        assertEquals(3, AuthValidation.passwordStrength("Budget#Win7"))
        assertEquals(4, AuthValidation.passwordStrength("Budget#Win7-Long"))
    }

    @Test
    fun `amounts accept comma or dot and reject zero or text`() {
        assertEquals(45.5, InputValidation.parseAmount("45,50")!!, 0.001)
        assertEquals(1250.0, InputValidation.parseAmount(" 1 250 ")!!, 0.001)
        assertNull(InputValidation.parseAmount("0"))
        assertNull(InputValidation.parseAmount("-5"))
        assertNull(InputValidation.parseAmount("abc"))
    }

    @Test
    fun `goal range must have min at or below max`() {
        assertNull(InputValidation.validateGoalRange(100.0, 500.0))
        assertNull(InputValidation.validateGoalRange(100.0, 0.0)) // no max set
        assertNotNull(InputValidation.validateGoalRange(600.0, 500.0))
        assertNotNull(InputValidation.validateGoalRange(null, 500.0))
        assertEquals(0.0, InputValidation.parseGoal("")!!, 0.001)
    }
}
