package mumayank.com.airshareproject

import android.content.Context
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import mumayank.com.airshare.AirShare
import android.view.inputmethod.InputMethodManager


class MainActivity : AppCompatActivity() {

    private var airShare: AirShare? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startConnectionButton.setOnClickListener {
            val uris = ArrayList<Uri>()
            uris.add(Uri.parse(""))

            airShare = AirShare(this, uris, object: AirShare.NetworkStarterCallbacks {
                override fun onWriteExternalStoragePermissionDenied() {
                    Toast.makeText(this@MainActivity, "permission not granted", Toast.LENGTH_SHORT).show()
                }

                override fun onNoFilesToSend() {
                    Toast.makeText(this@MainActivity, "no files to send", Toast.LENGTH_SHORT).show()
                }

                override fun onServerStarted(codeForClient: String) {
                    codeTextView.text = codeForClient
                    Toast.makeText(this@MainActivity, "on server started: $codeForClient", Toast.LENGTH_SHORT).show()
                }

                override fun onClientConnected() {
                    Toast.makeText(this@MainActivity, "on client connected", Toast.LENGTH_SHORT).show()
                }

            })
        }

        joinConnectionButton.setOnClickListener {

            var string = ""
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter code")
            val editText = EditText(this)
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.setSingleLine()
            builder.setView(editText)
            builder.setPositiveButton("OK") { _, _ ->
                string = editText.text.toString()

                airShare = AirShare(this, object: AirShare.NetworkJoinerCallbacks {
                    override fun onWriteExternalStoragePermissionDenied() {
                        Toast.makeText(this@MainActivity, "permission not granted", Toast.LENGTH_SHORT).show()
                    }

                    override fun getCodeForClient(): String {
                        return string
                    }

                    override fun onConnectedToServer() {
                        Toast.makeText(this@MainActivity, "connected to server", Toast.LENGTH_SHORT).show()
                    }

                })

            }
            builder.show()
            editText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }

        terminateConnectionButton.setOnClickListener {
            airShare?.terminateConnection()
        }
    }
}
