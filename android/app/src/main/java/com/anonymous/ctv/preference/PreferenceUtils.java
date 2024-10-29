package com.anonymous.ctv.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import com.google.android.gms.common.images.Size;
import com.google.common.base.Preconditions;
import com.anonymous.ctv.CameraSource;
import com.anonymous.ctv.CameraSource.SizePair;
import com.anonymous.ctv.R;

/** Utility class to retrieve shared preferences. */
public class PreferenceUtils {

    @Nullable
    public static SizePair getCameraPreviewSizePair(Context context, int cameraId) {
        Preconditions.checkArgument(
                cameraId == CameraSource.CAMERA_FACING_BACK
                        || cameraId == CameraSource.CAMERA_FACING_FRONT);
        String previewSizePrefKey;
        String pictureSizePrefKey;
        if (cameraId == CameraSource.CAMERA_FACING_BACK) {
            previewSizePrefKey = context.getString(R.string.pref_key_rear_camera_preview_size);
            pictureSizePrefKey = context.getString(R.string.pref_key_rear_camera_picture_size);
        } else {
            previewSizePrefKey = context.getString(R.string.pref_key_front_camera_preview_size);
            pictureSizePrefKey = context.getString(R.string.pref_key_front_camera_picture_size);
        }

        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            return new SizePair(
                    Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)),
                    Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null)));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean shouldHideDetectionInfo(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(R.string.pref_key_info_hide);
        return sharedPreferences.getBoolean(prefKey, false);
    }

    public static boolean shouldSegmentationEnableRawSizeMask(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(R.string.pref_key_segmentation_raw_size_mask);
        return sharedPreferences.getBoolean(prefKey, false);
    }

    public static boolean isCameraLiveViewportEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(R.string.pref_key_camera_live_viewport);
        return sharedPreferences.getBoolean(prefKey, false);
    }

    private PreferenceUtils() {}
}
