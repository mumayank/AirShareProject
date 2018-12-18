package mumayank.com.airshareproject;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import mumayank.com.airshare.AirShare;
import mumayank.com.airshare.AirShareAddFile;
import mumayank.com.airshare.AirShareFileProperties;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class JavaExampleActivity extends AppCompatActivity {

    // INCLUDE IN TOP
    private AirShare airShare;
    private AirShareAddFile airShareAddFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_example);

        // SENDER CODE
        airShare = new AirShare(this, new AirShare.StarterOfNetworkCallbacks() {
            @Override
            public void onWriteExternalStoragePermissionDenied() {

            }

            @Override
            public void onConnected() {

            }

            @Override
            public void onProgress(int progressPercentage) {

            }

            @Override
            public void onAllFilesSentAndReceivedSuccessfully() {

            }

            @Override
            public void onServerStarted(@NotNull String codeForClient) {

            }

            @NotNull
            @Override
            public ArrayList<Uri> getFilesUris() {
                return null;
            }

            @Override
            public void onNoFilesToSend() {

            }
        });

        // RECEIVER CODE
        airShare = new AirShare(this, new AirShare.JoinerOfNetworkCallbacks() {
            @Override
            public void onWriteExternalStoragePermissionDenied() {

            }

            @Override
            public void onConnected() {

            }

            @Override
            public void onProgress(int progressPercentage) {

            }

            @NotNull
            @Override
            public String getCodeForClient() {
                return null;
            }

            @Override
            public void onAllFilesSentAndReceivedSuccessfully() {

            }
        });

        // ADD FILE CODE
        airShareAddFile = new AirShareAddFile(this, new AirShareAddFile.Callbacks() {
            @Override
            public void onSuccess(@NotNull Uri uri) {

            }

            @Override
            public void onFileAlreadyAdded() {

            }
        }, new ArrayList<Uri>());

        // EXTRACT FILE PROPERTIES CODE
        Uri uri = Uri.parse("");
        AirShareFileProperties.Companion.extractFileProperties(this, uri, new AirShareFileProperties.Callbacks() {
            @Override
            public void onSuccess(@NotNull String fileDisplayName, long fileSizeInBytes, long fileSizeInMB) {

            }

            @Override
            public void onOperationFailed() {

            }
        });
    }

    // MUST OVERRIDE LIKE THIS IF CALLING AIR SHARE ADD FILE
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (airShareAddFile != null) {
            airShareAddFile.onActivityResult(requestCode, resultCode, data);
        }
    }

    // MUST OVERRIDE LIKE THIS IF USING AIR SHARE
    @Override
    protected void onDestroy() {
        if (airShare != null) {
            airShare.onDestroy();
        }
        super.onDestroy();
    }
}
