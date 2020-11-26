package com.dji.FPVDemo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.dianping.logan.Logan;
import com.dji.FPVDemo.customview.DetectionModelView;
import com.dji.FPVDemo.customview.TrackingForTouchFrameView;
import com.dji.FPVDemo.detection.ClassifierFromTensorFlow;
import com.dji.FPVDemo.detection.tflite.TFLiteObjectDetectionAPIModel;
import com.dji.FPVDemo.enums.TrackerTypeEnum;
import com.dji.FPVDemo.interf.AddOverlayView;
import com.dji.FPVDemo.interf.ConfirmLocationForTensorFlow;
import com.dji.FPVDemo.interf.SetRecognitionAlgorithm;
import com.dji.FPVDemo.utils.CommonUtils;
import com.dji.FPVDemo.utils.DensityUtil;
import com.dji.FPVDemo.utils.WriteFileUtil;
import com.dji.FPVDemo.utils.dialogs.DialogFragmentHelper;
import com.dji.FPVDemo.utils.dialogs.IDialogResultListener;
import com.dji.FPVDemo.view.MultiBoxTracker;
import com.dji.FPVDemo.view.OverlayView;
import com.dji.FPVDemo.view.xcslideview.XCSlideView;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
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

    private static final String HANDLE_THREAD_DETECTION_NAME = "ThreadForTensorFlow";
    private static final String HANDLE_THREAD_TRACKING_NAME = "ThreadForTracking";

    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    public static final int TF_OD_API_INPUT_SIZE = 300;

    public int widthDisplay;
    public int heightDisplay;

    private boolean runTracking = false;
    private boolean runDetectionForTensorFlow = false;
    // 虚拟摇杆默认是关闭的
    private boolean isSimulator = false;

    public AtomicBoolean detectNcnn = new AtomicBoolean(false);
    public AtomicBoolean detectTensorFlow = new AtomicBoolean(false);
    public AtomicBoolean trackingKcf = new AtomicBoolean(false);
    public AtomicBoolean trackingFdsst = new AtomicBoolean(false);

    private FlightController mFlightController;

    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    private Camera mCamera;

    private HandlerThread backgroundThreadForTracking;
    private Handler backgroundHandlerForTracking;

    private final Object lockForTracking = new Object();

    private HandlerThread backgroundThreadForTensorFlow;
    private Handler backgroundHandlerForTensorFlow;

    private final Object lockForTensorFlow = new Object();

    public ClassifierFromTensorFlow classifierFromTensorFlow;

    // 画框
    public MultiBoxTracker tracker;

    private XCSlideView slideViewRightMoreSetting;

    OverlayView ovTrackingOverlay;
    View overlayView;

    @BindView(R.id.ivMoreSetting)
    ImageView ivMoreSetting;

    //    private AutoFitTextureView mVideoSurface = null;
    @BindView(R.id.tvVideoPreviewer)
    TextureView tvVideoPreviewer = null;

    @BindView(R.id.ivBackgroundThread)
    ImageView ivBackgroundThread;

    @BindView(R.id.tvFPS)
    TextView tvFPS;

    @BindView(R.id.ivImageViewForFrame)
    ImageView ivImageViewForFrame;

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
    @BindView(R.id.llOverlayViewContainer)
    LinearLayout llOverlayViewContainer;

    TrackingForTouchFrameView itemTrackingForTouchFrameView;
    DetectionModelView itemDetectionModelView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_video_feeder);
        ButterKnife.bind(this);

        tracker = new MultiBoxTracker(this);

        widthDisplay = DensityUtil.getScreenWidthAndHeight(this)[0];
        heightDisplay = DensityUtil.getScreenWidthAndHeight(this)[1];

        initListener();

        // 注册无人机监听广播
        initFlightController();

        initSlideView();
    }

    private void initSlideView() {
        View menuViewRight = LayoutInflater.from(this).inflate(R.layout.layout_slideview, null);
        slideViewRightMoreSetting = XCSlideView.create(this, XCSlideView.Positon.RIGHT);
        slideViewRightMoreSetting.setMenuView(DJIMainActivity.this, menuViewRight);
        slideViewRightMoreSetting.setMenuWidth(widthDisplay * 4 / 9);
        itemTrackingForTouchFrameView = menuViewRight.findViewById(R.id.itemTrackingForTouchFrameView);
        itemTrackingForTouchFrameView.initView(this, llTouchFrameViewContainer);
        itemDetectionModelView = menuViewRight.findViewById(R.id.itemDetectionModelView);
        itemDetectionModelView.initView(this, getSupportFragmentManager());
        itemDetectionModelView.setAddOverlayViewCallback(new AddOverlayView() {
            @Override
            public void addOverlay() {
                addOverlayView();
            }
        });
        itemDetectionModelView.setRecognitionAlgorithmCallback(new SetRecognitionAlgorithm() {
            @Override
            public void initRecognitionAlgorithm(TrackerTypeEnum.TrackerType trackerType) {
                switch (trackerType) {
                    case USE_TENSORFLOW:
                        try {
                            // create either a new ImageClassifierQuantizedMobileNet or an ImageClassifierFloatInception
                            classifierFromTensorFlow = TFLiteObjectDetectionAPIModel.create(getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE, TF_OD_API_IS_QUANTIZED);
                        } catch (IOException e) {
                            Log.e("TensorFlow", "Failed to initialize an image classifier.");

                        }
                        break;
                    case USE_NCNN:
                        CommonUtils.showToast(DJIMainActivity.this, trackerType.toString());
                        initForNcnn();
                        break;
                    default:
                        break;
                }
            }
        });
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
        stopBackgroundThreadForTracking();
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
        detectNcnn.set(false);
        detectTensorFlow.set(false);
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

    @Override
    public void onBackPressed() {
        if (slideViewRightMoreSetting.isShow()) {
            slideViewRightMoreSetting.dismiss();
            return;
        }
        super.onBackPressed();
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
    public void startBackgroundThreadForTensorFlow() {
        backgroundThreadForTensorFlow = new HandlerThread(HANDLE_THREAD_DETECTION_NAME);
        backgroundThreadForTensorFlow.start();
        backgroundHandlerForTensorFlow = new Handler(backgroundThreadForTensorFlow.getLooper());
        synchronized (lockForTensorFlow) {
            runDetectionForTensorFlow = true;
        }
        backgroundHandlerForTensorFlow.post(periodicDetectionForTensorFlow);
    }

    /**
     * 停止后台线程
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stopBackgroundThreadForTensorFlow() {
        if (backgroundThreadForTensorFlow != null) {
            backgroundThreadForTensorFlow.quitSafely();
            try {
                backgroundThreadForTensorFlow.join();
                backgroundThreadForTensorFlow = null;
                backgroundHandlerForTensorFlow = null;
                synchronized (lockForTensorFlow) {
                    runDetectionForTensorFlow = false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 识别任务
     */
    private Runnable periodicDetectionForTensorFlow = new Runnable() {
        @Override
        public void run() {
            synchronized (lockForTensorFlow) {
                if (runDetectionForTensorFlow) {
                    classifyFrameForTensorFlow();
                }
            }
            backgroundHandlerForTensorFlow.post(periodicDetectionForTensorFlow);
        }
    };

    /**
     * 识别
     */
    private void classifyFrameForTensorFlow() {
        detectionForTensorFlow();
    }

    /**
     * 启动后台线程
     */
    public void startBackgroundThreadForTracking() {
        backgroundThreadForTracking = new HandlerThread(HANDLE_THREAD_TRACKING_NAME);
        backgroundThreadForTracking.start();
        backgroundHandlerForTracking = new Handler(backgroundThreadForTracking.getLooper());
        synchronized (lockForTracking) {
            runTracking = true;
        }
        backgroundHandlerForTracking.post(periodicDetectionForTracking);
    }

    /**
     * 停止后台线程
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stopBackgroundThreadForTracking() {
        if (backgroundThreadForTracking != null) {
            backgroundThreadForTracking.quitSafely();
            try {
                backgroundThreadForTracking.join();
                backgroundThreadForTracking = null;
                backgroundHandlerForTracking = null;
                synchronized (lockForTracking) {
                    runTracking = false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 识别任务
     */
    private Runnable periodicDetectionForTracking = new Runnable() {
        @Override
        public void run() {
            synchronized (lockForTracking) {
                if (runTracking) {
                    frameForTracking();
                }
            }
            backgroundHandlerForTracking.post(periodicDetectionForTracking);
        }
    };

    /**
     * 识别
     */
    private void frameForTracking() {
        switch (TrackerTypeEnum.trackerType) {
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
            case USE_NCNN:
                detectionForNcnn();
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
     * 通过回调获得RectF坐标，进行跟踪算法初始化
     *
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
                        TrackerTypeEnum.trackerType = TrackerTypeEnum.TrackerType.USE_KCF;
                        removeOverlayView();
                        break;
                    case 1:
                        trackingInitForFDSST(rectFForFrame, bitmapForTracking);
                        TrackerTypeEnum.trackerType = TrackerTypeEnum.TrackerType.USE_FDSST;
                        removeOverlayView();
                        break;
                    default:
                        break;
                }
            }
        }, true);
    }

    @OnClick(R.id.ivMoreSetting)
    public void isShowSlideView() {
        if (!slideViewRightMoreSetting.isShow()) {
            slideViewRightMoreSetting.show();
        }
    }

    /**
     * 开启关闭线程
     */
    @OnClick(R.id.ivBackgroundThread)
    public void clickBtnBackgroundThread() {
        if (runTracking) {
//            stopBackgroundThreadForTensorFlow();
            stopBackgroundThreadForTracking();
            if (!runTracking) {
                ivBackgroundThread.setBackgroundResource(R.mipmap.ic_detect_close);
            }

            if (TrackerTypeEnum.trackerType == TrackerTypeEnum.TrackerType.USE_FDSST) {
                clearFrame();
            }

            if (TrackerTypeEnum.trackerType == TrackerTypeEnum.TrackerType.USE_TENSORFLOW) {
                removeOverlayView();
            }

        } else {
//            startBackgroundThreadForTensorFlow();
            startBackgroundThreadForTracking();
            if (runTracking) {
                ivBackgroundThread.setBackgroundResource(R.mipmap.ic_detect_open);
            }

            if (itemTrackingForTouchFrameView != null) {
                itemTrackingForTouchFrameView.clearView();
            }
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
        CommonUtils.showToast(this, "通过点击按钮来开关虚拟摇杆" + " : " + (mFlightController != null));
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

    /**
     * 清除追踪的框
     */
    private void clearFrame() {
        final Bitmap croppedBitmap = Bitmap.createBitmap((int) tvVideoPreviewer.getWidth(), (int) tvVideoPreviewer.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);
        ivImageViewForFrame.post(new Runnable() {
            @Override
            public void run() {
                ivImageViewForFrame.setImageBitmap(croppedBitmap);
            }
        });
    }

    private void addOverlayView() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.inflater_overlay_view, null);

        ovTrackingOverlay = overlayView.findViewById(R.id.ovTrackingOverlay);

        llOverlayViewContainer.addView(overlayView,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ovTrackingOverlay.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(final Canvas canvas) {
                tracker.draw(canvas);
            }
        });

        tracker.inputTrackingOverlayObject(ovTrackingOverlay);
        tracker.setConfirmLocationForTensorFlow(new ConfirmLocationForTensorFlow() {
            @Override
            public void confirmForTracking(RectF rectFForFrame) {
                initTrackingAlgorithm(rectFForFrame);
                startBackgroundThreadForTracking();
            }
        });

    }

    public void removeOverlayView() {
        if (llOverlayViewContainer != null) {
            llOverlayViewContainer.removeAllViews();
        }
    }


    protected abstract void trackingInitForKCF(RectF rectFForFrame, Bitmap bitmapForTracking);

    protected abstract void trackingInitForFDSST(RectF rectFForFrame, Bitmap bitmapForTracking);

    protected abstract void trackingForKCF();

    protected abstract void trackingForFDSST();

    protected abstract void detectionForTensorFlow();

    protected abstract void initForNcnn();

    protected abstract void detectionForNcnn();
}