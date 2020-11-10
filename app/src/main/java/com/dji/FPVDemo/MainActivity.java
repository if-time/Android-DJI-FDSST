package com.dji.FPVDemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Display;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.dji.FPVDemo.detection.ClassifierFromTensorFlow;
import com.dji.FPVDemo.interf.ConfirmLocationForTracking;
import com.dji.FPVDemo.jni.NativeHelper;
import com.dji.FPVDemo.tracking.FDSSTResultFormJNI;
import com.dji.FPVDemo.tracking.KCFResultFormJNI;
import com.dji.FPVDemo.utils.WriteFileUtil;
import com.dji.FPVDemo.utils.dialogs.DialogFragmentHelper;
import com.dji.FPVDemo.utils.dialogs.IDialogResultListener;
import com.dji.FPVDemo.view.TouchPaintView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

/**
 * @author dongsiyuan
 * @date 2020年10月27日
 */
public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();

    private static final String HANDLE_THREAD_NAME = "CameraBackgroundDetection";

    private static final String TF_OD_API_MODEL_FILE = "file:///android_asset/frozen_inference_graph_v6.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    private static final int TF_OD_API_INPUT_SIZE = 300;

    private int widthDisplay;
    private int heightDisplay;

    private static float canvasWidth = 0;
    private static float canvasHeight = 0;

    private boolean runDetection = false;

    private enum TrackerType { USE_KCF, USE_FDSST}
    private static TrackerType trackerType = TrackerType.USE_KCF;

    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    private Camera mCamera;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private final Object lock = new Object();

    private ClassifierFromTensorFlow classifierFromTensorFlow;

