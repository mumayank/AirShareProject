package mumayank.com.airshareproject

import android.app.ActionBar
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import mumayank.com.airdialog.AirDialog
import mumayank.com.airshare.AirShare

class ReceiveActivity : AppCompatActivity() {

    private var airShare: AirShare? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        AirDialog(
            this,
            "Are you both connected to a same wifi/ hotspot?",
            "This app uses wifi to transfer files. You and your friend can get connected to a same wifi. Or you can start a wifi hotspot and the friend can connect to it, Or vice-versa.",
            isCancelable = false,
            airButton1 = AirDialog.Button("WE ARE CONNECTED VIA WIFI") {

                var string = ""
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Enter OTP")
                val editText = EditText(this)
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setSingleLine()
                builder.setView(editText)
                builder.setPositiveButton("OK") { _, _ ->
                    string = editText.text.toString()
                    var progressBar: ProgressBar?= null

                    airShare = AirShare(this, object: AirShare.CommonCallbacks {
                        override fun onProgress(progressPercentage: Int) {
                            progressBar?.progress = progressPercentage
                        }

                        override fun onWriteExternalStoragePermissionDenied() {
                            Toast.makeText(this@ReceiveActivity, "Cannot continue without file writing permission access", Toast.LENGTH_SHORT).show()
                        }

                        override fun onConnected() {
                            val progressDialog = Dialog(this@ReceiveActivity)
                            progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                            progressDialog.setContentView(R.layout.progress_dialog)
                            progressBar = progressDialog.findViewById(R.id.progressBar) as ProgressBar
                            val title = progressDialog.findViewById<TextView>(R.id.title)
                            title.text = "Receiving files..."
                            val cancel = progressDialog.findViewById(R.id.cancel) as TextView
                            cancel.setOnClickListener {
                                finish()
                            }
                            progressDialog.setCancelable(false)
                            progressDialog.setCanceledOnTouchOutside(false)
                            progressDialog.show()

                            val window = progressDialog.getWindow()
                            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        }

                        override fun onAllFilesSentAndReceivedSuccessfully() {
                            Toast.makeText(this@ReceiveActivity, "All files received successfully in 'Download' folder!", Toast.LENGTH_SHORT).show()
                            finish()
                        }

                    }, object: AirShare.NetworkJoinerCallbacks {
                        override fun getCodeForClient(): String {
                            return string
                        }

                    })

                }
                builder.show()

            },
            airButton3 = AirDialog.Button("CANCEL") {
                finish()
            }
        )

    }

    override fun onDestroy() {
        super.onDestroy()
        airShare?.onDestroy()
    }
}
