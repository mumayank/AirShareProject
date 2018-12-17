package mumayank.com.airshare

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Environment
import android.os.PowerManager
import android.view.WindowManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.*
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

private const val ANDROID_CONSTANT_IP_WHEN_STARTING_HOTSPOT = "192.168.43.1"
private const val ANDROID_CONSTANT_SERVER_IP_STARTS_WITH = "192.168.43."
private const val APP_FOLDER = "/AirShare"
private const val TIMEOUT = 1000 * 60 * 60 * 6 // 6 hours
private const val BUFFER = 1024

class AirShare private constructor(
    val activity: Activity,
    val commonCallbacks: CommonCallbacks,
    val networkStarterCallbacks: NetworkStarterCallbacks? = null,
    val networkJoinerCallbacks: NetworkJoinerCallbacks? = null,
    val senderCallbacks: SenderCallbacks? = null
) {

    constructor(
        activity: Activity,
        commonCallbacks: CommonCallbacks,
        networkStarterCallbacks: NetworkStarterCallbacks,
        senderCallbacks: SenderCallbacks
    ) : this(activity, commonCallbacks, networkStarterCallbacks, null, senderCallbacks)

    constructor(
        activity: Activity,
        commonCallbacks: CommonCallbacks,
        networkJoinerCallbacks: NetworkJoinerCallbacks,
        senderCallbacks: SenderCallbacks
    ) : this(activity, commonCallbacks, null, networkJoinerCallbacks, senderCallbacks)

    constructor(
        activity: Activity,
        commonCallbacks: CommonCallbacks,
        networkStarterCallbacks: NetworkStarterCallbacks
    ) : this(activity, commonCallbacks, networkStarterCallbacks, null, null)

    constructor(
        activity: Activity,
        commonCallbacks: CommonCallbacks,
        networkJoinerCallbacks: NetworkJoinerCallbacks
    ) : this(activity, commonCallbacks, null, networkJoinerCallbacks, null)

    private var wakeLock: PowerManager.WakeLock? = null
    private var clientSocket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var dataInputStream: DataInputStream? = null
    private var dataOutputStream: DataOutputStream? = null
    private var counter = -1
    private var clientTotalNumber = 0
    private var clientTotalSize = 0

    init {
        if ( (clientSocket != null) || (serverSocket != null) ) {
            terminateConnection()
        }
        if (senderCallbacks != null) {
            if (senderCallbacks.getFilesUris().size == 0) {
                senderCallbacks.onNoFilesToSend()
                terminateConnection()
            } else {
                checkPermission()
            }
        } else {
            checkPermission()
        }
    }

    interface CommonCallbacks {
        fun onWriteExternalStoragePermissionDenied()
        fun onConnected()
        fun onAllFilesSentAndReceivedSuccessfully()
    }

    interface NetworkStarterCallbacks {
        fun onServerStarted(codeForClient: String)
    }

    interface NetworkJoinerCallbacks {
        fun getCodeForClient(): String
    }

    interface SenderCallbacks {
        fun getFilesUris(): ArrayList<Uri>
        fun onNoFilesToSend()
    }

    private fun checkPermission() {
        TedPermission.with(activity)
            .setPermissionListener(object: PermissionListener {
                override fun onPermissionGranted() {
                    permissionGrantedProceed()
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    commonCallbacks.onWriteExternalStoragePermissionDenied()
                    terminateConnection()
                }
            })
            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }

    private fun permissionGrantedProceed() {
        addWakeLock()
        if (networkStarterCallbacks != null) {
            startNetwork()
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
                        commonCallbacks.onConnected()
                        startTransfer()
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
                commonCallbacks.onConnected()
                startTransfer()
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

    fun onDestroy() {
        terminateConnection()
    }

    fun terminateConnection() {
        try {
            wakeLock?.release()
            clientSocket?.close()
            serverSocket?.close()
            dataInputStream?.close()
            dataOutputStream?.close()
        } catch (e: Exception) {}
    }

    private fun startTransfer() {

        doAsync {

            dataInputStream = DataInputStream(clientSocket?.getInputStream())
            dataOutputStream = DataOutputStream(clientSocket?.getOutputStream())

            uiThread {

                // detect if sender or receiver
                if (senderCallbacks != null) {

                    // send total no. of files
                    if (dataOutputStream == null) {
                        terminateConnection()
                        return@uiThread
                    }

                    doAsync {
                        (dataOutputStream as DataOutputStream).writeUTF(senderCallbacks.getFilesUris().size.toString())
                        (dataOutputStream as DataOutputStream).flush()

                        // send total size
                        var totalSize = 0L
                        for (uri in senderCallbacks.getFilesUris()) {
                            AirShareFileProperties.extractFileProperties(activity, uri, object: AirShareFileProperties.Callbacks {
                                override fun onSuccess(fileDisplayName: String, fileSizeInMB: Long) {
                                    totalSize += fileSizeInMB
                                }

                                override fun onOperationFailed() {
                                    terminateConnection()
                                }
                            })
                        }
                        (dataOutputStream as DataOutputStream).writeUTF(totalSize.toString())
                        (dataOutputStream as DataOutputStream).flush()

                        (dataInputStream as DataInputStream).close()
                        (dataOutputStream as DataOutputStream).close()

                        uiThread {
                            // send file one by one, also send its size (we are assuming buffer size will be available)
                            sendNextFile()
                        }
                    }

                } else {

                    // receive total no. of files
                    if (dataInputStream == null) {
                        terminateConnection()
                        return@uiThread
                    }

                    doAsync {
                        clientTotalNumber = (dataInputStream as DataInputStream).readUTF().toInt()
                        clientTotalSize = (dataInputStream as DataInputStream).readUTF().toInt()

                        (dataInputStream as DataInputStream).close()
                        (dataOutputStream as DataOutputStream).close()

                        uiThread {
                            receiveNextFile()
                        }
                    }

                }
            }

        }

    }

    private fun sendNextFile() {

        doAsync {

            dataInputStream = DataInputStream(clientSocket?.getInputStream())
            dataOutputStream = DataOutputStream(clientSocket?.getOutputStream())

            uiThread {

                counter++

                if (senderCallbacks == null) {
                    terminateConnection()
                    return@uiThread
                }
                if (counter == senderCallbacks.getFilesUris().size) {
                    commonCallbacks.onAllFilesSentAndReceivedSuccessfully()
                    terminateConnection()
                    return@uiThread
                }

                // get uri
                val uri = senderCallbacks.getFilesUris().get(counter)

                // send file
                AirShareFileProperties.extractFileProperties(activity, uri, object: AirShareFileProperties.Callbacks {
                    override fun onSuccess(fileDisplayName: String, fileSizeInMB: Long) {

                        // send file name
                        if (dataOutputStream == null) {
                            terminateConnection()
                            return
                        }

                        doAsync {
                            (dataOutputStream as DataOutputStream).writeUTF(fileDisplayName)
                            (dataOutputStream as DataOutputStream).flush()
                            //(dataOutputStream as DataOutputStream).writeUTF(fileSizeInMB.toString())

                            // create input stream
                            val inputStream = activity.contentResolver.openInputStream(uri)
                            if (inputStream == null) {
                                terminateConnection()
                                return@doAsync
                            }

                            // send file
                            val byteArray = ByteArray(BUFFER)
                            var count = inputStream.read(byteArray) ?: 0
                            while (count > 0) {
                                (dataOutputStream as DataOutputStream).write(byteArray, 0, count)
                                count = inputStream.read(byteArray) ?: 0
                            }

                            (dataOutputStream as DataOutputStream).flush()
                            inputStream.close()

                            (dataInputStream as DataInputStream).close()
                            (dataOutputStream as DataOutputStream).close()

                            uiThread {
                                // send next
                                sendNextFile()
                            }

                        }

                    }

                    override fun onOperationFailed() {
                        terminateConnection()
                    }
                })
            }
        }

    }

    private fun receiveNextFile() {

        doAsync {

            dataInputStream = DataInputStream(clientSocket?.getInputStream())
            dataOutputStream = DataOutputStream(clientSocket?.getOutputStream())

            uiThread {

                counter++

                if (counter == clientTotalNumber) {
                    commonCallbacks.onAllFilesSentAndReceivedSuccessfully()
                    terminateConnection()
                    return@uiThread
                }

                // read file name
                if (dataInputStream == null) {
                    terminateConnection()
                    return@uiThread
                }

                doAsync {

                    val fileName = (dataInputStream as DataInputStream).readUTF()

                    // create output stream
                    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + APP_FOLDER)
                    path.mkdirs()
                    val file = File(path, fileName)
                    val fileOutputStream = FileOutputStream(file)

                    // receive file
                    if (dataInputStream == null) {
                        terminateConnection()
                        return@doAsync
                    }
                    val byteArray = ByteArray(BUFFER)
                    var count = (dataInputStream as DataInputStream).read(byteArray) ?: 0
                    while (count > 0) {
                        fileOutputStream.write(byteArray, 0, count)
                        count = (dataInputStream as DataInputStream).read(byteArray) ?: 0
                    }

                    fileOutputStream.flush()
                    fileOutputStream.close()

                    (dataInputStream as DataInputStream).close()
                    (dataOutputStream as DataOutputStream).close()

                    uiThread {
                        receiveNextFile()
                    }
                }
            }
        }

    }

}