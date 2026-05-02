package fr.s2009.shutapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.InetSocketAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketException
import java.util.Enumeration
import java.util.concurrent.Executors
import kotlin.collections.emptyList

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
    } else {
        (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
    }
}

fun isNetworkAvailableFlow(context: Context): Flow<Boolean> = callbackFlow {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runBlocking { delay(1000)  }
            trySend(isNetworkAvailable(context))
        }

        override fun onUnavailable() {
            trySend(false)
        }

        override fun onLost(network: Network) {
            trySend(false)
        }
    }

    connectivityManager.registerDefaultNetworkCallback(callback)
    trySend(isNetworkAvailable(context))
    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}

fun isPortOpen(ip: String): Boolean {
    try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(ip, 7421), 200)
            return true
        }
    } catch (_: IOException) {
        return false
    }
}

private val isScanning = MutableStateFlow(-1)
val isScanningFlow: StateFlow<Int> = isScanning.asStateFlow()

val devicesFound = MutableStateFlow(emptyList<Device>())

fun scanLocalNetwork() {
    devicesFound.value = emptyList()

    val nts = getNetworkToScan() ?: return
    val executor = Executors.newFixedThreadPool(30)
    var addresses: List<String> = emptyList()
    var subnet = ""

    for (p in 0..3){
        if (nts.second[p] == 256) {
            subnet += nts.first[p].toString() + "."
        } else {
            if(p == 3) {
                for (i in 1..(254 - nts.second[p])) {
                    addresses = addresses.plus("$subnet$i")
                }
            } else {
                for (i in 1..(255 - nts.second[p])) {
                    addresses = addresses.plus("$subnet$i.")
                }
            }
        }
    }

    for (h in 0..<addresses.size) {
        isScanning.value = h
        executor.submit {
            if (isPortOpen(addresses[h])) {
                getDeviceInfo(
                    ip = addresses[h],
                    onResult = { device ->
                        devicesFound.value = devicesFound.value.plus(device)
                    },
                    onError = {}
                )
            }
        }
    }

    isScanning.value = -1
    executor.shutdown()
}


private fun getInterfaceAddress(): InterfaceAddress? {
    try {
        val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()

        while(en.hasMoreElements()) {
            val networkInterface: NetworkInterface = en.nextElement()
            val interfaceAddresses: MutableList<InterfaceAddress> = networkInterface.interfaceAddresses

            for (i in 0 until interfaceAddresses.size) {
                val interfaceAddress = interfaceAddresses[i]

                if (interfaceAddress.toString().indexOf((":")) < 0) {
                    if (!interfaceAddress.address.isLoopbackAddress)
                        return interfaceAddress
                }
            }
        }
    } catch (e: SocketException) {
        e.printStackTrace()
    }
    return null
}

private fun binaryToInt(binaryDigits: String): Int {
    val binaryValues = arrayListOf(1, 2, 4, 8, 16, 32, 64, 128, 256)
    val reversedDigits = binaryDigits.reversed()

    var returnedInt = 0
    var x = 0
    for (i in reversedDigits){
        if (i == '1' && x < 16) {
            returnedInt += binaryValues[x]
        }
        x++
    }
    return returnedInt
}

private fun getNetworkToScan(): Pair<List<Int>, List<Int>>? {
    val ipAddress = getInterfaceAddress()?.address.toString().drop(1).split(".")
    val mask = getInterfaceAddress()?.networkPrefixLength?.toInt()

    if(ipAddress.isEmpty() || mask == null) return null

    val binaryMask: List<String> = ("1".repeat(mask) + "0".repeat(32 - mask)).chunked(8)
    var binaryIp: List<String> = emptyList()

    for (part in ipAddress) {
        val bin = Integer.toBinaryString(part.toInt())
        binaryIp = binaryIp.plus("0".repeat(8 - bin.length) + bin)
    }

    var networkAddress: List<Int> = emptyList()
    var ranges: List<Int> = emptyList()

    for (p in 0..3) {
        var part = ""

        for (i in 0..7) {
            part += if(binaryMask[p][i] == '1' && binaryIp[p][i] == '1') {
                "1"
            } else {
                "0"
            }
        }

        networkAddress = networkAddress.plus(binaryToInt(part))

        ranges = if(binaryMask[p].contains("0")) {
            ranges.plus(binaryToInt(binaryMask[p]))
        } else {
            ranges.plus(256)
        }
    }

    return networkAddress to ranges
}