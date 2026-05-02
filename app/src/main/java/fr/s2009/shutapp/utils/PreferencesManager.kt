package fr.s2009.shutapp.utils

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class Device(
    val name: String,
    val ip: String,
    val os: String
)

@Serializable
data class Prefs(
    val devices: List<Device>?
)

object PreferencesSerializer : Serializer<Prefs> {
    override val defaultValue: Prefs = Prefs(devices = null)

    override suspend fun readFrom(input: InputStream): Prefs =
        try {
            Json.decodeFromString<Prefs>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read Preferences", serialization)
        }

    override suspend fun writeTo(t: Prefs, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(t)
                    .encodeToByteArray()
            )
        }
    }
}

private val Context.dataStore: DataStore<Prefs> by dataStore(
    fileName = "preferences.json",
    serializer = PreferencesSerializer,
)

class PreferencesManager (private val context: Context) {
    fun getDevices(): Flow<List<Device>> = context.dataStore.data.map { it.devices ?: emptyList() }

    suspend fun addDevice(device: Device) {
        context.dataStore.updateData { currentPrefs ->
            currentPrefs.copy(devices = currentPrefs.devices?.plus(device) ?: listOf(device))
        }
    }

    suspend fun removeDevice(device: Device) {
        context.dataStore.updateData { currentPrefs ->
            currentPrefs.copy(devices = currentPrefs.devices?.minus(device) ?: emptyList())
        }
    }

    suspend fun hasDevice(ip: String): Boolean {
        val devices = getDevices().first()

        for (device in devices) {
            if(device.ip == ip) return true
        }

        return false
    }

    suspend fun getDevice(ip: String): Device? {
        val devices = getDevices().first()

        for (device in devices) {
            if(device.ip == ip) return device
        }

        return null
    }
}