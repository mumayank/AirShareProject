package mumayank.com.airshare

import android.app.Activity
import android.content.Intent
import android.net.Uri

private const val SELECT_FILES = 1243

class AirShareAddFile(activity: Activity, callbacks: Callbacks, addFileUris: ArrayList<Uri> = ArrayList()) {

    interface Callbacks {
        fun onSuccess(uri: Uri)
        fun onFileAlreadyAdded()
    }

    private var addFileUris = ArrayList<Uri>()
    private var callbacks: Callbacks? = null

    init {
        this.addFileUris = addFileUris
        this.callbacks = callbacks

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "*/*"
        }
        activity.startActivityForResult(intent, SELECT_FILES)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_FILES && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                if (addFileUris.contains(uri)) {
                    callbacks?.onFileAlreadyAdded()
                } else {
                    callbacks?.onSuccess(uri)
                }
            }
        }
    }

}