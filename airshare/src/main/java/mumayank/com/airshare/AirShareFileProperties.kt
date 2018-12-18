package mumayank.com.airshare

import android.app.Activity
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns

class AirShareFileProperties {
    interface Callbacks {
        fun onSuccess(fileDisplayName: String, fileSizeInBytes: Long, fileSizeInMB: Long)
        fun onOperationFailed()
    }

    companion object {
        fun extractFileProperties(activity: Activity, fileUri: Uri, callbacks: Callbacks) {
            val cursor: Cursor? = activity.contentResolver.query(fileUri, null, null, null, null, null)
            if (cursor == null) {
                callbacks.onOperationFailed()
            } else {
                cursor.use {
                    if (it.moveToFirst()) {
                        val displayName: String = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                        var size = 0L
                        var originalSize = 0L
                        if (!it.isNull(sizeIndex)) {
                            originalSize = it.getLong(sizeIndex)
                            size = (originalSize/1024L)/1024L
                        }
                        callbacks.onSuccess(displayName, originalSize, size)
                    } else {
                        callbacks.onOperationFailed()
                    }
                    cursor.close()
                }
            }
        }

    }
}