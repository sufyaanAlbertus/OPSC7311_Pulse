package com.pulse.budget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.budget.PrefsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide theme state. */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PrefsRepository(application)

    val isDarkTheme = prefs.isDarkTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    fun toggleTheme() {
        viewModelScope.launch {
            prefs.setDarkTheme(!isDarkTheme.value)
        }
    }
}
