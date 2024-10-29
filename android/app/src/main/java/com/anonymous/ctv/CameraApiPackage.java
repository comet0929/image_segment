package com.anonymous.ctv;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CameraApiPackage implements ReactPackage {

    @Override
    public List<ViewManager> createViewManagers(
            ReactApplicationContext reactContext) {
        List<ViewManager> views = new ArrayList<>();
        //views.add(new CameraViewManager(reactContext));
        return  views;
    }

   @Override
   public List<NativeModule> createNativeModules(
           ReactApplicationContext reactContext) {
       List<NativeModule> modules = new ArrayList<>();

       modules.add(new CameraApiModule(reactContext));
       modules.add(new LivePreviewModule(reactContext));

       return modules;
   }

}