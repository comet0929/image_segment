package com.anonymous.ctv;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import java.util.Map;
import java.util.HashMap;
import android.util.Log;

public class CameraApiModule extends ReactContextBaseJavaModule {
    CameraApiModule(ReactApplicationContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return "CameraApiModule";
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public void createCalendarEvent(String name, String location) {
        Log.d("CalendarModule555", "Create event called with name: " + name
        + " and location: " + location);
    }
}