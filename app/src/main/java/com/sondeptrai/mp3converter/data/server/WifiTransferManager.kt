package com.sondeptrai.mp3converter.data.server

import android.content.Context
import android.net.wifi.WifiManager
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

data class TransferLog(
    val id: Long = System.currentTimeMillis(),
    val time: String,
    val message: String
)

class WifiTransferManager(
    private val context: Context,
    private val fileManager: AudioFileManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var server: WifiHttpServer? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    private val _logs = MutableStateFlow<List<TransferLog>>(emptyList())
    val logs: StateFlow<List<TransferLog>> = _logs.asStateFlow()

    fun startServer(port: Int = 8080): Boolean {
        if (_isServerRunning.value) return true

        val ip = getLocalWifiIpAddress()
        if (ip == null) {
            addLog("Không tìm thấy mạng Wi-Fi hoặc Hotspot! Vui lòng kết nối Wi-Fi.")
            return false
        }

        return try {
            server = WifiHttpServer(
                port = port,
                context = context,
                fileManager = fileManager,
                onLogEvent = { message -> addLog(message) },
                onFilesChanged = {
                    scope.launch {
                        fileManager.reloadLibrary()
                    }
                }
            )
            server?.start()
            _isServerRunning.value = true
            _serverUrl.value = "http://$ip:$port"
            addLog("Máy chủ đã khởi động tại http://$ip:$port")
            true
        } catch (e: Exception) {
            addLog("Lỗi khởi động máy chủ: ${e.localizedMessage}")
            false
        }
    }

    fun stopServer() {
        try {
            server?.stop()
            server = null
            _isServerRunning.value = false
            _serverUrl.value = null
            addLog("Máy chủ đã dừng hoạt động.")
        } catch (e: Exception) {
            addLog("Lỗi dừng máy chủ: ${e.localizedMessage}")
        }
    }

    fun getLocalWifiIpAddress(): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    (ipInt shr 8) and 0xff,
                    (ipInt shr 16) and 0xff,
                    (ipInt shr 24) and 0xff
                )
            }

            // Fallback for Hotspot or non-standard interfaces
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun addLog(message: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val log = TransferLog(
            time = timeFormat.format(Date()),
            message = message
        )
        _logs.value = listOf(log) + _logs.value.take(40)
    }
}
