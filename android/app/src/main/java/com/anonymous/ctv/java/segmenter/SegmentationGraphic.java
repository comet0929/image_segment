
package com.anonymous.ctv.java.segmenter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import androidx.annotation.ColorInt;
import com.anonymous.ctv.GraphicOverlay;
import com.anonymous.ctv.GraphicOverlay.Graphic;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import java.nio.ByteBuffer;
import android.util.Log;
import android.content.Context;
import android.graphics.Bitmap.Config;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
/** Draw the mask from SegmentationResult in preview. */

public class SegmentationGraphic extends Graphic {

    private final ByteBuffer mask;
    private final int maskWidth;
    private final int maskHeight;
    private final boolean isRawSizeMaskEnabled;
    private final float scaleX;
    private final float scaleY;
    int[] bg_colors = new int[0];

    private static String bg_img = "";

    private static Bitmap originalBitmap;
    private final RenderScript renderScript;
    public SegmentationGraphic(GraphicOverlay overlay, SegmentationMask segmentationMask, String bg_img, Context context) {
        super(overlay);
        String bg = "/data/user/0/com.anonymous.ctv/cache/" + bg_img;
        this.renderScript = RenderScript.create(context);

        mask = segmentationMask.getBuffer();
        maskWidth = segmentationMask.getWidth();
        maskHeight = segmentationMask.getHeight();

        isRawSizeMaskEnabled =
                maskWidth != overlay.getImageWidth()
                        || maskHeight != overlay.getImageHeight();
        scaleX = overlay.getImageWidth() * 1f / maskWidth;
        scaleY = overlay.getImageHeight() * 1f / maskHeight;

        if(!SegmentationGraphic.bg_img.equals(bg)){
            SegmentationGraphic.bg_img = "/data/user/0/com.anonymous.ctv/cache/" + bg_img;
            String imagePath = "/data/user/0/com.anonymous.ctv/cache/" + bg_img;
            SegmentationGraphic.originalBitmap  = BitmapFactory.decodeFile(imagePath);
        }

        if(SegmentationGraphic.originalBitmap != null){
            Bitmap bg_bitmap = Bitmap.createScaledBitmap(SegmentationGraphic.originalBitmap, maskWidth, maskHeight, true);
            bg_colors = new int[maskWidth * maskHeight];
            if(bg_bitmap != null){
                bg_bitmap.getPixels(bg_colors, 0, maskWidth, 0, 0, maskWidth, maskHeight);
            }
        }



    }

    /** Draws the segmented background on the supplied canvas. */
    @Override
    public void draw(Canvas canvas) {
        Bitmap bitmap = Bitmap.createBitmap(maskColorsFromByteBuffer(mask, bg_colors), maskWidth, maskHeight, Config.ARGB_8888);
        if (isRawSizeMaskEnabled) {
            Matrix matrix = new Matrix(getTransformationMatrix());
            matrix.preScale(scaleX, scaleY);
            canvas.drawBitmap(bitmap, matrix, null);
        } else {
            canvas.drawBitmap(bitmap, getTransformationMatrix(), null);
        }
        bitmap.recycle();
        mask.rewind();
    }

    /** Converts byteBuffer floats to ColorInt array that can be used as a mask. */
    @ColorInt
    private int[] maskColorsFromByteBuffer(ByteBuffer byteBuffer, int[] img) {
        int[] colors = img.length == 0 ? new int[maskWidth * maskHeight] : img;
        for (int i = 0; i < maskWidth * maskHeight; i++) {
            float backgroundLikelihood = 1 - byteBuffer.getFloat();
            if (backgroundLikelihood > 0.9) {
                if (img.length == 0) colors[i] = Color.argb(255, 255, 255, 255);
            } else if (backgroundLikelihood > 0.2) {
                int alpha = (int) (182.9 * backgroundLikelihood - 36.6 + 0.5);
                colors[i] = Color.argb(alpha, img.length == 0 ? 255 : 0, 0, 0);
            } else {
                colors[i] = Color.argb(0, 255, 255, 255);
            }
        }
        return applyConditionalBlurEffect(colors);
    }

    private int[] applyConditionalBlurEffect(int[] colors) {
        Bitmap bitmap = Bitmap.createBitmap(colors, maskWidth, maskHeight, Config.ARGB_8888);

        Allocation input = Allocation.createFromBitmap(renderScript, bitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation output = Allocation.createTyped(renderScript, input.getType());
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));

        blur.setRadius(5f);
        blur.setInput(input);
        blur.forEach(output);
        output.copyTo(bitmap);

        bitmap.getPixels(colors, 0, maskWidth, 0, 0, maskWidth, maskHeight);

        input.destroy();
        output.destroy();
        blur.destroy();
        bitmap.recycle();

        return colors;
    }

    public void releaseResources() {
        renderScript.destroy();
    }


}
