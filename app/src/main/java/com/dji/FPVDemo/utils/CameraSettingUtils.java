package com.dji.FPVDemo.utils;

import com.dji.FPVDemo.DJIApplication;

import dji.sdk.camera.Camera;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;

/**
 * 无人机摄像头设置
 *
 * @author dongsiyuan
 * @since 2020/11/24 14:41
 */
public class CameraSettingUtils {

    /**
     * 红外调整
     * @param mCamera
     */
    private void setThermalConfig(Camera mCamera) {
        BaseProduct baseProduct = DJIApplication.getProductInstance();
        mCamera = baseProduct.getCameras().get(0);
        mCamera.setDisplayMode(SettingsDefinitions.DisplayMode.MSX, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {

                }
            }
        });
        mCamera.setMSXLevel(90, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });
        mCamera.setThermalPalette(SettingsDefinitions.ThermalPalette.FUSION, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });

    }
}
