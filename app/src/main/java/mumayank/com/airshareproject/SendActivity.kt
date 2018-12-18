package mumayank.com.airshareproject

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_server.*
import kotlinx.android.synthetic.main.item.view.*
import mumayank.com.airdialog.AirDialog
import mumayank.com.airrecyclerview.AirRecyclerView
import mumayank.com.airshare.AirShare
import mumayank.com.airshare.AirShareAddFile
import mumayank.com.airshare.AirShareFileProperties

class SendActivity : AppCompatActivity() {

    private lateinit var rvAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private val uris = ArrayList<Uri>()
    private var total = 0L
    private var airShareAddFile: AirShareAddFile? = null
    private var airShare: AirShare? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = "Air Share : Send"

        defineRv()

        addFile.setOnClickListener {
            airShareAddFile = AirShareAddFile(this, object: AirShareAddFile.Callbacks {
                override fun onSuccess(uri: Uri) {
                    uris.add(uri)
                    rvAdapter.notifyItemInserted(uris.size - 1)
                }

                override fun onFileAlreadyAdded() {
                    Toast.makeText(this@SendActivity, "File already added", Toast.LENGTH_SHORT).show()
                }

            })
        }

        clearAll.setOnClickListener {
            if (uris.size == 0) {
                Toast.makeText(this, "No files to remove", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AirDialog(
                this,
                "Sure to remove all files from this list?",
                airButton1 = AirDialog.Button("REMOVE ALL") {
                    val count = uris.size
                    uris.clear()
                    rvAdapter.notifyItemRangeRemoved(0, count)
                },
                airButton2 = AirDialog.Button("CANCEL") {
                    // do nothing
                }
            )
        }

        send.setOnClickListener {

            var airDialog: AirDialog? = null

            AirDialog(
                this,
                "Are you both connected to a same wifi/ hotspot?",
                "This app uses wifi to transfer files. You and your friend can get connected to a same wifi. Or you can start a wifi hotspot and the friend can connect to it, Or vice-versa.",
                isCancelable = false,
                airButton1 = AirDialog.Button("WE ARE CONNECTED VIA WIFI") {
                    airShare = AirShare(this, object: AirShare.CommonCallbacks {
                        override fun onWriteExternalStoragePermissionDenied() {
                            Toast.makeText(this@SendActivity, "Cannot continue without file writing permission access", Toast.LENGTH_SHORT).show()
                        }

                        override fun onConnected() {
                            airDialog?.dismiss()
                            AirDialog(
                                this@SendActivity,
                                "Sending files...",
                                isCancelable = false,
                                airButton3 = AirDialog.Button("CANCEL") {
                                    finish()
                                }
                            )
                        }

                        override fun onAllFilesSentAndReceivedSuccessfully() {
                            Toast.makeText(this@SendActivity, "All files sent successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        }

                    }, object: AirShare.NetworkStarterCallbacks {
                        override fun onServerStarted(codeForClient: String) {
                            airDialog = AirDialog(
                                this@SendActivity,
                                "OTP: $codeForClient",
                                "Your friend will need this to join your connection",
                                isCancelable = false,
                                airButton3 = AirDialog.Button("CANCEL") {
                                    finish()
                                }
                            )
                        }

                    }, object: AirShare.SenderCallbacks {
                        override fun getFilesUris(): ArrayList<Uri> {
                            return uris
                        }

                        override fun onNoFilesToSend() {
                            Toast.makeText(this@SendActivity, "No files to send!", Toast.LENGTH_SHORT).show()
                        }

                    })

                },
                airButton3 = AirDialog.Button("CANCEL") {
                    finish()
                }
            )

        }
    }

    override fun onDestroy() {
        airShare?.onDestroy()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        finish()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        airShareAddFile?.onActivityResult(requestCode, resultCode, data)
    }

    private fun defineRv() {
        rvAdapter = AirRecyclerView.initAndGetAdapter(this, rv, object: AirRecyclerView.AirRecyclerViewCallbacks {
            override fun getBindView(viewHolder: RecyclerView.ViewHolder, viewType: Int, position: Int) {
                val customViewHolder = viewHolder as CustomViewHolder
                val uri = uris[position]

                AirShareFileProperties.extractFileProperties(this@SendActivity, uri, object: AirShareFileProperties.Callbacks {
                    override fun onSuccess(fileDisplayName: String, fileSizeInBytes: Long, fileSizeInMB: Long) {
                        customViewHolder.fileNameTextView.text = fileDisplayName
                        customViewHolder.fileSizeTextView.text = "$fileSizeInMB MB"
                        total += fileSizeInMB
                        updateTotal()
                    }

                    override fun onOperationFailed() {
                        // should not occur. If it does, ignore.
                    }

                })

                customViewHolder.deleteImageView.setOnClickListener {
                    val index = uris.indexOf(uri)

                    AirShareFileProperties.extractFileProperties(this@SendActivity, uri, object: AirShareFileProperties.Callbacks {
                        override fun onSuccess(fileDisplayName: String, fileSizeInBytes: Long, fileSizeInMB: Long) {
                            total -= fileSizeInMB
                            updateTotal()
                            uris.removeAt(index)
                            rvAdapter.notifyItemRemoved(index)
                        }

                        override fun onOperationFailed() {
                            // should not occur. If it does, ignore.
                        }

                    })
                }
            }

            override fun getSize(): Int {
                return uris.size
            }

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                return CustomViewHolder(view)
            }

            override fun getViewLayout(viewType: Int): Int {
                return R.layout.item
            }

            override fun getViewType(position: Int): Int {
                return 0
            }

        })
    }

    private fun updateTotal() {
        totalTextView.text = total.toString() + " MB IN TOTAL"
    }

    class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileNameTextView: TextView = view.fileNameTextView
        val fileSizeTextView: TextView = view.fileSizeTextView
        val deleteImageView: ImageView = view.deleteImageView
    }

}