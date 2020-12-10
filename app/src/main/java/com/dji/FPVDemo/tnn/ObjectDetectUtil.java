package com.dji.FPVDemo.tnn;

import android.app.Activity;
import android.util.Log;

public class ObjectDetectUtil {

    public String initModel(Activity activity) {

        String targetDir = activity.getFilesDir().getAbsolutePath();

        //copy detect model to sdcard
        String[] modelPathsDetector = {
                "yolov5s.tnnmodel",
                "yolov5s-permute.tnnproto",
        };

        for (int i = 0; i < modelPathsDetector.length; i++) {
            String modelFilePath = modelPathsDetector[i];
            String interModelFilePath = targetDir + "/" + modelFilePath;
            FileUtils.copyAsset(activity.getAssets(), "yolov5/" + modelFilePath, interModelFilePath);
        }
        Log.i("modelPath", "initModel: " + targetDir);
        return targetDir;
    }
}
