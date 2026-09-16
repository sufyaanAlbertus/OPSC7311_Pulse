package com.pulse.budget.data.repository

import android.util.Log
import com.pulse.budget.data.local.AppDatabase
import com.pulse.budget.data.local.UserEntity
import java.security.MessageDigest
import java.time.LocalDate

private const val TAG = "AuthRepository"

sealed class AuthResult {
    data class Success(val userId: Long) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/**
 * Brief (Part 2) asks for login with "a username and password". The stored field is still
 * called `email` on UserEntity/UserDao for backward compatibility with the schema, but it is
 * treated as a plain username here — no "@" or email-format requirement.
 */
class AuthRepository(db: AppDatabase) {
    private val userDao = db.userDao()

    private fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun register(username: String, password: String, profileImageUri: String? = null): AuthResult {
        val normalized = username.trim().lowercase()
        if (normalized.length < 3) {
            return AuthResult.Failure("Username must be at least 3 characters.")
        }
        if (password.length < 6) {
            return AuthResult.Failure("Password must be at least 6 characters.")
        }
        if (userDao.findByEmail(normalized) != null) {
            return AuthResult.Failure("That username is already taken.")
        }
        val id = userDao.insert(
            UserEntity(
                email = normalized,
                passwordHash = hash(password),
                createdAtEpochDay = LocalDate.now().toEpochDay(),
                profileImageUri = profileImageUri
            )
        )
        Log.d(TAG, "Registered new user: '$normalized' -> id=$id")
        return AuthResult.Success(id)
    }

    suspend fun login(username: String, password: String): AuthResult {
        val normalized = username.trim().lowercase()
        val user = userDao.findByEmail(normalized)
        if (user == null) {
            Log.d(TAG, "Login failed: no user '$normalized'")
            return AuthResult.Failure("No account found for that username.")
        }
        return if (user.passwordHash == hash(password)) {
            Log.d(TAG, "Login success: '$normalized' -> id=${user.id}")
            AuthResult.Success(user.id)
        } else {
            Log.d(TAG, "Login failed: wrong password for '$normalized'")
            AuthResult.Failure("Incorrect password.")
        }
    }

    /** Looks up the display username for the Profile screen. */
    suspend fun getUsername(userId: Long): String? = userDao.findById(userId)?.email

    /** Looks up when the account was created, for the Profile screen's "member since" line. */
    suspend fun getCreatedAtEpochDay(userId: Long): Long? = userDao.findById(userId)?.createdAtEpochDay

    /** Looks up the current profile picture, for the Profile screen's avatar. */
    suspend fun getProfileImageUri(userId: Long): String? = userDao.findById(userId)?.profileImageUri

    /** Called when the user taps their avatar on the Profile screen to change it. */
    suspend fun updateProfileImage(userId: Long, uri: String?) {
        Log.d(TAG, "Profile picture updated for user id=$userId")
        userDao.updateProfileImage(userId, uri)
    }
}