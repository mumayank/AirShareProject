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
    val starterOfNetworkCallbacks: StarterOfNetworkCallbacks? = null,
    val joinerOfNetworkCallbacks: JoinerOfNetworkCallbacks? = null
) {

    constructor(
        activity: Activity,
        starterOfNetworkCallbacks: StarterOfNetworkCallbacks
    ) : this(activity, starterOfNetworkCallbacks, null)

    constructor(
        activity: Activity,
        joinerOfNetworkCallbacks: JoinerOfNetworkCallbacks
    ) : this(activity, null, joinerOfNetworkCallbacks)

    private var wakeLock: PowerManager.WakeLock? = null
    private var clientSocket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var dataInputStream: DataInputStream? = null
    private var dataOutputStream: DataOutputStream? = null
    private var counter = -1
    private var totalNoOfFiles = 0
    private var totalFileSize = 0L
    private var progressFileSize = 0L

    init {
        if ( (clientSocket != null) || (serverSocket != null) ) {
            terminateConnection()
        }
        if (starterOfNetworkCallbacks != null) {
            if (starterOfNetworkCallbacks.getFilesUris().size == 0) {
                starterOfNetworkCallbacks.onNoFilesToSend()
                terminateConnection()
            } else {
                checkPermission()
            }
        } else {
            checkPermission()
        }
    }

    interface StarterOfNetworkCallbacks {
        fun onWriteExternalStoragePermissionDenied()
        fun onConnected()
        fun onProgress(progressPercentage: Int)
        fun onAllFilesSentAndReceivedSuccessfully()
        fun onServerStarted(codeForClient: String)
        fun getFilesUris(): ArrayList<Uri>
        fun onNoFilesToSend()
    }

    interface JoinerOfNetworkCallbacks {
        fun onWriteExternalStoragePermissionDenied()
        fun onConnected()
        fun onProgress(progressPercentage: Int)
        fun getCodeForClient(): String
        fun onAllFilesSentAndReceivedSuccessfully()
    }

    interface SenderCallbacks {
    }

    private fun checkPermission() {
        TedPermission.with(activity)
            .setPermissionListener(object: PermissionListener {
                override fun onPermissionGranted() {
                    permissionGrantedProceed()
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    starterOfNetworkCallbacks?.onWriteExternalStoragePermissionDenied()
                    joinerOfNetworkCallbacks?.onWriteExternalStoragePermissionDenied()
                    terminateConnection()
                }
            })
            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }

    private fun permissionGrantedProceed() {
        addWakeLock()
        if (starterOfNetworkCallbacks != null) {
            startNetwork()
        } else if (joinerOfNetworkCallbacks != null) {
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

                if (starterOfNetworkCallbacks == null) {
                    terminateConnection()
                    return@uiThread
                }

                starterOfNetworkCallbacks.onServerStarted("$code$port")

                doAsync {
                    clientSocket = serverSocket?.accept()

                    if (clientSocket == null) {
                        terminateConnection()
                        return@doAsync
                    }

                    uiThread {
                        starterOfNetworkCallbacks.onConnected()
                        startTransfer()
                    }
                }
            }
        }
    }

    private fun joinNetwork() {

        if (joinerOfNetworkCallbacks == null) {
            terminateConnection()
            return
        }

        val code = joinerOfNetworkCallbacks.getCodeForClient()

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
                joinerOfNetworkCallbacks.onConnected()
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
                if (starterOfNetworkCallbacks != null) {

                    // send total no. of files
                    if (dataOutputStream == null) {
                        terminateConnection()
                        return@uiThread
                    }
                    doAsync {
                        (dataOutputStream as DataOutputStream).writeUTF(starterOfNetworkCallbacks.getFilesUris().size.toString())
                        (dataOutputStream as DataOutputStream).flush()

                        // send total file size
                        for (uri in starterOfNetworkCallbacks.getFilesUris()) {
                            AirShareFileProperties.extractFileProperties(activity, uri, object: AirShareFileProperties.Callbacks {
                                override fun onSuccess(fileDisplayName: String, fileSizeInBytes: Long, fileSizeInMB: Long) {
                                    totalFileSize += fileSizeInBytes
                                }

                                override fun onOperationFailed() {
                                    terminateConnection()
                                }
                            })
                        }
                        (dataOutputStream as DataOutputStream).writeUTF(totalFileSize.toString())
                        (dataOutputStream as DataOutputStream).flush()

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
                        totalNoOfFiles = (dataInputStream as DataInputStream).readUTF().toInt()
                        totalFileSize = (dataInputStream as DataInputStream).readUTF().toLong()

                        uiThread {
                            receiveNextFile()
                        }
                    }

                }
            }

        }

    }

    private fun sendNextFile() {

        counter++

        if (starterOfNetworkCallbacks == null) {
            terminateConnection()
            return
        }
        if (counter == starterOfNetworkCallbacks.getFilesUris().size) {
            starterOfNetworkCallbacks.onAllFilesSentAndReceivedSuccessfully()
            terminateConnection()
            return
        }

        // get next uri
        val uri = starterOfNetworkCallbacks.getFilesUris().get(counter)

        // send file
        AirShareFileProperties.extractFileProperties(activity, uri, object: AirShareFileProperties.Callbacks {
            override fun onSuccess(fileDisplayName: String, fileSizeInBytes: Long, fileSizeInMB: Long) {

                // send file name and size
                if (dataOutputStream == null) {
                    terminateConnection()
                    return
                }

                doAsync {
                    (dataOutputStream as DataOutputStream).writeUTF(fileDisplayName)
                    (dataOutputStream as DataOutputStream).flush()
                    (dataOutputStream as DataOutputStream).writeUTF(fileSizeInBytes.toString())
                    (dataOutputStream as DataOutputStream).flush()

                    // create input stream
                    val inputStream = activity.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        terminateConnection()
                        return@doAsync
                    }

                    // send file
                    val buffer = ByteArray(fileSizeInBytes.toInt())
                    inputStream.read(buffer)
                    (dataOutputStream as DataOutputStream).write(buffer, 0, fileSizeInBytes.toInt())
                    (dataOutputStream as DataOutputStream).flush()
                    inputStream.close()

                    uiThread {
                        progressFileSize += fileSizeInBytes
                        val progress = progressFileSize.toDouble() / totalFileSize.toDouble()
                        val progressPercentage = (progress * 100.toDouble()).toInt()
                        starterOfNetworkCallbacks.onProgress(progressPercentage)
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

    private fun receiveNextFile() {

        counter++

        if (joinerOfNetworkCallbacks == null) {
            terminateConnection()
            return
        }

        if (counter == totalNoOfFiles) {
            joinerOfNetworkCallbacks.onAllFilesSentAndReceivedSuccessfully()
            terminateConnection()
            return
        }

        // read file name
        if (dataInputStream == null) {
            terminateConnection()
            return
        }

        doAsync {

            val fileName = (dataInputStream as DataInputStream).readUTF()
            val fileSize = (dataInputStream as DataInputStream).readUTF().toInt()

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

            val buffer = ByteArray(fileSize)
            (dataInputStream as DataInputStream).readFully(buffer)
            fileOutputStream.write(buffer, 0, fileSize)

            fileOutputStream.flush()
            fileOutputStream.close()

            uiThread {
                progressFileSize += fileSize.toLong()
                val progress = progressFileSize.toDouble() / totalFileSize.toDouble()
                val progressPercentage = (progress * 100.toDouble()).toInt()
                joinerOfNetworkCallbacks.onProgress(progressPercentage)

                receiveNextFile()
            }
        }

    }

}