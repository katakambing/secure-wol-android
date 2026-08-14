package com.securewol.app.ui.dashboard

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securewol.app.core.network.WolDispatcher
import com.securewol.app.core.security.BiometricAuthManager
import com.securewol.app.core.security.SecureLogger
import com.securewol.app.core.security.SessionManager
import com.securewol.app.data.model.PcDevice
import com.securewol.app.data.repository.PcRepository
import com.securewol.app.data.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardEvent {
    data class ShowToast(val message: String) : DashboardEvent()
    object SessionExpired : DashboardEvent()
}

class DashboardViewModel(
    private val pcRepository: PcRepository,
    private val securityRepository: SecurityRepository
) : ViewModel() {

    val pcList: StateFlow<List<PcDevice>> = pcRepository.pcListFlow

    private val _pendingPowerOnPc = MutableStateFlow<PcDevice?>(null)
    val pendingPowerOnPc: StateFlow<PcDevice?> = _pendingPowerOnPc.asStateFlow()

    private val _isSendingWol = MutableStateFlow(false)
    val isSendingWol: StateFlow<Boolean> = _isSendingWol.asStateFlow()

    private val _events = MutableSharedFlow<DashboardEvent>()
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

    fun onPowerOnClicked(pc: PcDevice) {
        if (!SessionManager.isSessionValid()) {
            viewModelScope.launch {
                _events.emit(DashboardEvent.SessionExpired)
            }
            return
        }
        _pendingPowerOnPc.value = pc
    }

    fun dismissPowerOnConfirmation() {
        _pendingPowerOnPc.value = null
    }

    /**
     * Executes the guarded WoL packet transmission.
     */
    fun confirmPowerOn(activity: FragmentActivity? = null) {
        val targetPc = _pendingPowerOnPc.value ?: return

        val settings = securityRepository.getSecuritySettings()
        if (settings.requireAuthBeforePowerOn && activity != null) {
            val biometricManager = BiometricAuthManager(activity)
            if (biometricManager.canAuthenticateWithBiometrics()) {
                biometricManager.showBiometricPrompt(
                    title = "Confirm Power On",
                    subtitle = "Verify biometric to wake ${targetPc.name}",
                    allowDeviceCredentialFallback = true,
                    callback = object : BiometricAuthManager.AuthCallback {
                        override fun onAuthSuccess() {
                            executeWolDispatch(targetPc)
                        }

                        override fun onAuthFailed() {
                            viewModelScope.launch {
                                _events.emit(DashboardEvent.ShowToast("Biometric verification failed"))
                            }
                        }

                        override fun onAuthError(errorCode: Int, errString: CharSequence) {
                            viewModelScope.launch {
                                _events.emit(DashboardEvent.ShowToast("Authentication canceled"))
                            }
                        }

                        override fun onUsePinFallback() {
                            executeWolDispatch(targetPc)
                        }
                    }
                )
                return
            }
        }

        executeWolDispatch(targetPc)
    }

    private fun executeWolDispatch(targetPc: PcDevice) {
        _pendingPowerOnPc.value = null
        _isSendingWol.value = true

        viewModelScope.launch {
            try {
                when (val result = WolDispatcher.sendWakeOnLan(targetPc)) {
                    is WolDispatcher.WolResult.Success -> {
                        _events.emit(DashboardEvent.ShowToast("Wake-on-LAN Magic Packet sent to ${targetPc.name}"))
                    }
                    is WolDispatcher.WolResult.Failure -> {
                        _events.emit(DashboardEvent.ShowToast(result.errorMessage))
                    }
                    is WolDispatcher.WolResult.SecurityDenied -> {
                        _events.emit(DashboardEvent.ShowToast("Security Alert: Session expired"))
                        _events.emit(DashboardEvent.SessionExpired)
                    }
                }
            } catch (e: Exception) {
                SecureLogger.e("Error during WoL dispatch", e)
                _events.emit(DashboardEvent.ShowToast("Failed to transmit WoL packet"))
            } finally {
                _isSendingWol.value = false
            }
        }
    }

    fun deletePc(pc: PcDevice) {
        if (!SessionManager.isSessionValid()) {
            viewModelScope.launch {
                _events.emit(DashboardEvent.SessionExpired)
            }
            return
        }
        pcRepository.deletePcDevice(pc.id)
        viewModelScope.launch {
            _events.emit(DashboardEvent.ShowToast("Removed ${pc.name}"))
        }
    }

    fun lockAppNow() {
        SessionManager.invalidateSession()
        viewModelScope.launch {
            _events.emit(DashboardEvent.SessionExpired)
        }
    }
}
