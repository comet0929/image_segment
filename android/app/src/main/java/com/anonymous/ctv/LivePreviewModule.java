package com.anonymous.ctv;

import android.content.Intent;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.anonymous.ctv.java.LivePreviewActivity;

public class LivePreviewModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public LivePreviewModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "LivePreviewModule";
    }

    @ReactMethod
    public void startLivePreviewActivity(String color) {
        Intent intent = new Intent(reactContext, LivePreviewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Pass the color as an extra
        intent.putExtra("color", color);
        reactContext.startActivity(intent);
    }
}
