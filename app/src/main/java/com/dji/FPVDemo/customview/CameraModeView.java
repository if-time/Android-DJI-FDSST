package com.dji.FPVDemo.customview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dji.FPVDemo.DJIApplication;
import com.dji.FPVDemo.R;

import butterknife.BindView;
import butterknife.OnClick;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;

/**
 * 切换视频流
 *
 * @author dongsiyuan
 * @since 2020/12/3 15:57
 */
public class CameraModeView extends CommonView {

    @BindView(R.id.btnCheck)
    Button btnCheck;
    @BindView(R.id.btnVisual)
    Button btnVisual;

    public CameraModeView(@NonNull Context context) {
        super(context);
    }

    public CameraModeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraModeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int configLayoutId() {
        return R.layout.item_layout_camera_mode_view;
    }

    @OnClick(R.id.btnCheck)
    public void checkThermal() {
        Camera cameraThermal = DJIApplication.getProductInstance().getCameras().get(1);
        if (cameraThermal != null) {
            cameraThermal.setDisplayMode(SettingsDefinitions.DisplayMode.MSX, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
            cameraThermal.setMSXLevel(50, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.i("checkThermal", "checkThermalset: " + djiError.toString());
                    }
                }
            });
            cameraThermal.getMSXLevel(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    Log.i("checkThermal", "checkThermal2: " + integer.toString());
                }

                @Override
                public void onFailure(DJIError djiError) {
                    Log.i("checkThermal", "checkThermal2: " + djiError.toString());
                }
            });
        }
    }

    @OnClick(R.id.btnVisual)
    public void setVisual() {
        Camera cameraThermal = DJIApplication.getProductInstance().getCameras().get(1);
        if (cameraThermal != null) {
            cameraThermal.setDisplayMode(SettingsDefinitions.DisplayMode.VISUAL_ONLY, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }
    }
}
