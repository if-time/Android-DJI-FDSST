package com.dji.FPVDemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.dianping.logan.Logan;
import com.dji.FPVDemo.detection.ClassifierFromTensorFlow;
import com.dji.FPVDemo.detection.tflite.TFLiteObjectDetectionAPIModel;
import com.dji.FPVDemo.interf.ConfirmLocationForTracking;
import com.dji.FPVDemo.jni.NativeHelper;
import com.dji.FPVDemo.tracking.FDSSTResultFormJNI;
import com.dji.FPVDemo.tracking.KCFResultFormJNI;
import com.dji.FPVDemo.utils.BorderedText;
import com.dji.FPVDemo.utils.LogUtil;
import com.dji.FPVDemo.utils.WriteFileUtil;
import com.dji.FPVDemo.utils.dialogs.DialogFragmentHelper;
import com.dji.FPVDemo.utils.dialogs.IDialogResultListener;
import com.dji.FPVDemo.view.MultiBoxTracker;
import com.dji.FPVDemo.view.OverlayView;
import com.dji.FPVDemo.view.TouchPaintView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
 * 主要是提供视频预览
 * @author dongsiyuan
 * @date 2020年10月27日
 */
public class VideoFeederMainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = VideoFeederMainActivity.class.getName();

    private static final String HANDLE_THREAD_NAME = "CameraBackgroundDetection";

    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    //    private static final String TF_OD_API_MODEL_FILE = "file:///android_asset/frozen_inference_graph_v6.pb";
