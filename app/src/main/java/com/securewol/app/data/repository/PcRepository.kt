package com.securewol.app.data.repository

import android.content.Context
import com.securewol.app.core.security.KeystoreManager
import com.securewol.app.core.security.SecureLogger
import com.securewol.app.core.security.SessionManager
import com.securewol.app.data.model.PcDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * PcRepository: Encrypted persistence layer for configured PC devices.
 * All operations require an authenticated active session.
 */
class PcRepository(private val context: Context) {

    companion object {
        private const val KEY_STORED_PCS_JSON = "stored_pc_devices_enc_json"
    }

    private val prefs by lazy { KeystoreManager.getEncryptedPreferences(context) }
    private val _pcListFlow = MutableStateFlow<List<PcDevice>>(emptyList())
    val pcListFlow: StateFlow<List<PcDevice>> = _pcListFlow.asStateFlow()

    init {
        loadPcList()
    }

    /**
     * Loads the stored PC list from encrypted storage.
     */
    fun loadPcList(): List<PcDevice> {
        val rawJson = prefs.getString(KEY_STORED_PCS_JSON, null)
        if (rawJson.isNullOrBlank()) {
            _pcListFlow.value = emptyList()
            return emptyList()
        }

        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<PcDevice>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PcDevice(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        macAddress = obj.getString("macAddress"),
                        ipAddress = obj.optString("ipAddress", ""),
                        broadcastAddress = obj.optString("broadcastAddress", "255.255.255.255"),
                        port = obj.optInt("port", 9),
                        secureOnPassword = if (obj.has("secureOnPassword") && !obj.isNull("secureOnPassword")) obj.getString("secureOnPassword") else null,
                        agentAuthToken = if (obj.has("agentAuthToken") && !obj.isNull("agentAuthToken")) obj.getString("agentAuthToken") else null,
                        createdAtEpoch = obj.optLong("createdAtEpoch", System.currentTimeMillis())
                    )
                )
            }
            _pcListFlow.value = list
            list
        } catch (e: Exception) {
            SecureLogger.e("Failed to deserialize encrypted PC configurations", e)
            emptyList()
        }
    }

    /**
     * Saves or updates a PC device.
     */
    @Synchronized
    fun savePcDevice(device: PcDevice) {
        SessionManager.validateSessionOrThrow()

        val currentList = loadPcList().toMutableList()
        val cleanTargetMac = device.macAddress.replace(":", "").replace("-", "").trim().lowercase()
        
        // Find existing either by ID or by matching MAC address
        val index = currentList.indexOfFirst {
            it.id == device.id || it.macAddress.replace(":", "").replace("-", "").trim().lowercase() == cleanTargetMac
        }

        if (index >= 0) {
            // Update existing entry keeping original ID
            currentList[index] = device.copy(id = currentList[index].id)
        } else {
            currentList.add(device)
        }
        persistList(currentList)
        SecureLogger.i("PC device saved to encrypted storage")
    }

    /**
     * Deletes a PC device by ID.
     */
    @Synchronized
    fun deletePcDevice(deviceId: String) {
        SessionManager.validateSessionOrThrow()

        val currentList = loadPcList().toMutableList()
        currentList.removeAll { it.id == deviceId }
        persistList(currentList)
        SecureLogger.i("PC device deleted from encrypted storage")
    }

    /**
     * Gets a specific PC device by ID.
     */
    fun getPcDeviceById(deviceId: String): PcDevice? {
        return loadPcList().find { it.id == deviceId }
    }

    private fun persistList(list: List<PcDevice>) {
        val array = JSONArray()
        for (pc in list) {
            val obj = JSONObject().apply {
                put("id", pc.id)
                put("name", pc.name)
                put("macAddress", pc.macAddress)
                put("ipAddress", pc.ipAddress)
                put("broadcastAddress", pc.broadcastAddress)
                put("port", pc.port)
                put("secureOnPassword", pc.secureOnPassword)
                put("agentAuthToken", pc.agentAuthToken)
                put("createdAtEpoch", pc.createdAtEpoch)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_STORED_PCS_JSON, array.toString()).commit()
        _pcListFlow.value = list.toList()
    }
}
