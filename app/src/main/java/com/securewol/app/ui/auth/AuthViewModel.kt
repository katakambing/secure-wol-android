package com.securewol.app.ui.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securewol.app.core.security.BiometricAuthManager
import com.securewol.app.core.security.SecureLogger
import com.securewol.app.data.model.AuthState
import com.securewol.app.data.repository.SecurityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Authenticated : AuthUiState()
    data class Error(val message: String, val remainingAttempts: Int) : AuthUiState()
    data class LockedOut(val remainingSeconds: Long) : AuthUiState()
}

class AuthViewModel(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var lockoutTimerJob: Job? = null

    init {
        checkLockoutStatus()
    }

    fun checkLockoutStatus() {
        if (securityRepository.lockoutController.isLockedOut()) {
            startLockoutCountdown(securityRepository.lockoutController.getRemainingLockoutSeconds())
        } else {
            _uiState.value = AuthUiState.Idle
        }
    }

    /**
     * Attempts Biometric authentication.
     */
    fun triggerBiometricAuth(activity: FragmentActivity) {
        if (securityRepository.lockoutController.isLockedOut()) {
            checkLockoutStatus()
            return
        }

        val biometricManager = BiometricAuthManager(activity)
        if (!biometricManager.canAuthenticateWithBiometrics()) {
            SecureLogger.d("Biometrics unavailable or not enrolled on device")
            return
        }

        biometricManager.showBiometricPrompt(
            title = "Authenticate to continue",
            subtitle = "Scan biometric or enter device PIN to unlock PC controls",
            allowDeviceCredentialFallback = true,
            callback = object : BiometricAuthManager.AuthCallback {
                override fun onAuthSuccess() {
                    securityRepository.recordBiometricSuccess()
                    _uiState.value = AuthUiState.Authenticated
                }

                override fun onAuthFailed() {
                    securityRepository.recordBiometricFailure()
                    handleFailure()
                }

                override fun onAuthError(errorCode: Int, errString: CharSequence) {
                    SecureLogger.w("Biometric error [$errorCode]: $errString")
                }

                override fun onUsePinFallback() {
                    _uiState.value = AuthUiState.Idle
                }
            }
        )
    }

    /**
     * Verifies entered App PIN.
     */
    fun submitPin(pin: String) {
        if (securityRepository.lockoutController.isLockedOut()) {
            checkLockoutStatus()
            return
        }

        val success = securityRepository.verifyAppPin(pin.toCharArray())
        if (success) {
            _uiState.value = AuthUiState.Authenticated
        } else {
            handleFailure()
        }
    }

    private fun handleFailure() {
        if (securityRepository.lockoutController.isLockedOut()) {
            startLockoutCountdown(securityRepository.lockoutController.getRemainingLockoutSeconds())
        } else {
            val count = securityRepository.lockoutController.getFailedAttemptsCount()
            _uiState.value = AuthUiState.Error("Incorrect PIN or biometric mismatch", count)
        }
    }

    private fun startLockoutCountdown(initialSeconds: Long) {
        lockoutTimerJob?.cancel()
        lockoutTimerJob = viewModelScope.launch {
            var seconds = initialSeconds
            while (seconds > 0) {
                _uiState.value = AuthUiState.LockedOut(seconds)
                delay(1000)
                seconds = securityRepository.lockoutController.getRemainingLockoutSeconds()
            }
            _uiState.value = AuthUiState.Idle
        }
    }
}