//    private AutoFitTextureView mVideoSurface = null;
    private TextureView mVideoSurface = null;
    private Button btnThermal;
    private Button btnThread;

    private ImageView imageViewForFrame;

    private TouchPaintView touchView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        initUI();
        getDisplaySize();
        initListener();




    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = findViewById(R.id.video_previewer_surface);
        imageViewForFrame = findViewById(R.id.imageView);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        btnThermal = findViewById(R.id.btnThermal);
        btnThermal.setOnClickListener(this);
        btnThread = findViewById(R.id.btnThread);
        btnThread.setOnClickListener(this);

        touchView = (TouchPaintView) findViewById(R.id.touch_view);
    }

    private void initListener() {

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
        touchView.setConfirmLocationForTracking(new ConfirmLocationForTracking() {
            @Override
            public void confirmForTracking(final RectF rectFForFrame) {
                // showToast("回调");
                // 截取此区域的bitmap传入fdsst中
                final Bitmap bitmapForTracking = mVideoSurface.getBitmap();

//                DialogUtils.showListDialog(MainActivity.this, getSupportFragmentManager(),"选择哪种跟踪算法？",new String[]{"KCF", "FDSST"});

                String titleList = "选择哪种跟踪算法？";
                final String[] languanges = new String[]{"KCF", "FDSST"};
                DialogFragmentHelper.showListDialog(MainActivity.this, getSupportFragmentManager(), titleList, languanges, new IDialogResultListener<Integer>() {
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
    }

    /**
     * FDSST初始化
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
        BaseProduct baseProduct = FPVDemoApplication.getProductInstance();
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
        onProductChange();

        if (mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
        Log.e(TAG, "onResume: onResume is finished");
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
        super.onDestroy();
    }

    protected void onProductChange() {
        initPreviewer();
    }

    private void initPreviewer() {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            //            showToast(getString(R.string.disconnected));
        } else {
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (null != mVideoSurface) {
                    mVideoSurface.setSurfaceTextureListener(this);
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
            default:
                break;
        }
    }

    /**
     * 通过FDSST进行跟踪
     */
    private void trackingForFDSST() {
        canvasWidth = mVideoSurface.getWidth();
        canvasHeight = mVideoSurface.getHeight();
        imageViewForFrame.getLayoutParams().width = mVideoSurface.getWidth();
        imageViewForFrame.getLayoutParams().height = mVideoSurface.getHeight();

        final Bitmap croppedBitmap = Bitmap.createBitmap((int) canvasWidth, (int) canvasHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);

        Bitmap bitmap = mVideoSurface.getBitmap();

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
            showToast("ms: " + (System.currentTimeMillis() - start));
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5.0f);
            paint.setAntiAlias(true);

            //            showToast(result.x + " " + result.y + " " + (result.width + result.x) + " " + (result.height + result.y));
            canvas.drawRect(result.x, result.y, result.width + result.x, result.height + result.y, paint);
            writeAprilTagsStatus(result.x, result.y, result.width + result.x, result.height + result.y);
            imageViewForFrame.post(new Runnable() {
                @Override
                public void run() {
                    imageViewForFrame.setImageBitmap(croppedBitmap);
                }
            });
        }
    }

    /**
     * 通过KCF进行跟踪
     */
    private void trackingForKCF() {
        canvasWidth = mVideoSurface.getWidth();
        canvasHeight = mVideoSurface.getHeight();
        imageViewForFrame.getLayoutParams().width = mVideoSurface.getWidth();
        imageViewForFrame.getLayoutParams().height = mVideoSurface.getHeight();

        final Bitmap croppedBitmap = Bitmap.createBitmap((int) canvasWidth, (int) canvasHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);

        Bitmap bitmap = mVideoSurface.getBitmap();

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
            imageViewForFrame.post(new Runnable() {
                @Override
                public void run() {
                    imageViewForFrame.setImageBitmap(croppedBitmap);
                }
            });
        }
    }


    /**
     * detectionForTensorFlow
     */
    private void detectionForTensorFlow() {
        if (classifierFromTensorFlow == null) {
            showToast("Uninitialized Classifier or invalid context.");
            return;
        }

        Bitmap bitmap = mVideoSurface.getBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);

        if (bitmap == null) {
            showToast("bitmap == null");
            return;
        } else {
            //           showToast("bitmap == getWidth: " + bitmap.getWidth() + " bitmap == getHeight: " + bitmap.getHeight());
            final List<ClassifierFromTensorFlow.Recognition> results = classifierFromTensorFlow.recognizeImage(bitmap);

            canvasWidth = mVideoSurface.getWidth();
            canvasHeight = mVideoSurface.getHeight();
            //           if (mVideoSurface.getWidth() != imageView.getWidth() || mVideoSurface.getHeight() != imageView.getHeight()) {
            //               canvasWidth = mVideoSurface.getWidth();
            //               canvasHeight = mVideoSurface.getHeight();
            imageViewForFrame.getLayoutParams().width = mVideoSurface.getWidth();
            imageViewForFrame.getLayoutParams().height = mVideoSurface.getHeight();
            //           }
            bitmap.recycle();

            final Bitmap croppedBitmap = Bitmap.createBitmap((int) canvasWidth, (int) canvasHeight, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(croppedBitmap);

            for (final ClassifierFromTensorFlow.Recognition result : results) {
                final RectF location = result.getLocation();
                Log.i(TAG, "classifyFrame: " + location.bottom);
                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                    Paint paint = new Paint();
                    Paint paint1 = new Paint();
                    if (result.getTitle().equals("openeyes")) {
                        paint.setColor(Color.GREEN);
                        paint1.setColor(Color.GREEN);
                    } else if (result.getTitle().equals("closeeyes")) {
                        paint.setColor(Color.RED);
                        paint1.setColor(Color.RED);

                    } else if (result.getTitle().equals("phone")) {
                        paint.setColor(0xFFFF9900);
                        paint1.setColor(0xFFFF9900);

                    } else if (result.getTitle().equals("smoke")) {
                        paint.setColor(Color.YELLOW);
                        paint1.setColor(Color.YELLOW);
                    } else {
                        paint.setColor(Color.WHITE);
                    }

                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5.0f);
                    paint.setAntiAlias(true);
                    paint1.setStyle(Paint.Style.FILL);
                    paint1.setAlpha(125);
                    //                canvas.drawRect(location, paint);
                    //                   canvas.drawText();
                    canvas.drawRect(canvasWidth * location.left / TF_OD_API_INPUT_SIZE, canvasHeight * location.top / TF_OD_API_INPUT_SIZE, canvasWidth * location.right / TF_OD_API_INPUT_SIZE, canvasHeight * location.bottom / TF_OD_API_INPUT_SIZE, paint);
                    canvas.drawRect(canvasWidth * location.left / TF_OD_API_INPUT_SIZE, canvasHeight * location.top / TF_OD_API_INPUT_SIZE, canvasWidth * location.right / TF_OD_API_INPUT_SIZE, canvasHeight * location.bottom / TF_OD_API_INPUT_SIZE, paint1);
                    canvas.drawRect((float) (canvasWidth * 0.5), (float) (canvasHeight * 0.5), (float) (canvasWidth * 0.5), (float) (canvasHeight * 0.5), paint);
                }

            }
            imageViewForFrame.post(new Runnable() {
                @Override
                public void run() {
                    imageViewForFrame.setImageBitmap(croppedBitmap);
                }
            });
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnThermal:
                setThermalConfig();
                showToast("红外");
                break;
            case R.id.btnThread:
                startBackgroundThread();
                touchView.clearView();
                break;
            default:
                break;
        }
    }

    /**
     * 获取跟踪算法返回的结果
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

}