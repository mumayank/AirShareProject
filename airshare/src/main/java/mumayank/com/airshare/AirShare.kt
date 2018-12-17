package mumayank.com.airshare

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.view.WindowManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.DataInputStream
import java.io.DataOutputStream
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

private const val ANDROID_CONSTANT_IP_WHEN_STARTING_HOTSPOT = "192.168.43.1"
private const val ANDROID_CONSTANT_SERVER_IP_STARTS_WITH = "192.168.43."
private const val TIMEOUT = 1000 * 60 * 60 * 6 // 6 hours

class AirShare private constructor(
    val activity: Activity,
    val uris: ArrayList<Uri>? = null,
    val networkStarterCallbacks: NetworkStarterCallbacks? = null,
    val networkJoinerCallbacks: NetworkJoinerCallbacks? = null
) {

    constructor(
        activity: Activity,
        uris: ArrayList<Uri>,
        networkStarterCallbacks: NetworkStarterCallbacks
    ) : this(activity, uris, networkStarterCallbacks, null)

    constructor(
        activity: Activity,
        networkJoinerCallbacks: NetworkJoinerCallbacks
    ) : this(activity, null, null, networkJoinerCallbacks)

    private var wakeLock: PowerManager.WakeLock? = null
    private var clientSocket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var dataInputStream: DataInputStream? = null
    private var dataOutputStream: DataOutputStream? = null

    interface NetworkStarterCallbacks {
        fun onWriteExternalStoragePermissionDenied()
        fun onNoFilesToSend()
        fun onServerStarted(codeForClient: String)
        fun onClientConnected()
    }

    interface NetworkJoinerCallbacks {
        fun onWriteExternalStoragePermissionDenied()
        fun getCodeForClient(): String
        fun onConnectedToServer()
    }

    init {
        if ( (clientSocket != null) || (serverSocket != null) ) {
            terminateConnection()
        }
        checkPermission()
    }

    private fun checkPermission() {
        TedPermission.with(activity)
            .setPermissionListener(object: PermissionListener {
                override fun onPermissionGranted() {
                    permissionGrantedProceed()
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    if (networkStarterCallbacks != null) {
                        networkStarterCallbacks.onWriteExternalStoragePermissionDenied()
                    } else if (networkJoinerCallbacks != null) {
                        networkJoinerCallbacks.onWriteExternalStoragePermissionDenied()
                    }
                    terminateConnection()
                }
            })
            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }

    private fun permissionGrantedProceed() {
        addWakeLock()
        if (networkStarterCallbacks != null) {
            if ( (uris == null) || (uris.size == 0) ) {
                networkStarterCallbacks.onNoFilesToSend()
                terminateConnection()
            } else {
                startNetwork()
            }
        } else if (networkJoinerCallbacks != null) {
            joinNetwork()
        } else {
            terminateConnection()
        }
    }

    private fun addWakeLock() {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        wakeLock =
                (activity.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                    newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                        acquire(TIMEOUT.toLong())
                    }
                }
    }

    private fun startNetwork() {
        doAsync {

            var port = Random.nextInt(1000, 9999)
            while (serverSocket == null) {
                try {
                    serverSocket = ServerSocket(port)
                } catch (e: Exception) {
                    port = Random.nextInt(1000, 9999)
                    serverSocket = null
                }
            }

            serverSocket?.soTimeout = TIMEOUT
            uiThread {

                val ip = getIpAddress(activity)
                val index = ip.lastIndexOf(".")
                var code = ip.substring(index + 1, ip.length)
                if (code == "0") {
                    code = ""
                }

                if (networkStarterCallbacks == null) {
                    terminateConnection()
                    return@uiThread
                }

                networkStarterCallbacks.onServerStarted("$code$port")

                doAsync {
                    clientSocket = serverSocket?.accept()

                    if (clientSocket == null) {
                        terminateConnection()
                        return@doAsync
                    }

                    uiThread {
                        networkStarterCallbacks.onClientConnected()

                        doAsync {
                            dataInputStream = DataInputStream(clientSocket?.getInputStream())
                            dataOutputStream = DataOutputStream(clientSocket?.getOutputStream())
                            if ( (clientSocket == null) || (dataInputStream == null) || (dataOutputStream == null) ) {
                                terminateConnection()
                                return@doAsync
                            } else {
                                // todo start sending on background
                            }
                        }
                    }
                }
            }
        }
    }

    private fun joinNetwork() {

        if (networkJoinerCallbacks == null) {
            terminateConnection()
            return
        }

        val code = networkJoinerCallbacks.getCodeForClient()

        val port = code.substring(code.length - 4, code.length)
        val endIp = code.substring(0, code.length - 4)
        var serverIp = ""
        if (endIp == "") {
            serverIp = ANDROID_CONSTANT_IP_WHEN_STARTING_HOTSPOT
        } else {
            val ip = getIpAddress(activity)
            val index = ip.lastIndexOf(".")
            val pendingIp = ip.substring(0, index + 1)
            if (pendingIp == "0.0.0.") {
                serverIp = ANDROID_CONSTANT_SERVER_IP_STARTS_WITH + endIp
            } else {
                serverIp = pendingIp + endIp
            }
        }

        doAsync {
            clientSocket = Socket(serverIp, port.toInt())

            if (clientSocket == null) {
                terminateConnection()
                return@doAsync
            }

            uiThread {
                networkJoinerCallbacks.onConnectedToServer()

                // todo start receiving on background
            }
        }
    }

    companion object {

        private fun getIpAddress(context: Context): String {
            val wifiMan = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInf = wifiMan.connectionInfo
            val ipAddress = wifiInf.ipAddress
            val ip = String.format("%d.%d.%d.%d", ipAddress and 0xff, ipAddress shr 8 and 0xff, ipAddress shr 16 and 0xff, ipAddress shr 24 and 0xff)
            return ip
        }

    }

    fun terminateConnection() {
        wakeLock?.release()
        clientSocket?.close()
        serverSocket?.close()
        dataInputStream?.close()
        dataOutputStream?.close()
    }

}