//    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final float TEXT_SIZE_DIP = 10;

    private int widthDisplay;
    private int heightDisplay;

    private static float canvasWidth = 0;
    private static float canvasHeight = 0;

    private boolean runDetection = false;
    // 虚拟摇杆默认是关闭的
    private boolean isSimulator = false;

    private float controlValueIncX;
    private float controlValueIncY;
    private float dxCenterScreenObject;
    private float dyCenterScreenObject;

    private enum TrackerType {USE_KCF, USE_FDSST, USE_TENSORFLOW}

    private static TrackerType trackerType = TrackerType.USE_TENSORFLOW;

    private FlightController mFlightController;

    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    private Camera mCamera;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private final Object lock = new Object();

    private ClassifierFromTensorFlow classifierFromTensorFlow;

    // 画框
    private MultiBoxTracker tracker;
    private BorderedText borderedText;

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

    @BindView(R.id.tpvTouchFrame)
    TouchPaintView tpvTouchFrame;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_video_feeder);
        ButterKnife.bind(this);

        final float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);
        tracker = new MultiBoxTracker(this);

        getDisplaySize();

        initListener();

        // 注册无人机监听广播
        initFlightController();

        try {
            // create either a new ImageClassifierQuantizedMobileNet or an ImageClassifierFloatInception
            classifierFromTensorFlow = TFLiteObjectDetectionAPIModel.create(getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE, TF_OD_API_IS_QUANTIZED);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.");
        }
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

        // 来自TouchPaintView
        // 确定点击到了画框区域
        tpvTouchFrame.setConfirmLocationForTracking(new ConfirmLocationForTracking() {
            @Override
            public void confirmForTracking(final RectF rectFForFrame) {
                // showToast("回调");
                // 截取此区域的bitmap传入fdsst中
                final Bitmap bitmapForTracking = tvVideoPreviewer.getBitmap();

//                DialogUtils.showListDialog(MainActivity.this, getSupportFragmentManager(),"选择哪种跟踪算法？",new String[]{"KCF", "FDSST"});

                String titleList = "选择哪种跟踪算法？";
                final String[] languanges = new String[]{"KCF", "FDSST"};
                DialogFragmentHelper.showListDialog(VideoFeederMainActivity.this, getSupportFragmentManager(), titleList, languanges, new IDialogResultListener<Integer>() {
                    @Override
                    public void onDataResult(Integer result) {
                        showToast(languanges[result]);
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
        });

        ovTrackingOverlay.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(final Canvas canvas) {
                tracker.draw(canvas);
            }
        });
        tracker.setFrameConfiguration(widthDisplay, heightDisplay);
    }

    /**
     * FDSST初始化
     *
     * @param rectFForFrame
     * @param bitmapForTracking
     */
    private void trackingInitForFDSST(RectF rectFForFrame, Bitmap bitmapForTracking) {
        if (bitmapForTracking != null) {
//            int[] pixels = new int[bitmapForTracking.getWidth() * bitmapForTracking.getHeight()];
//            bitmapForTracking.getPixels(pixels, 0, bitmapForTracking.getWidth(),
//                    0, 0, bitmapForTracking.getWidth(), bitmapForTracking.getHeight());

            NativeHelper.getInstance().initFdsst(bitmapForTracking, rectFForFrame.left, rectFForFrame.top,
                    rectFForFrame.right, rectFForFrame.bottom, bitmapForTracking.getWidth(), bitmapForTracking.getHeight());
            showToast("init" + rectFForFrame.left + " " + rectFForFrame.top + " " +
                    rectFForFrame.right + " " + rectFForFrame.bottom);
        } else {
            showToast("bitmapForFDSST == null");
        }
    }

    /**
     * KCF初始化
     *
     * @param rectFForFrame
     * @param bitmapForTracking
     */
    private void trackingInitForKCF(RectF rectFForFrame, Bitmap bitmapForTracking) {
        if (bitmapForTracking != null) {
//            int[] pixels = new int[bitmapForTracking.getWidth() * bitmapForTracking.getHeight()];
//            bitmapForTracking.getPixels(pixels, 0, bitmapForTracking.getWidth(),
//                    0, 0, bitmapForTracking.getWidth(), bitmapForTracking.getHeight());
//            NativeHelper.getInstance().initKcf(pixels, rectFForFrame.left, rectFForFrame.top,
//                    rectFForFrame.right, rectFForFrame.bottom, bitmapForTracking.getWidth(), bitmapForTracking.getHeight());


            NativeHelper.getInstance().initKcf(bitmapForTracking, rectFForFrame.left, rectFForFrame.top,
                    rectFForFrame.right, rectFForFrame.bottom, bitmapForTracking.getWidth(), bitmapForTracking.getHeight());

            showToast("init" + rectFForFrame.left + " " + rectFForFrame.top + " " +
                    rectFForFrame.right + " " + rectFForFrame.bottom);
        } else {
            showToast("bitmapForKCF == null");
        }
    }


    /**
     * 获取屏幕大小
     */
    private void getDisplaySize() {
        WindowManager manager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        if (Build.VERSION.SDK_INT < 17) {
            display.getSize(point);
        } else {
            display.getRealSize(point);
        }
        widthDisplay = point.x;
        heightDisplay = point.y;
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

//    protected void onProductChange() {
//        initPreviewer();
//    }

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

    private void showToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
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
     * 定期识别
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
     * tensorFlow识别
     */
    private void classifyFrame() {
        //detectionForTensorFlow();

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
     * 通过FDSST进行跟踪
     */
    private void trackingForFDSST() {
        canvasWidth = tvVideoPreviewer.getWidth();
        canvasHeight = tvVideoPreviewer.getHeight();
        ivImageViewForFrame.getLayoutParams().width = tvVideoPreviewer.getWidth();
        ivImageViewForFrame.getLayoutParams().height = tvVideoPreviewer.getHeight();

        final Bitmap croppedBitmap = Bitmap.createBitmap((int) canvasWidth, (int) canvasHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);

        Bitmap bitmap = tvVideoPreviewer.getBitmap();

        if (bitmap == null) {
            showToast("bitmap == null");
            return;
        } else {
            // 获取到识别出的位置并画框
            long start = System.currentTimeMillis();
//            int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
//            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            FDSSTResultFormJNI result = NativeHelper.getInstance().usingFdsst(bitmap, bitmap.getWidth(), bitmap.getHeight());
            bitmap.recycle();

            dxCenterScreenObject = widthDisplay / 2 - result.x + result.width / 2;
            dyCenterScreenObject = heightDisplay / 2 - result.y + result.height / 2;

            controlValueIncX = (float) (dxCenterScreenObject * 0.0009f);
            controlValueIncY = (float) (dyCenterScreenObject * 0.0009f);
            flyControl((-1) * controlValueIncX, controlValueIncY, 0, 0);

            StringBuffer sb = new StringBuffer();
            LogUtil.addLineToSB(sb, "center_x: ", Math.round((result.x + result.width / 2) * 100) / 100);
            LogUtil.addLineToSB(sb, "center_y: ", Math.round((result.y + result.height / 2) * 100) / 100);
            LogUtil.addLineToSB(sb, "距离屏幕中心 x(px): ", Math.round((dxCenterScreenObject) * 100) / 100);
            LogUtil.addLineToSB(sb, "距离屏幕中心 y(px): ", Math.round((dyCenterScreenObject) * 100) / 100);
            LogUtil.addLineToSB(sb, "controlValueIncX: ", controlValueIncX);
            LogUtil.addLineToSB(sb, "controlValueIncY: ", controlValueIncY);
            setResultToText(sb.toString());
//            showToast("ms: " + (System.currentTimeMillis() - start));
            setFPS(1000 / (System.currentTimeMillis() - start));
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5.0f);
            paint.setAntiAlias(true);

            //            showToast(result.x + " " + result.y + " " + (result.width + result.x) + " " + (result.height + result.y));
            canvas.drawRect(result.x, result.y, result.width + result.x, result.height + result.y, paint);
            writeAprilTagsStatus(result.x, result.y, result.width + result.x, result.height + result.y);
            ivImageViewForFrame.post(new Runnable() {
                @Override
                public void run() {
                    ivImageViewForFrame.setImageBitmap(croppedBitmap);
                }
            });
        }
    }

    /**
     * 通过KCF进行跟踪
     */
    private void trackingForKCF() {
        canvasWidth = tvVideoPreviewer.getWidth();
        canvasHeight = tvVideoPreviewer.getHeight();
        ivImageViewForFrame.getLayoutParams().width = tvVideoPreviewer.getWidth();
        ivImageViewForFrame.getLayoutParams().height = tvVideoPreviewer.getHeight();

        final Bitmap croppedBitmap = Bitmap.createBitmap((int) canvasWidth, (int) canvasHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);

        Bitmap bitmap = tvVideoPreviewer.getBitmap();

        if (bitmap == null) {
            showToast("bitmap == null");
            return;
        } else {
            // 获取到识别出的位置并画框
//            showToast("usingKcf(bitmap)" + bitmap.getConfig());
            long start = System.currentTimeMillis();
//            int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
//            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
//            KCFResultFormJNI result = NativeHelper.getInstance().usingKcf(pixels, bitmap.getWidth(), bitmap.getHeight());
            KCFResultFormJNI result = NativeHelper.getInstance().usingKcf(bitmap, bitmap.getWidth(), bitmap.getHeight());
            bitmap.recycle();
            showToast("ms: " + (System.currentTimeMillis() - start));
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5.0f);
            paint.setAntiAlias(true);

//            showToast(result.x + " " + result.y + " " + (result.width + result.x) + " " + (result.height + result.y));
            canvas.drawRect(result.x, result.y, result.width + result.x, result.height + result.y, paint);
            writeAprilTagsStatus(result.x, result.y, result.width + result.x, result.height + result.y);
            ivImageViewForFrame.post(new Runnable() {
                @Override
                public void run() {
                    ivImageViewForFrame.setImageBitmap(croppedBitmap);
                }
            });
        }
    }


    /**
     * detectionForTensorFlow
     */
    private void detectionForTensorFlow() {
        ovTrackingOverlay.postInvalidate();
        if (classifierFromTensorFlow == null) {
            showToast("Uninitialized Classifier or invalid context.");
            return;
        }

        Bitmap bitmap = tvVideoPreviewer.getBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);

        if (bitmap == null) {
            showToast("bitmap == null");
            return;
        } else {
            //           showToast("bitmap == getWidth: " + bitmap.getWidth() + " bitmap == getHeight: " + bitmap.getHeight());
            final List<ClassifierFromTensorFlow.Recognition> results = classifierFromTensorFlow.recognizeImage(bitmap);

            canvasWidth = tvVideoPreviewer.getWidth();
            canvasHeight = tvVideoPreviewer.getHeight();
            //           if (mVideoSurface.getWidth() != imageView.getWidth() || mVideoSurface.getHeight() != imageView.getHeight()) {
            //               canvasWidth = mVideoSurface.getWidth();
            //               canvasHeight = mVideoSurface.getHeight();
            ivImageViewForFrame.getLayoutParams().width = tvVideoPreviewer.getWidth();
            ivImageViewForFrame.getLayoutParams().height = tvVideoPreviewer.getHeight();
            //           }
            bitmap.recycle();

            final Bitmap croppedBitmap = Bitmap.createBitmap((int) canvasWidth, (int) canvasHeight, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(croppedBitmap);

            final List<ClassifierFromTensorFlow.Recognition> mappedRecognitions = new LinkedList<ClassifierFromTensorFlow.Recognition>();

            for (final ClassifierFromTensorFlow.Recognition result : results) {
                final RectF location = result.getLocation();

                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {

                    RectF locationDisplay = new RectF(canvasWidth * location.left / TF_OD_API_INPUT_SIZE,
                            canvasHeight * location.top / TF_OD_API_INPUT_SIZE,
                            canvasWidth * location.right / TF_OD_API_INPUT_SIZE,
                            canvasHeight * location.bottom / TF_OD_API_INPUT_SIZE);
                    result.setLocation(locationDisplay);
                    mappedRecognitions.add(result);
//                    Paint paint = new Paint();
//                    Paint paint1 = new Paint();
//                    if (result.getTitle().equals("openeyes")) {
//                        paint.setColor(Color.GREEN);
//                        paint1.setColor(Color.GREEN);
//                    } else if (result.getTitle().equals("closeeyes")) {
//                        paint.setColor(Color.RED);
//                        paint1.setColor(Color.RED);
//
//                    } else if (result.getTitle().equals("phone")) {
//                        paint.setColor(0xFFFF9900);
//                        paint1.setColor(0xFFFF9900);
//
//                    } else if (result.getTitle().equals("smoke")) {
//                        paint.setColor(Color.YELLOW);
//                        paint1.setColor(Color.YELLOW);
//                    } else {
//                        paint.setColor(Color.WHITE);
//                    }
//
//                    paint.setStyle(Paint.Style.STROKE);
//                    paint.setStrokeWidth(5.0f);
//                    paint.setAntiAlias(true);
//                    paint1.setStyle(Paint.Style.FILL);
//                    paint1.setAlpha(125);
//                    //                canvas.drawRect(location, paint);
//                    //                   canvas.drawText();
//                    canvas.drawRect(canvasWidth * location.left / TF_OD_API_INPUT_SIZE, canvasHeight * location.top / TF_OD_API_INPUT_SIZE, canvasWidth * location.right / TF_OD_API_INPUT_SIZE, canvasHeight * location.bottom / TF_OD_API_INPUT_SIZE, paint);
//                    canvas.drawRect(canvasWidth * location.left / TF_OD_API_INPUT_SIZE, canvasHeight * location.top / TF_OD_API_INPUT_SIZE, canvasWidth * location.right / TF_OD_API_INPUT_SIZE, canvasHeight * location.bottom / TF_OD_API_INPUT_SIZE, paint1);
//                    canvas.drawRect((float) (canvasWidth * 0.5), (float) (canvasHeight * 0.5), (float) (canvasWidth * 0.5), (float) (canvasHeight * 0.5), paint);


                }

            }
            tracker.trackResultsFromTensorFlow(mappedRecognitions);
            ovTrackingOverlay.postInvalidate();
//            imageViewForFrame.post(new Runnable() {
//                @Override
//                public void run() {
//                    imageViewForFrame.setImageBitmap(croppedBitmap);
//                }
//            });
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
                        WriteFileUtil.putStringToExternalStorage(currentTime() + "\r\n", dir, "time_.txt", true);

                    }
                }
            }
        });
    }

    private String currentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        return sdf.format(curDate);
    }

    private void setFPS(final long fps) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvFPS.setText("FPS: " + fps);
            }
        });
    }

    /**
     * Push Status to TextView
     * @param string
     */
    private void setResultToText(final String string) {
        VideoFeederMainActivity.this.runOnUiThread(new Runnable() {
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
    private void flyControl(float leftRight, float frontBack, float turnLeftRight, float upDown) {
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

                            String yaw = String.format("%.2f", stateData.getYaw());
                            String pitch = String.format("%.2f", stateData.getPitch());
                            String roll = String.format("%.2f", stateData.getRoll());
                            String positionX = String.format("%.2f", stateData.getPositionX());
                            String positionY = String.format("%.2f", stateData.getPositionY());
                            String positionZ = String.format("%.2f", stateData.getPositionZ());
                            Log.i("Timeline", "simulator yaw:" + yaw + " |pitch:" + pitch + " |roll" + roll + " |positionX:" + positionX + " |positionY" + positionY + " |positionZ:" + positionZ);
                        }
                    });
                }
            });

        }
    }

    private void setResultToToast(final String string) {
        VideoFeederMainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(VideoFeederMainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R.id.btnThermalCamera)
    public void setThermalCamera() {
        setThermalConfig();
        showToast("红外");
    }

    @OnClick(R.id.btnBackgroundThread)
    public void clickBtnBackgroundThread() {
        startBackgroundThread();
        tpvTouchFrame.clearView();
    }

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

    @OnClick(R.id.ivSimulatorSetting)
    public void simulatorStatus() {
        if (mFlightController != null) {
            mFlightController.getVirtualStickModeEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    Log.i("getVirtualStick", "onSuccess: " + aBoolean);
                    setResultToToast("虚拟摇杆" + aBoolean);
                    Logan.w("getVirtualStick " + aBoolean, 2);
                    if (!aBoolean) {
                        mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    setResultToToast(djiError.getDescription());
                                } else {
                                    isSimulator = true;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ivSimulatorSetting.setImageResource(R.mipmap.ic_irtual_joystick);
                                            setResultToToast("虚拟摇杆开启");
                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        mFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    setResultToToast(djiError.getDescription());
                                } else {
                                    isSimulator = false;
                                    setResultToToast("虚拟摇杆关闭");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setResultToToast("simulator_stop_iv, 虚拟摇杆关闭");
                                            ivSimulatorSetting.setImageResource(R.mipmap.ic_remote_control);
                                        }
                                    });
                                }
                            }
                        });
                    }
                }

                @Override
                public void onFailure(DJIError djiError) {

                }
            });
        }
    }

}