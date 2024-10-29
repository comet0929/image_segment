
package com.anonymous.ctv.java.segmenter;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.anonymous.ctv.GraphicOverlay;
import com.anonymous.ctv.java.VisionProcessorBase;
import com.anonymous.ctv.preference.PreferenceUtils;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

/** A processor to run Segmenter. */
public class SegmenterProcessor extends VisionProcessorBase<SegmentationMask> {

    private static final String TAG = "SegmenterProcessor";

    private final Segmenter segmenter;

    private final String img;

    public SegmenterProcessor(Context context, String img) {
        this(context, /* isStreamMode= */ true, img);
    }

    public SegmenterProcessor(Context context, boolean isStreamMode, String img) {
        super(context);
        this.img = img;
        SelfieSegmenterOptions.Builder optionsBuilder = new SelfieSegmenterOptions.Builder();
        optionsBuilder.setDetectorMode(
                isStreamMode ? SelfieSegmenterOptions.STREAM_MODE : SelfieSegmenterOptions.SINGLE_IMAGE_MODE);
        if (PreferenceUtils.shouldSegmentationEnableRawSizeMask(context)) {
            optionsBuilder.enableRawSizeMask();
        }

        SelfieSegmenterOptions options = optionsBuilder.build();
        segmenter = Segmentation.getClient(options);
        Log.d(TAG, "SegmenterProcessor created with option: " + img);

    }

    @Override
    protected Task<SegmentationMask> detectInImage(InputImage image) {
        return segmenter.process(image);
    }

    @Override
    protected void onSuccess(
            @NonNull SegmentationMask segmentationMask, @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.add(new SegmentationGraphic(graphicOverlay, segmentationMask, img));
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Segmentation failed: " + e);
    }
}
