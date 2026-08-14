package com.securewol.app.ui.dashboard

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securewol.app.core.network.PcPowerStatus
import com.securewol.app.core.network.PcStatusChecker
import com.securewol.app.core.network.RemoteCommandResult
import com.securewol.app.core.network.RemotePowerAction
import com.securewol.app.core.network.RemotePowerManager
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

    private val _pcStatusMap = MutableStateFlow<Map<String, PcPowerStatus>>(emptyMap())
    val pcStatusMap: StateFlow<Map<String, PcPowerStatus>> = _pcStatusMap.asStateFlow()

    private val _pendingPowerOnPc = MutableStateFlow<PcDevice?>(null)
    val pendingPowerOnPc: StateFlow<PcDevice?> = _pendingPowerOnPc.asStateFlow()

    private val _pendingRemoteAction = MutableStateFlow<Pair<PcDevice, RemotePowerAction>?>(null)
    val pendingRemoteAction: StateFlow<Pair<PcDevice, RemotePowerAction>?> = _pendingRemoteAction.asStateFlow()

    private val _isSendingWol = MutableStateFlow(false)
    val isSendingWol: StateFlow<Boolean> = _isSendingWol.asStateFlow()

    private val _events = MutableSharedFlow<DashboardEvent>()
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

    init {
        startStatusPolling()
    }

    fun startStatusPolling() {
        viewModelScope.launch {
            while (true) {
                val currentPcs = pcList.value
                val updatedMap = _pcStatusMap.value.toMutableMap()
                for (pc in currentPcs) {
                    if (pc.ipAddress.isNotBlank()) {
                        val status = PcStatusChecker.checkStatus(pc.ipAddress)
                        updatedMap[pc.id] = status
                    } else {
                        updatedMap[pc.id] = PcPowerStatus.OFFLINE
                    }
                }
                _pcStatusMap.value = updatedMap
                kotlinx.coroutines.delay(4000) // Poll every 4 seconds
            }
        }
    }

    fun onPowerOnClicked(pc: PcDevice) {
        if (!SessionManager.isSessionValid()) {
            viewModelScope.launch {
                _events.emit(DashboardEvent.SessionExpired)
            }
            return
        }
        _pendingPowerOnPc.value = pc
    }

    fun onRemoteActionClicked(pc: PcDevice, action: RemotePowerAction) {
        if (!SessionManager.isSessionValid()) {
            viewModelScope.launch {
                _events.emit(DashboardEvent.SessionExpired)
            }
            return
        }
        _pendingRemoteAction.value = Pair(pc, action)
    }

    fun dismissRemoteAction() {
        _pendingRemoteAction.value = null
    }

    fun confirmRemoteAction() {
        val pair = _pendingRemoteAction.value ?: return
        val targetPc = pair.first
        val action = pair.second
        _pendingRemoteAction.value = null

        viewModelScope.launch {
            val result = RemotePowerManager.executePowerAction(
                targetIp = targetPc.ipAddress,
                action = action
            )
            when (result) {
                is RemoteCommandResult.Success -> {
                    _events.emit(DashboardEvent.ShowToast("${action.displayName} command sent to ${targetPc.name}"))
                    // Refresh status after 2 seconds
                    kotlinx.coroutines.delay(2000)
                    val status = PcStatusChecker.checkStatus(targetPc.ipAddress)
                    val map = _pcStatusMap.value.toMutableMap()
                    map[targetPc.id] = status
                    _pcStatusMap.value = map
                }
                is RemoteCommandResult.Failure -> {
                    _events.emit(DashboardEvent.ShowToast(result.message))
                }
            }
        }
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
