package com.securewol.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securewol.app.core.security.SessionManager
import com.securewol.app.data.model.AutoLockTimeout
import com.securewol.app.data.model.SecuritySettings
import com.securewol.app.data.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    object DeviceWiped : SettingsEvent()
    object SessionExpired : SettingsEvent()
}

class SecuritySettingsViewModel(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(securityRepository.getSecuritySettings())
    val settings: StateFlow<SecuritySettings> = _settings.asStateFlow()

    private val _deviceId = MutableStateFlow(securityRepository.deviceBindingManager.getBoundDeviceId() ?: "Unknown")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun updateBiometric(enabled: Boolean) {
        val updated = _settings.value.copy(isBiometricEnabled = enabled)
        saveSettings(updated)
    }

    fun updateAutoLockTimeout(timeout: AutoLockTimeout) {
        val updated = _settings.value.copy(autoLockTimeout = timeout)
        saveSettings(updated)
    }

    fun updateRequireAuthBeforePowerOn(required: Boolean) {
        val updated = _settings.value.copy(requireAuthBeforePowerOn = required)
        saveSettings(updated)
    }

    private fun saveSettings(newSettings: SecuritySettings) {
        try {
            securityRepository.updateSecuritySettings(newSettings)
            _settings.value = newSettings
        } catch (e: Exception) {
            viewModelScope.launch {
                _events.emit(SettingsEvent.SessionExpired)
            }
        }
    }

    fun changePin(oldPin: String, newPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!SessionManager.isSessionValid()) {
            viewModelScope.launch { _events.emit(SettingsEvent.SessionExpired) }
            return
        }

        if (newPin.length < 4) {
            onError("New PIN must be at least 4 digits")
            return
        }

        val success = securityRepository.changePin(oldPin.toCharArray(), newPin.toCharArray())
        if (success) {
            onSuccess()
            viewModelScope.launch { _events.emit(SettingsEvent.ShowToast("Owner PIN successfully updated")) }
        } else {
            onError("Current PIN was incorrect.")
        }
    }

    fun reRegisterDevice() {
        securityRepository.deviceBindingManager.invalidateAndReRegister()
        viewModelScope.launch {
            _events.emit(SettingsEvent.ShowToast("Device invalidated. All cryptographic storage wiped."))
            _events.emit(SettingsEvent.DeviceWiped)
        }
    }

    fun lockAppNow() {
        SessionManager.invalidateSession()
        viewModelScope.launch {
            _events.emit(SettingsEvent.SessionExpired)
        }
    }
}
