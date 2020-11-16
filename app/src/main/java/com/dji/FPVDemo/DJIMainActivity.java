package com.dji.FPVDemo;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.dianping.logan.Logan;
import com.dji.FPVDemo.interf.ConfirmLocationForTracking;
import com.dji.FPVDemo.utils.CommonUtils;
import com.dji.FPVDemo.utils.WriteFileUtil;
import com.dji.FPVDemo.utils.dialogs.DialogFragmentHelper;
import com.dji.FPVDemo.utils.dialogs.IDialogResultListener;
import com.dji.FPVDemo.view.OverlayView;
import com.dji.FPVDemo.view.TouchFrameView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;


/**
 * 主要是提供视频预览、DJI相关API以及其他功能
 *
 * @author dongsiyuan
 * @date 2020年10月27日
 */
public abstract class DJIMainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = DJIMainActivity.class.getName();

    private static final String HANDLE_THREAD_NAME = "CameraBackgroundDetection";

    private boolean runDetection = false;
    // 虚拟摇杆默认是关闭的
    private boolean isSimulator = false;

    public enum TrackerType {USE_KCF, USE_FDSST, USE_TENSORFLOW}

    public static TrackerType trackerType = TrackerType.USE_TENSORFLOW;

    private FlightController mFlightController;

    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    private Camera mCamera;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private final Object lock = new Object();

    private View touchFrameView;

    //    private AutoFitTextureView mVideoSurface = null;
    @BindView(R.id.tvVideoPreviewer)
    TextureView tvVideoPreviewer = null;
    @BindView(R.id.btnThermalCamera)
    Button btnThermalCamera;
    @BindView(R.id.btnBackgroundThread)
    Button btnBackgroundThread;

    @BindView(R.id.tvFPS)
    TextView tvFPS;

    @BindView(R.id.ivImageViewForFrame)
    ImageView ivImageViewForFrame;

