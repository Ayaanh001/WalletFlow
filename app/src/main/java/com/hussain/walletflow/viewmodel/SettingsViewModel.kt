package com.hussain.walletflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.walletflow.data.UserPreferencesRepository
import com.hussain.walletflow.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserPreferencesRepository(application)

    val themeMode: StateFlow<ThemeMode> = repository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.AUTO
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
    }
}
