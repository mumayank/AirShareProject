package mumayank.com.airshareproject

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
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

            AirDialog.show(
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
            AirDialog.show(
                this,
                "Are you both connected to a same wifi/ hotspot?",
                "This app uses wifi to transfer files. You and your friend can get connected to a same wifi. Or you can start a wifi hotspot and the friend can connect to it, Or vice-versa.",
                isCancelable = false,
                airButton1 = AirDialog.Button("WE ARE CONNECTED VIA WIFI") {

                    AirDialog.show(
                        this,
                        "Make a choice",
                        "Among you two, one person needs to start the connection while the other needs to join that connection",
                        isCancelable = false,
                        airButton1 = AirDialog.Button("I WILL START") {



                        },
                        airButton2 = AirDialog.Button("I WILL JOIN") {



                        },
                        airButton3 = AirDialog.Button("CANCEL") {
                            finish()
                        }
                    )

                },
                airButton3 = AirDialog.Button("CANCEL") {
                    finish()
                }
            )


            airShare = AirShare(this, uris, object: AirShare.NetworkStarterCallbacks {
                override fun onWriteExternalStoragePermissionDenied() {
                    Toast.makeText(this@SendActivity, "Please grant permission to continue", Toast.LENGTH_SHORT).show()
                }

                override fun onNoFilesToSend() {
                    Toast.makeText(this@SendActivity, "No files to send", Toast.LENGTH_SHORT).show()
                }

                override fun onServerStarted(codeForClient: String) {
                    AirDialog.show(
                        this@SendActivity,
                        "$codeForClient is the OTP for receiver",
                        "Receiver must enter this OTP in their app to continue",
                        isCancelable = false,
                        airButton1 = AirDialog.Button("DONE") {

                        },
                        airButton3 = AirDialog.Button("CANCEL") {
                            finish()
                        }
                    )
                    Toast.makeText(this@SendActivity, "Server started", Toast.LENGTH_SHORT).show()
                }

                override fun onClientConnected() {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun getFileUris(): ArrayList<Uri> {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onAllFilesSentSuccessfully() {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

            })
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
                    override fun onSuccess(fileDisplayName: String, fileSizeInMB: Long) {
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
                        override fun onSuccess(fileDisplayName: String, fileSizeInMB: Long) {
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