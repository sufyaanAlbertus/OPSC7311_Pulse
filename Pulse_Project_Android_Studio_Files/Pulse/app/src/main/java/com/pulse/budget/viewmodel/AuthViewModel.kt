package com.pulse.budget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.budget.PrefsRepository
import com.pulse.budget.data.local.AppDatabase
import com.pulse.budget.data.repository.AuthRepository
import com.pulse.budget.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val authRepo = AuthRepository(db)
    private val prefs = PrefsRepository(application)

    /** Null while the initial DataStore read hasn't completed, then null-if-logged-out or a user id. */
    val sessionUserId: StateFlow<Long?> = prefs.sessionUserId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    /** Display username for the Profile screen, re-looked-up whenever the session user changes. */
    val currentUsername: StateFlow<String?> = sessionUserId.mapLatest { id ->
        id?.let { authRepo.getUsername(it) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** "Member since" epoch day for the Profile screen. */
    val memberSinceEpochDay: StateFlow<Long?> = sessionUserId.mapLatest { id ->
        id?.let { authRepo.getCreatedAtEpochDay(it) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Current profile picture Uri (as a String), re-looked-up whenever the session user or the
     *  picture itself changes — bumping this StateFlow's upstream via re-emitting sessionUserId
     *  isn't enough after an in-place update, so updateProfilePicture() re-pushes the same id
     *  through a separate trigger below to force a refresh. */
    private val _profileImageRefreshTrigger = MutableStateFlow(0)
    val currentProfileImageUri: StateFlow<String?> = kotlinx.coroutines.flow.combine(
        sessionUserId, _profileImageRefreshTrigger
    ) { id, _ -> id }.mapLatest { id ->
        id?.let { authRepo.getProfileImageUri(it) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun register(username: String, password: String, profileImageUri: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = authRepo.register(username, password, profileImageUri)) {
                is AuthResult.Success -> {
                    prefs.setSessionUserId(result.userId)
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
                    prefs.setSessionUserId(result.userId)
                    _errorMessage.value = null
                    onSuccess()
                }
                is AuthResult.Failure -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch { prefs.setSessionUserId(null) }
    }

    /** Called from the Profile screen when the user taps their avatar to change it. */
    fun updateProfilePicture(uri: String?) {
        val userId = sessionUserId.value ?: return
        viewModelScope.launch {
            authRepo.updateProfileImage(userId, uri)
            _profileImageRefreshTrigger.value += 1
        }
    }
}