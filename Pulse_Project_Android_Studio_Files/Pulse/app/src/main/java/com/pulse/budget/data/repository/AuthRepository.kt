package com.pulse.budget.data.repository

/*
 * References:
 *  - Firebase (2025) Password authentication on Android. https://firebase.google.com/docs/auth/android/password-auth
 *  - Anthropic (2026) Claude [Large language model] — used as a coding assistant; see AI declaration in README.
 */

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.pulse.budget.util.AuthValidation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthRepository"

sealed class AuthResult {
    data class Success(val uid: String) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/** Login and registration use Firebase Authentication. */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val pulseRepository: PulseRepository = PulseRepository()
) {

    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun register(username: String, password: String, profileImageBase64: String?): AuthResult {
        AuthValidation.validateUsername(username)?.let { return AuthResult.Failure(it) }
        AuthValidation.validatePassword(password, username)?.let { return AuthResult.Failure(it) }
        val normalized = AuthValidation.normalizeUsername(username)

        return try {
            val result = auth.createUserWithEmailAndPassword(AuthValidation.usernameToEmail(normalized), password).await()
            val uid = result.user?.uid ?: return AuthResult.Failure("Registration failed. Please try again.")
            Log.d(TAG, "Firebase Auth: registered '$normalized' -> uid=$uid")

            // Create the user's Firestore documents straight away.
            pulseRepository.createUserProfile(uid, normalized, profileImageBase64)
            pulseRepository.seedBadgesIfMissing(uid)
            AuthResult.Success(uid)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth: register failed for '$normalized'", e)
            AuthResult.Failure(friendlyMessage(e))
        }
    }

    suspend fun login(username: String, password: String): AuthResult {
        val normalized = AuthValidation.normalizeUsername(username)
        if (normalized.isBlank() || password.isBlank()) {
            return AuthResult.Failure("Enter your username and password.")
        }
        return try {
            val result = auth.signInWithEmailAndPassword(AuthValidation.usernameToEmail(normalized), password).await()
            val uid = result.user?.uid ?: return AuthResult.Failure("Login failed. Please try again.")
            Log.d(TAG, "Firebase Auth: login success '$normalized' -> uid=$uid")
            AuthResult.Success(uid)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth: login failed for '$normalized'", e)
            AuthResult.Failure(friendlyMessage(e))
        }
    }

    fun logout() {
        Log.d(TAG, "Firebase Auth: sign out uid=$currentUid")
        auth.signOut()
    }

    /** Turns Firebase exceptions into messages a user can act on. */
    private fun friendlyMessage(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "That username is already taken."
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Use 8+ characters with upper and lower case, a number and a symbol."
        is FirebaseAuthInvalidUserException -> "No account found for that username."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect username or password."
        // Firebase blocks a device after many failed attempts (protects against password guessing).
        is FirebaseTooManyRequestsException -> "Too many attempts. For your security, please wait a few minutes and try again."
        is FirebaseNetworkException -> "No internet connection. Check your data or Wi-Fi and try again."
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }

    companion object {
        /** Emits the signed-in uid (or null) every time the Firebase login state changes. */
        fun authState(auth: FirebaseAuth = FirebaseAuth.getInstance()): Flow<String?> = callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }
}
