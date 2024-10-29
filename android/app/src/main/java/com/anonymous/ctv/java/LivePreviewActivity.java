package com.anonymous.ctv.java;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.annotation.KeepName;
import com.anonymous.ctv.CameraSource;
import com.anonymous.ctv.CameraSourcePreview;
import com.anonymous.ctv.GraphicOverlay;
import com.anonymous.ctv.R;
import com.anonymous.ctv.java.segmenter.SegmenterProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.media.MediaPlayer;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;

import android.view.View;
import android.view.ViewGroup;


import android.os.Handler;
import android.widget.Button;


import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderCodecInfo;
import com.hbisoft.hbrecorder.HBRecorderListener;


import android.content.ContentResolver;
import android.content.ContentValues;

import android.content.SharedPreferences;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;

import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;


import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.widget.SwitchCompat;

import java.text.SimpleDateFormat;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.sql.Date;
import java.util.Locale;

import androidx.constraintlayout.widget.ConstraintLayout;


@KeepName
public final class LivePreviewActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, HBRecorderListener {
    private static final String SELFIE_SEGMENTATION = "Selfie Segmentation";

    private static final String TAG = "LivePreviewActivity";

    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private String selectedModel = SELFIE_SEGMENTATION;
    private String bg_img = "";
    private static final int PERMISSION_REQUESTS = 1;
    private static final String[] REQUIRED_RUNTIME_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private HBRecorder hbRecorder;
    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_POST_NOTIFICATIONS = 33;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private static final int PERMISSION_REQ_ID_FOREGROUND_SERVICE_MEDIA_PROJECTION = PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE + 1;
    private boolean hasPermissions = false;
    private Button recordButton;
    private ToggleButton facingSwitch;
    private ConstraintLayout mainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        Toast.makeText(getApplicationContext(), "Tap on the screen to stop after you start the recording", 3000).show();

        Intent intent = getIntent();
        String color = intent.getStringExtra("color");
        if (color != null) {
            setColor(color); // Call setColor with the passed color
        }

        setContentView(R.layout.activity_vision_live_preview);

        hbRecorder = new HBRecorder(this, this);

