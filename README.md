# AirShare
[![](https://jitpack.io/v/mumayank/AirShareProject.svg)](https://jitpack.io/#mumayank/AirShareProject)

An Android library to make it super easy to include '**share files without internet**' feature inside your app

![](https://images.unsplash.com/photo-1521575107034-e0fa0b594529)

+ Uses tcp socket over wifi internally
+ Works when either both phones are connected to the same wifi, or when one phone starts wifi hotspot and another connects to it, or vice-versa
+ Provides helper methods to display choose files UI
+ Provides helper methods to extract information out of file Uri
+ Callbacks for progress updates
+ 100% written in Kotlin
+ Full Java support

# Screenshots

|   |  |
| ------------- | ------------- |
| ![](https://github.com/mumayank/AirShareProject/blob/master/img/1.jpg)  | ![](https://github.com/mumayank/AirShareProject/blob/master/img/2.jpg)  |
| ![](https://github.com/mumayank/AirShareProject/blob/master/img/3.jpg)  | ![](https://github.com/mumayank/AirShareProject/blob/master/img/4.jpg)  |
| ![](https://github.com/mumayank/AirShareProject/blob/master/img/5.jpg)  | ![](https://github.com/mumayank/AirShareProject/blob/master/img/6.jpg)  |
| ![](https://github.com/mumayank/AirShareProject/blob/master/img/7.jpg)  | ![](https://github.com/mumayank/AirShareProject/blob/master/img/8.jpg)  |6.png "Logo")  |

# Usage

Wherever you want to send files:
1. Declare `airShare` object variable inside the `activity`
2. Init the variable whenever user wants to send the files
3. Clean up on `onDestroy`

```kotlin
class SendActivity : AppCompatActivity() {
  
    private var airShare: AirShare? = null // ADD THIS LINE ON TOP
    
    ...
    
    // PRE REQUISITE IS USERS MUST BE ON SAME WIFI (ONE USER STARTING HOTSPOT AND ANOTHER JOINING IT IS ALSO OK)
    // SINCE THERE IS NO APIs TO DO THIS, DEV CAN ONLY CONFIRM FROM THE USER ABOUT THIS
    // TO TRANSFER FILES, INITIALIZE AIRSHARE OBJECT:
    
    airShare = AirShare(this, object: AirShare.StarterOfNetworkCallbacks {
    
                        override fun onWriteExternalStoragePermissionDenied() {
                            // INFORM USER THIS PERMISSION IS NECESSARY, AND TRY AGAIN                        
                        }

                        override fun onConnected() {
                            // INFORM USER THAT FILE TRANSFER IS STARTING NOW
                        }

                        override fun onProgress(progressPercentage: Int) {
                            // INFORM USER ABOUT THIS PROGRESS UPDATE, LIKE UPDATE THE PROGRESS BAR
                        }

                        override fun onAllFilesSentAndReceivedSuccessfully() {
                            // INFORM USER THAT ALL FILES ARE TRANSFERRED/ RECEIVED
                        }

                        override fun onServerStarted(codeForClient: String) {
                            // THE OTHER PERSON MUST ENTER THIS OTP (codeForClient) IN THEIR APP TO ESTABLISH CONNECTION
                        }

                        override fun getFilesUris(): ArrayList<Uri> {
                            // RETURN YOUR ARRAY LIST OF FILE URIs
                        }

                        override fun onNoFilesToSend() {
                            // INFORM USER TO SELECT SOME FILES FIRST, AND TRY AGAIN
                        }

                    })
                 
      // INCLUDE THIS IN THE END
      override fun onDestroy() {
          airShare?.onDestroy()       // THIS LINE
          super.onDestroy()
      }
}
```

Wherever you want to receive files:
1. Declare `airShare` object variable inside the `activity`
2. Init the variable whenever user wants to send the files
3. Clean up on `onDestroy`

```kotlin
class ReceiveActivity : AppCompatActivity() {

    private var airShare: AirShare? = null
    
    ...
    
    airShare = AirShare(this, object: AirShare.CommonCallbacks {
                    override fun onProgress(progressPercentage: Int) {
                        // INFORM USER ABOUT THIS PROGRESS UPDATE, LIKE UPDATE THE PROGRESS BAR
                    }

                    override fun onWriteExternalStoragePermissionDenied() {
                        // INFORM USER THIS PERMISSION IS NECESSARY, AND TRY AGAIN
                    }

                    override fun onConnected() {
                        // INFORM USER THAT FILE TRANSFER IS STARTING NOW
                    }

                    override fun onAllFilesSentAndReceivedSuccessfully() {
                        // INFORM USER THAT ALL FILES ARE TRANSFERRED/ RECEIVED
                    }

                }, object: AirShare.NetworkJoinerCallbacks {
                    override fun getCodeForClient(): String {
                        // RETURN THE OTP RECEIVED BY THE SENDER IN THEIR APP TO ESTABLISH CONNECTION
                    }

                })
                
    airShare = AirShare(this, object: AirShare.JoinerOfNetworkCallbacks {
    
                override fun onWriteExternalStoragePermissionDenied() {
                    // INFORM USER THIS PERMISSION IS NECESSARY, AND TRY AGAIN
                }

                override fun onConnected() {
                    // INFORM USER THAT FILE TRANSFER IS STARTING NOW
                }

                override fun onProgress(progressPercentage: Int) {
                    // INFORM USER ABOUT THIS PROGRESS UPDATE, LIKE UPDATE THE PROGRESS BAR
                }

                override fun getCodeForClient(): String {
                    // RETURN THE OTP RECEIVED BY THE SENDER IN THEIR APP TO ESTABLISH CONNECTION
                }

                override fun onAllFilesSentAndReceivedSuccessfully() {
                    // INFORM USER THAT ALL FILES ARE TRANSFERRED/ RECEIVED
                }

            })

    // INCLUDE THIS IN THE END
    override fun onDestroy() {
        airShare?.onDestroy()       // THIS LINE
        super.onDestroy()
    }
```

# Helpers

The library also provides helper methods. Such as:
+ File chooser ([using storage access network](https://developer.android.com/guide/topics/providers/document-provider))
+ File properties extractor

Whenever you want to user file chooser:
1. Declare `airShareAddFile` object variable inside the `activity`
2. Init the variable whenever user wants to choose more file
3. Override `onActivityResult` to call `airShareAddFile`'s method by the same name

```kotlin
class SendActivity : AppCompatActivity() {
  
    private var airShareAddFile: AirShareAddFile? = null // ADD THIS LINE ON TOP
    
    ...
    
    // WHEN USER WANTS TO ADD MORE FILE:
    airShareAddFile = AirShareAddFile(this, object: AirShareAddFile.Callbacks {
                override fun onSuccess(uri: Uri) {
                    // DO SOMETHING WITH THIS URI
                }

                override fun onFileAlreadyAdded() {
                    // IF USER HAS SELECTED A PREVIOUSLY SELECTED FILE, THIS CALLBACK WOULD BE FIRED UP
                }

            }, myFileUriArrayList) // WHERE myFileUriArrayList IS AN ARRAY LIST OF URIs OF FILEs SELECTED TILL NOW BY USER
                  
                  
    // INCLUDE THIS IN THE END
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        airShareAddFile?.onActivityResult(requestCode, resultCode, data)
    }
    
}
```

To extract properties from a file's uri:

```kotlin

AirShareFileProperties.extractFileProperties(this, uri, object: AirShareFileProperties.Callbacks {
                        override fun onSuccess(fileDisplayName: String, fileSizeInBytes: Long, fileSizeInMB: Long) {
                            // DO SOMETHING
                        }

                        override fun onOperationFailed() {
                            // IDEALLY SPEAKING, SHOULD NEVER OCCUR. BUT STILL, FOR SAFETY, HANDLE IT JUST IN CASE.
                        }

                    })

```
# Java

Java usage remains exactly the same as stated above. This section is a guidance for Java-devs:
```java
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
```

# Setup

Add this line in your root build.gradle at the end of repositories:

```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' } // this line
  }
}
  ```
Add this line in your app build.gradle:
```gradle
dependencies {
  implementation 'com.github.mumayank:AirShareProject:LATEST_VERSION' // this line
}
```
where LATEST_VERSION is [![](https://jitpack.io/v/mumayank/AirShareProject.svg)](https://jitpack.io/#mumayank/AirShareProject)
