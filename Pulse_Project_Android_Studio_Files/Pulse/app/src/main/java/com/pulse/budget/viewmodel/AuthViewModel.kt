package com.pulse.budget.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.budget.data.model.UserProfile
import com.pulse.budget.data.repository.AuthRepository
import com.pulse.budget.data.repository.AuthResult
import com.pulse.budget.data.repository.PulseRepository
import com.pulse.budget.util.AuthValidation
import com.pulse.budget.util.ImageUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "AuthViewModel"

/** Login state comes from Firebase Authentication. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val pulseRepo = PulseRepository()
    private val authRepo = AuthRepository(pulseRepository = pulseRepo)

    /** Signed-in Firebase uid, or null when logged out. */
    val sessionUserId: StateFlow<String?> = AuthRepository.authState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = authRepo.currentUid
    )

    private val profile: StateFlow<UserProfile?> = sessionUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else pulseRepo.userProfile(uid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentUsername: StateFlow<String?> = profile.map { it?.username?.takeIf { name -> name.isNotBlank() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val memberSinceEpochDay: StateFlow<Long?> = profile.map { it?.createdAtEpochDay?.takeIf { day -> day > 0 } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Base64 JPEG of the profile picture (stored in Firestore), or null. */
    val currentProfileImage: StateFlow<String?> = profile.map { it?.profileImageBase64 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun register(
        username: String,
        password: String,
        confirmPassword: String,
        profileImageUri: Uri?,
        onSuccess: () -> Unit
    ) {
        // Validate locally first so the user gets instant feedback without a network call.
        AuthValidation.validateRegistration(username, password, confirmPassword)?.let {
            _errorMessage.value = it
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val imageBase64 = profileImageUri?.let {
                ImageUtils.uriToCompressedBase64(getApplication(), it, ImageUtils.PROFILE_MAX_DIMENSION, quality = 70)
            }
            when (val result = authRepo.register(username, password, imageBase64)) {
                is AuthResult.Success -> {
                    Log.d(TAG, "Register success uid=${result.uid}")
                    _errorMessage.value = null
                    onSuccess()
                }
                is AuthResult.Failure -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = authRepo.login(username, password)) {
                is AuthResult.Success -> {
                    Log.d(TAG, "Login success uid=${result.uid}")
                    _errorMessage.value = null
                    onSuccess()
                }
                is AuthResult.Failure -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        authRepo.logout()
    }

    /** Called from the Profile screen when the user taps their avatar to change it. */
    fun updateProfilePicture(uri: Uri) {
        val uid = sessionUserId.value ?: return
        viewModelScope.launch {
            try {
                val base64 = ImageUtils.uriToCompressedBase64(getApplication(), uri, ImageUtils.PROFILE_MAX_DIMENSION, quality = 70)
                    ?: return@launch
                pulseRepo.updateProfileImage(uid, base64)
            } catch (e: Exception) {
                Log.e(TAG, "Profile picture upload failed", e)
            }
        }
    }
}