//    @BindView(R.id.tpvTouchFrame)
    TouchFrameView tpvTouchFrame;

    @BindView(R.id.ovTrackingOverlay)
    OverlayView ovTrackingOverlay;

    @BindView(R.id.ivSimulatorSetting)
    ImageView ivSimulatorSetting;

    //详情 抽屉
    @BindView(R.id.ivTrackingDrawerControlIb)
    ImageButton ivTrackingDrawerControlIb;
    @BindView(R.id.sdTrackingDrawer)
    SlidingDrawer sdTrackingDrawer;
    @BindView(R.id.tvTrackingPushInfo)
    TextView tvTrackingPushInfo;

    @BindView(R.id.llTouchFrameViewContainer)
    LinearLayout llTouchFrameViewContainer;
    @BindView(R.id.llViewForFrameContainer)
    LinearLayout llViewForFrameContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_video_feeder);
        ButterKnife.bind(this);
        initListener();

        // 注册无人机监听广播
        initFlightController();
    }

    private void initListener() {

        if (null != tvVideoPreviewer) {
            tvVideoPreviewer.setSurfaceTextureListener(this);
        }

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
    }

    private void setThermalConfig() {
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

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
//        onProductChange();
        initFlightController();
        if (tvVideoPreviewer == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        uninitPreviewer();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void onReturn(View view) {
        this.finish();
    }

    @Override
    protected void onDestroy() {
        uninitPreviewer();
        // 关闭虚拟摇杆
        if (mFlightController != null) {
            mFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        setResultToToast(djiError.getDescription());
                    } else {
                        setResultToToast("虚拟摇杆关闭");
                    }
                }
            });
        }
        super.onDestroy();
    }

    private void initPreviewer() {

        BaseProduct product = DJIApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            //            showToast(getString(R.string.disconnected));
        } else {
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (null != tvVideoPreviewer) {
                    tvVideoPreviewer.setSurfaceTextureListener(this);
                }
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    private void uninitPreviewer() {
        //        Camera camera = DemoApplication.getCameraInstance();
        if (mCamera != null) {
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.i(TAG, "onSurfaceTextureUpdated: 1111111111");
    }

    /**
     * 启动后台线程
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        synchronized (lock) {
            runDetection = true;
        }
        backgroundHandler.post(periodicDetection);
    }

    /**
     * 停止后台线程
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
                synchronized (lock) {
                    runDetection = false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 识别任务
     */
    private Runnable periodicDetection = new Runnable() {
        @Override
        public void run() {
            synchronized (lock) {
                if (runDetection) {
                    classifyFrame();
                }
            }
            backgroundHandler.post(periodicDetection);
        }
    };

    /**
     * 识别
     */
    private void classifyFrame() {
        switch (trackerType) {
            case USE_KCF:
                trackingForKCF();
//                showToast("trackingForKCF");
                break;
            case USE_FDSST:
                trackingForFDSST();
//                showToast("trackingForFDSST");
                break;
            case USE_TENSORFLOW:
                detectionForTensorFlow();
                break;
            default:
                break;
        }
    }

    /**
     * 获取跟踪算法返回的结果
     *
     * @param l_x
     * @param l_y
     * @param r_x
     * @param r_y
     */
    public void writeAprilTagsStatus(final int l_x, final int l_y, final int r_x, final int r_y) {
        // 将数据写入文件，包括每次识别后：停机坪框的中心与屏幕中心在x轴和y轴的距离差、无人机高度、x方向和y方向的控制量、当前时间
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (WriteFileUtil.isExternalStorageWritable()) {
                    String status = Environment.getExternalStorageState();
                    if (status.equals(Environment.MEDIA_MOUNTED)) {
                        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/tracking/");
                        if (!dir.exists()) {
                            dir.mkdir();
                        }
                        WriteFileUtil.putStringToExternalStorage(l_x + "\r\n", dir, "l_x.txt", true);
                        WriteFileUtil.putStringToExternalStorage(l_y + "\r\n", dir, "l_y.txt", true);
                        WriteFileUtil.putStringToExternalStorage(r_x + "\r\n", dir, "r_x.txt", true);
                        WriteFileUtil.putStringToExternalStorage(r_y + "\r\n", dir, "r_y.txt", true);
                        WriteFileUtil.putStringToExternalStorage(CommonUtils.currentTime() + "\r\n", dir, "time_.txt", true);
                    }
                }
            }
        });
    }

    public void setFPS(final long fps) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvFPS.setText("FPS: " + fps);
            }
        });
    }

    /**
     * Push Status to TextView
     *
     * @param string
     */
    public void setResultToText(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvTrackingPushInfo.setText(string);
            }
        });
    }

    /**
     * 虚拟摇杆功能，可通过程序控制无人机的前、后、左、右、上升、下降的飞行动作
     *
     * @param leftRight     正数为右，负数为左
     * @param frontBack     正数为前，负数为后
     * @param turnLeftRight
     * @param upDown        正数为上升，负数为下降
     */
    public void flyControl(float leftRight, float frontBack, float turnLeftRight, float upDown) {
        if (mFlightController == null) {
            BaseProduct product = DJIApplication.getProductInstance();
            if (product == null || !product.isConnected()) {
                setResultToToast("未连接到无人机！");
            } else {
                if (product instanceof Aircraft) {
                    Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
                    mFlightController = aircraft.getFlightController();
                }
            }
        }

        mFlightController.sendVirtualStickFlightControlData(new FlightControlData(leftRight, frontBack, turnLeftRight, upDown), new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    setResultToToast("flyControl_DJIError: " + djiError.getDescription());
                }
            }
        });
    }

    /**
     * 初始化无人机控制，并获取无人机位置、各种状态
     */
    private void initFlightController() {
        BaseProduct product = DJIApplication.getProductInstance();
        if (product == null || !product.isConnected()) {
            setResultToToast("未连接到无人机！");
        } else {
            if (product instanceof Aircraft) {
                //                mFlightController = ((Aircraft) product).getFlightController();
                Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
                mFlightController = aircraft.getFlightController();

            }
        }
        // 无人机
        if (mFlightController != null) {
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            //            mFlightController.setYawControlMode(YawControlMode.ANGLE);
            mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            mFlightController.getSimulator().setStateCallback(new SimulatorState.Callback() {
                @Override
                public void onUpdate(final SimulatorState stateData) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
                }
            });

        }
    }

    private void setResultToToast(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DJIMainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * touchFrameView
     *
     * @param view
     */
    public void addTouchFrameView(View view) {
        touchFrameView = LayoutInflater.from(this).inflate(R.layout.inflater_touch_frame, null);

        tpvTouchFrame = touchFrameView.findViewById(R.id.tpvTouchFrame);

        llViewForFrameContainer.addView(touchFrameView,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 来自TouchPaintView
        // 确定点击到了画框区域
        tpvTouchFrame.setConfirmLocationForTracking(new ConfirmLocationForTracking() {
            @Override
            public void confirmForTracking(final RectF rectFForFrame) {
                // showToast("回调");
                initTrackingAlgorithm(rectFForFrame);
            }
        });

    }

    /**
     * 通过回调获得RectF坐标，进行跟踪算法初始化
     * @param rectFForFrame
     */
    public void initTrackingAlgorithm(RectF rectFForFrame) {
        // 截取此区域的bitmap传入fdsst中
        final Bitmap bitmapForTracking = tvVideoPreviewer.getBitmap();

//                DialogUtils.showListDialog(MainActivity.this, getSupportFragmentManager(),"选择哪种跟踪算法？",new String[]{"KCF", "FDSST"});

        String titleList = "选择哪种跟踪算法？";
        final String[] languanges = new String[]{"KCF", "FDSST"};
        DialogFragmentHelper.showListDialog(DJIMainActivity.this, getSupportFragmentManager(), titleList, languanges, new IDialogResultListener<Integer>() {
            @Override
            public void onDataResult(Integer result) {
                CommonUtils.showToast(DJIMainActivity.this, languanges[result]);
                switch (result) {
                    case 0:
                        trackingInitForKCF(rectFForFrame, bitmapForTracking);
                        trackerType = TrackerType.USE_KCF;
                        break;
                    case 1:
                        trackingInitForFDSST(rectFForFrame, bitmapForTracking);
                        trackerType = TrackerType.USE_FDSST;
                        break;
                    default:
                        break;
                }
            }
        }, true);
    }

    @OnClick(R.id.btnThermalCamera)
    public void setThermalCamera() {
        setThermalConfig();
        CommonUtils.showToast(DJIMainActivity.this, "红外");
    }

    /**
     * 开启关闭线程
     */
    @OnClick(R.id.btnBackgroundThread)
    public void clickBtnBackgroundThread() {
        if (runDetection) {
            stopBackgroundThread();
        } else {
            startBackgroundThread();
//            tpvTouchFrame.clearView();
        }
    }

    /**
     * 识别时显示识别信息
     */
    @OnClick(R.id.ivTrackingDrawerControlIb)
    public void slidingDrawerOpenClose() {
        if (sdTrackingDrawer.isOpened()) {
            Log.i("tracking_drawer", "isOpened: animateClose");
            setResultToToast("tracking_drawer, isOpened?: animateClose");
            sdTrackingDrawer.animateClose();
        } else {
            Log.i("tracking_drawer", "isClosed: animateOpen");
            setResultToToast("tracking_drawer, isClosed?: animateOpen");
            sdTrackingDrawer.animateOpen();
        }
    }

    /**
     * 通过点击按钮来开关虚拟摇杆
     */
    @OnClick(R.id.ivSimulatorSetting)
    public void simulatorStatusEnabled() {
        if (mFlightController != null) {
            mFlightController.getVirtualStickModeEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    Log.i("getVirtualStick", "onSuccess: " + aBoolean);
                    setResultToToast("虚拟摇杆" + aBoolean);
                    Logan.w("getVirtualStick " + aBoolean, 2);
                    setVirtualStickModeEnabled(aBoolean);
                }

                @Override
                public void onFailure(DJIError djiError) {
                    setResultToToast("虚拟摇杆开启失败");
                }
            });
        }
    }

    /**
     * 调用DJI的API设置虚拟摇杆
     *
     * @param aBoolean
     */
    private void setVirtualStickModeEnabled(Boolean aBoolean) {
        mFlightController.setVirtualStickModeEnabled(!aBoolean, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    setResultToToast(djiError.getDescription());
                } else {
                    isSimulator = !aBoolean;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!aBoolean) {
                                ivSimulatorSetting.setImageResource(R.mipmap.ic_irtual_joystick);
                                setResultToToast("虚拟摇杆开启");
                            } else {
                                setResultToToast("simulator_stop_iv, 虚拟摇杆关闭");
                                ivSimulatorSetting.setImageResource(R.mipmap.ic_remote_control);
                            }
                        }
                    });
                }
            }
        });
    }

    protected abstract void trackingInitForKCF(RectF rectFForFrame, Bitmap bitmapForTracking);

    protected abstract void trackingInitForFDSST(RectF rectFForFrame, Bitmap bitmapForTracking);

    protected abstract void trackingForKCF();

    protected abstract void trackingForFDSST();

    protected abstract void detectionForTensorFlow();
}