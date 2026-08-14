package com.securewol.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securewol.app.data.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SetupUiState {
    object EnterNewPin : SetupUiState()
    data class ConfirmPin(val initialPin: String) : SetupUiState()
    object Success : SetupUiState()
    data class Error(val message: String) : SetupUiState()
}

class SetupViewModel(private val securityRepository: SecurityRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SetupUiState>(SetupUiState.EnterNewPin)
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private var firstEnteredPin = ""

    fun onFirstPinEntered(pin: String) {
        if (pin.length < 4) {
            _uiState.value = SetupUiState.Error("PIN must be at least 4 digits")
            return
        }
        firstEnteredPin = pin
        _uiState.value = SetupUiState.ConfirmPin(pin)
    }

    fun onConfirmPinEntered(confirmPin: String) {
        if (confirmPin != firstEnteredPin) {
            _uiState.value = SetupUiState.Error("PINs do not match. Please try again.")
            return
        }

        viewModelScope.launch {
            val enrolled = securityRepository.enrollOwner(confirmPin.toCharArray())
            firstEnteredPin = ""
            if (enrolled) {
                _uiState.value = SetupUiState.Success
            } else {
                _uiState.value = SetupUiState.Error("Failed to initialize cryptographic storage.")
            }
        }
    }

    fun resetToStart() {
        firstEnteredPin = ""
        _uiState.value = SetupUiState.EnterNewPin
    }
}
