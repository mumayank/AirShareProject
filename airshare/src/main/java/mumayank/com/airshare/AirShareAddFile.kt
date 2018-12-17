package mumayank.com.airshare

import android.app.Activity
import android.content.Intent
import android.net.Uri

private const val SELECT_FILES = 1243

class AirShareAddFile {

    interface Callbacks {
        fun onSuccess(addFileUris: ArrayList<Uri>)
        fun onFileAlreadyAdded()
    }

    private var addFileUris = ArrayList<Uri>()
    private var callbacks: Callbacks? = null

    fun addFile(activity: Activity, callbacks: Callbacks, addFileUris: ArrayList<Uri> = ArrayList()) {
        this.addFileUris = addFileUris
        this.callbacks = callbacks

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
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
                    addFileUris.add(uri)
                    callbacks?.onSuccess(addFileUris)
                }
            }
        }
    }

}