        preview = findViewById(R.id.preview_view);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }

        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions();
        }

        facingSwitch = findViewById(R.id.facing_switch);
        mainView = findViewById(R.id.main_view);

        facingSwitch.setOnCheckedChangeListener(this);


        createCameraSource(selectedModel);

        recordButton = findViewById(R.id.recordButton);
        setOnClickListeners();

        mainView.setOnClickListener(v -> {
            if (hasPermissions) {
                if (hbRecorder.isBusyRecording()) {
                    hbRecorder.stopScreenRecording();
                    recordButton.setAlpha(1f);
                    facingSwitch.setVisibility(View.VISIBLE);
                    recordButton.setText("REC");
                }
            }
        });

    }


    private void createCameraSource(String model) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        try {
            cameraSource.setMachineLearningFrameProcessor(new SegmenterProcessor(this, bg_img));
        } catch (RuntimeException e) {
            Log.e(TAG, "Can not create image processor: " + model, e);
            Toast.makeText(
                            getApplicationContext(),
                            "Can not create image processor: " + e.getMessage(),
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "Set facing");
        if (cameraSource != null) {
            if (isChecked) {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
            } else {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
            }
        }
        preview.stop();
        startCameraSource();
    }


    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }

                preview.start(cameraSource, graphicOverlay);
                // startRecording();
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        createCameraSource(selectedModel);
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
        //stopRecording();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
            //stopRecording();
        }
    }

    // Check if all runtime permissions are granted
    private boolean allRuntimePermissionsGranted() {
        for (String permission : REQUIRED_RUNTIME_PERMISSIONS) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    // Request the necessary runtime permissions
    private void getRuntimePermissions() {
        List<String> permissionsToRequest = new ArrayList<String>();
        for (String permission : REQUIRED_RUNTIME_PERMISSIONS) {
            if (!isPermissionGranted(this, permission)) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUESTS
            );
        }
    }

    // Helper function to check if a permission is granted
    private boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    public void setColor(String color) {
        Log.i(TAG, "Color setting: " + color);
        // Your color logic
        bg_img = color;
    }

    @Override
    public void HBRecorderOnStart() {
        Log.e("HBRecorder", "HBRecorderOnStart called");
    }

    @Override
    public void HBRecorderOnComplete() {
        //After file was created
        Toast.makeText(getApplicationContext(), "Video file is saved in your storage", 3000).show();
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        //When an error occurs
    }

    @Override
    public void HBRecorderOnPause() {
        //When recording was paused
    }

    @Override
    public void HBRecorderOnResume() {
        //When recording was resumed
    }

    //Start Button OnClickListener
    private void setOnClickListeners() {
        recordButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // first check if permissions were granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // SDK 34
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS, PERMISSION_REQ_POST_NOTIFICATIONS)
                            && checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)
                            && checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION, PERMISSION_REQ_ID_FOREGROUND_SERVICE_MEDIA_PROJECTION)) {
                        hasPermissions = true;
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // SDK 33
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS, PERMISSION_REQ_POST_NOTIFICATIONS)
                            && checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
                        hasPermissions = true;
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
                        hasPermissions = true;
                    }
                } else {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)
                            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
                        hasPermissions = true;
                    }
                }

                if (hasPermissions) {
                    // check if recording is in progress and stop it if it is
                    if (hbRecorder.isBusyRecording()) {
                        hbRecorder.stopScreenRecording();
                        recordButton.setAlpha(1f);
                        facingSwitch.setVisibility(View.VISIBLE);
                        recordButton.setText("REC");
                    } else {
                        startRecordingScreen();
//                        Toast.makeText(getApplicationContext(), "Tap on the screen to stop recording ", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Log.e("HBRecorder", "Less than API 21");
            }
        });
    }

    private void createFolder() {
        File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HBRecorder");
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created");
            }
        }
    }

    private void refreshGalleryFile() {
        MediaScannerConnection.scanFile(this,
                new String[]{hbRecorder.getFilePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void updateGalleryUri() {
        contentValues.clear();
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
        getContentResolver().update(mUri, contentValues, null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startRecordingScreen() {

        quickSettings();
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
        recordButton.setAlpha(0f);
        facingSwitch.setVisibility(View.GONE);
        recordButton.setText("STOP");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void quickSettings() {
        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.recordHDVideo(true);
        hbRecorder.isAudioEnabled(true);
        hbRecorder.setNotificationTitle("Recording Screen");
        hbRecorder.setNotificationDescription("Recording Current Screen");
    }

    //Check if permissions was granted
    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQ_POST_NOTIFICATIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO);
                } else {
                    hasPermissions = false;
                    showLongToast("No permission for " + Manifest.permission.POST_NOTIFICATIONS);
                }
                break;
            case PERMISSION_REQ_ID_RECORD_AUDIO:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
                } else {
                    hasPermissions = false;
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                }
                break;
            case PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    hasPermissions = true;
                    startRecordingScreen();
                } else {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION, PERMISSION_REQ_ID_FOREGROUND_SERVICE_MEDIA_PROJECTION);
                    } else {
                        hasPermissions = false;
                        showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
                break;
            case PERMISSION_REQ_ID_FOREGROUND_SERVICE_MEDIA_PROJECTION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermissions = true;
                    startRecordingScreen();
                } else {
                    hasPermissions = false;
                    showLongToast("No permission for " + Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    //Set file path or Uri depending on SDK version
                    setOutputPath();
                    //Start screen recording
                    hbRecorder.startScreenRecording(data, resultCode);

                }
            }
        }
    }

    ContentResolver resolver;
    ContentValues contentValues;
    Uri mUri;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setOutputPath() {
        String filename = generateFileName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = getContentResolver();
            contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "HBRecorder");
            contentValues.put(MediaStore.Video.Media.TITLE, filename);
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            //FILE NAME SHOULD BE THE SAME
            hbRecorder.setFileName(filename);
            hbRecorder.setOutputUri(mUri);
        } else {
            createFolder();
            hbRecorder.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/HBRecorder");
        }
    }

    //Generate a timestamp to be used as a file name
    private String generateFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate).replace(" ", "");
    }

    //Show Toast
    private void showLongToast(final String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

}
