package fr.s2009.shutapp.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private class AppWebSocketListener(
    private val onMessageReceived: (String) -> Unit,
    private val onConnected: () -> Unit,
    private val onDisconnected: (String?) -> Unit,
    private val onError: (Throwable) -> Unit
) : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        onConnected()
    }
    override fun onMessage(webSocket: WebSocket, text: String) {
        onMessageReceived(text)
    }
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        onDisconnected(reason)
    }
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onError(t)
    }
}

private object WebSocketManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    fun connect(
        url: String,
        listener: AppWebSocketListener
    ) {
        if (webSocket != null) return
        val request = Request.Builder()
            .url(url)
            .build()
        webSocket = client.newWebSocket(request, listener)
    }
    fun sendMessage(message: String): Boolean {
        return webSocket?.send(message) ?: false
    }
    fun close() {
        webSocket?.close(1000, "Closed by client")
        webSocket = null
    }
}

fun getDeviceInfo(ip: String, onResult: (Device) -> Unit, onError: (Throwable) -> Unit) {
    Thread {
        if(!isPortOpen(ip)) return@Thread onError(Throwable("ShutApp port is not open."))

        val wsListener = AppWebSocketListener(
            onConnected = {},
            onDisconnected = {},
            onError = {e -> onError(e)},
            onMessageReceived = { msg ->
                val jsonParser = Json { ignoreUnknownKeys = true }
                val jsonMap = jsonParser.decodeFromString<Map<String, String>>(msg)
                val device = Device(
                    name = jsonMap["hostname"] ?: "",
                    ip = ip,
                    os = jsonMap["os"] ?: ""
                )

                onResult(device)
            },
        )

        WebSocketManager.connect("ws://${ip}:7421", wsListener)
        WebSocketManager.sendMessage("ping")
        WebSocketManager.close()
    }.start()
}

private val isDeviceOnline = MutableStateFlow(false)
val isDeviceOnlineFlow: StateFlow<Boolean> = isDeviceOnline.asStateFlow()


fun checkDeviceStatus(ip: String) {
    Thread {
        if(!isPortOpen(ip)) {
            isDeviceOnline.value = false

            runBlocking { delay(10000) }

            checkDeviceStatus(ip)
            Thread.currentThread().interrupt()
        }

        val wsListener = AppWebSocketListener(
            onConnected = { isDeviceOnline.value = true },
            onDisconnected = {},
            onError = {
                isDeviceOnline.value = false

                WebSocketManager.close()

                runBlocking { delay(10000) }

                checkDeviceStatus(ip)
                Thread.currentThread().interrupt()
            },
            onMessageReceived = {},
        )

        WebSocketManager.connect("ws://${ip}:7421", wsListener)
    }.start()
}

fun sendCommand(ip: String, cmd: String) {
    if(!isDeviceOnline.value) return

    val wsListener = AppWebSocketListener(
        onConnected = {},
        onDisconnected = {},
        onError = {},
        onMessageReceived = {},
    )

    WebSocketManager.connect("ws://${ip}:7421", wsListener)
    WebSocketManager.sendMessage(cmd)
    WebSocketManager.close()
}