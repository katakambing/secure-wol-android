package com.securewol.app.ui.pcedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securewol.app.core.network.WolPacketBuilder
import com.securewol.app.core.security.SessionManager
import com.securewol.app.data.model.PcDevice
import com.securewol.app.data.repository.PcRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PcEditUiEvent {
    object Saved : PcEditUiEvent()
    object Deleted : PcEditUiEvent()
    data class Error(val message: String) : PcEditUiEvent()
}

class PcEditViewModel(
    private val pcRepository: PcRepository,
    private val editingPcId: String?
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _macAddress = MutableStateFlow("")
    val macAddress: StateFlow<String> = _macAddress.asStateFlow()

    private val _ipAddress = MutableStateFlow("")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    private val _broadcastAddress = MutableStateFlow("255.255.255.255")
    val broadcastAddress: StateFlow<String> = _broadcastAddress.asStateFlow()

    private val _port = MutableStateFlow("9")
    val port: StateFlow<String> = _port.asStateFlow()

    private val _secureOnPassword = MutableStateFlow("")
    val secureOnPassword: StateFlow<String> = _secureOnPassword.asStateFlow()

    private val _events = MutableSharedFlow<PcEditUiEvent>()
    val events: SharedFlow<PcEditUiEvent> = _events.asSharedFlow()

    val isEditing: Boolean = !editingPcId.isNullOrBlank()

    init {
        if (!editingPcId.isNullOrBlank()) {
            loadExistingPc(editingPcId)
        }
    }

    private fun loadExistingPc(id: String) {
        val pc = pcRepository.getPcDeviceById(id)
        if (pc != null) {
            _name.value = pc.name
            _macAddress.value = pc.macAddress
            _ipAddress.value = pc.ipAddress
            _broadcastAddress.value = pc.broadcastAddress
            _port.value = pc.port.toString()
            _secureOnPassword.value = pc.secureOnPassword ?: ""
        }
    }

    fun onNameChanged(v: String) { _name.value = v }
    fun onMacChanged(v: String) { _macAddress.value = v }
    fun onIpChanged(v: String) { _ipAddress.value = v }
    fun onBroadcastChanged(v: String) { _broadcastAddress.value = v }
    fun onPortChanged(v: String) { _port.value = v }
    fun onSecureOnChanged(v: String) { _secureOnPassword.value = v }

    fun save() {
        if (!SessionManager.isSessionValid()) {
            viewModelScope.launch {
                _events.emit(PcEditUiEvent.Error("Session expired. Please authenticate."))
            }
            return
        }

        val pcName = _name.value.trim()
        val rawMac = _macAddress.value.trim()
        val rawPort = _port.value.trim().toIntOrNull() ?: 9

        if (pcName.isBlank()) {
            viewModelScope.launch { _events.emit(PcEditUiEvent.Error("PC Name cannot be blank.")) }
            return
        }

        if (!WolPacketBuilder.isValidMac(rawMac)) {
            viewModelScope.launch { _events.emit(PcEditUiEvent.Error("Invalid MAC address. Must be 12 hex characters (e.g. 00:11:22:33:44:55).")) }
            return
        }

        val device = PcDevice(
            id = editingPcId ?: java.util.UUID.randomUUID().toString(),
            name = pcName,
            macAddress = rawMac,
            ipAddress = _ipAddress.value.trim(),
            broadcastAddress = _broadcastAddress.value.trim().ifBlank { "255.255.255.255" },
            port = rawPort,
            secureOnPassword = _secureOnPassword.value.trim().ifBlank { null }
        )

        try {
            pcRepository.savePcDevice(device)
            viewModelScope.launch { _events.emit(PcEditUiEvent.Saved) }
        } catch (e: Exception) {
            viewModelScope.launch { _events.emit(PcEditUiEvent.Error("Failed to save PC configuration.")) }
        }
    }

    fun delete() {
        if (editingPcId.isNullOrBlank()) return
        try {
            pcRepository.deletePcDevice(editingPcId)
            viewModelScope.launch { _events.emit(PcEditUiEvent.Deleted) }
        } catch (e: Exception) {
            viewModelScope.launch { _events.emit(PcEditUiEvent.Error("Failed to delete PC configuration.")) }
        }
    }
}
