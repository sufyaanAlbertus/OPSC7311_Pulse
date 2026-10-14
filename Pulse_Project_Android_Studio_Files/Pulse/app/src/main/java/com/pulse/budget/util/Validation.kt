package com.pulse.budget.util

// Input validation pulled into pure functions so it can be unit-tested (ValidationTest).
object AuthValidation {

    /** Firebase Authentication's email/password provider needs an email address. */
    const val USERNAME_EMAIL_DOMAIN = "users.pulse-budget.app"

    const val MIN_USERNAME = 3
    const val MAX_USERNAME = 20
    const val MIN_PASSWORD = 8

    private val USERNAME_PATTERN = Regex("^[a-z0-9._]+$")

    fun normalizeUsername(raw: String): String = raw.trim().lowercase()

    fun usernameToEmail(raw: String): String = "${normalizeUsername(raw)}@$USERNAME_EMAIL_DOMAIN"

    /** Returns an error message, or null when the username is valid. */
    fun validateUsername(raw: String): String? {
        val u = normalizeUsername(raw)
        return when {
            u.length < MIN_USERNAME -> "Username must be at least $MIN_USERNAME characters."
            u.length > MAX_USERNAME -> "Username can be at most $MAX_USERNAME characters."
            !USERNAME_PATTERN.matches(u) -> "Use only letters, numbers, dots or underscores."
            else -> null
        }
    }

    // Password policy for NEW accounts: at least 8 characters with an upper-case letter.

    /** One rule of the password policy, for the live checklist on the Register screen. */
    data class PasswordRule(val label: String, val passed: Boolean)

    private val COMMON_PASSWORDS = setOf(
        "password", "password1", "password123", "12345678", "123456789", "1234567890", "qwerty123",
        "qwertyuiop", "iloveyou", "admin123", "welcome1", "letmein1", "abc12345", "11111111",
        "00000000", "passw0rd", "p@ssw0rd", "p@ssword1", "football1", "monkey123", "sunshine1"
    )

    fun passwordRules(password: String, username: String = ""): List<PasswordRule> {
        val u = normalizeUsername(username)
        return listOf(
            PasswordRule("At least $MIN_PASSWORD characters", password.length >= MIN_PASSWORD),
            PasswordRule("An upper-case letter (A–Z)", password.any { it.isUpperCase() }),
            PasswordRule("A lower-case letter (a–z)", password.any { it.isLowerCase() }),
            PasswordRule("A number (0–9)", password.any { it.isDigit() }),
            PasswordRule("A symbol (e.g. ! @ # \$ %)", password.any { !it.isLetterOrDigit() && !it.isWhitespace() }),
            PasswordRule("No spaces", password.isNotEmpty() && password.none { it.isWhitespace() }),
            PasswordRule(
                "Not a common password or your username",
                password.isNotEmpty() && password.lowercase() !in COMMON_PASSWORDS &&
                        (u.length < 3 || !password.lowercase().contains(u))
            )
        )
    }

    /** 0 = empty, 1 = weak, 2 = fair, 3 = good, 4 = strong. */
    fun passwordStrength(password: String, username: String = ""): Int {
        if (password.isEmpty()) return 0
        val passed = passwordRules(password, username).count { it.passed }
        val total = passwordRules(password, username).size
        return when {
            passed == total && password.length >= 12 -> 4
            passed == total -> 3
            passed >= total - 2 -> 2
            else -> 1
        }
    }

    /** Returns the first broken rule as an error message, or null when the password is strong enough. */
    fun validatePassword(password: String, username: String = ""): String? =
        passwordRules(password, username).firstOrNull { !it.passed }?.let { "Stronger password needed: ${it.label}." }

    fun validateRegistration(username: String, password: String, confirmPassword: String): String? =
        validateUsername(username)
            ?: validatePassword(password, username)
            ?: if (password != confirmPassword) "Passwords do not match." else null
}

object InputValidation {
    /** Parses an amount typed by the user; accepts "45,50" as well as "45.50". */
    fun parseAmount(text: String): Double? {
        val value = text.trim().replace(" ", "").replace(',', '.').toDoubleOrNull() ?: return null
        return if (value > 0.0 && value.isFinite()) value else null
    }

    /** Parses a goal amount where 0 is allowed (means "no goal"). */
    fun parseGoal(text: String): Double? {
        if (text.isBlank()) return 0.0
        val value = text.trim().replace(" ", "").replace(',', '.').toDoubleOrNull() ?: return null
        return if (value >= 0.0 && value.isFinite()) value else null
    }

    /** Returns an error message, or null when the min/max pair is valid. */
    fun validateGoalRange(min: Double?, max: Double?): String? = when {
        min == null || max == null -> "Enter valid amounts (numbers only)."
        max > 0.0 && min > max -> "Minimum goal can't be more than the maximum goal."
        else -> null
    }
}
