package com.dji.FPVDemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import com.dji.FPVDemo.detection.ClassifierFromTensorFlow;
import com.dji.FPVDemo.detection.tflite.TFLiteObjectDetectionAPIModel;
import com.dji.FPVDemo.jni.NativeHelper;
import com.dji.FPVDemo.tracking.FDSSTResultFormJNI;
import com.dji.FPVDemo.tracking.KCFResultFormJNI;
import com.dji.FPVDemo.utils.BorderedText;
import com.dji.FPVDemo.utils.CommonUtils;
import com.dji.FPVDemo.utils.LogUtil;
import com.dji.FPVDemo.view.MultiBoxTracker;
import com.dji.FPVDemo.view.OverlayView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * 在这个类里进行识别跟踪
 *
 * @author dongsiyuan
 * @date 2020/11/12 15:42
 */
public class MainActivity extends DJIMainActivity {

    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final float TEXT_SIZE_DIP = 10;

    private int widthDisplay;
    private int heightDisplay;

    private static float canvasWidth = 0;
    private static float canvasHeight = 0;

    private float controlValueIncX;
    private float controlValueIncY;
    private float dxCenterScreenObject;
    private float dyCenterScreenObject;

    private ClassifierFromTensorFlow classifierFromTensorFlow;

    // 画框
    private MultiBoxTracker tracker;
    private BorderedText borderedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getDisplaySize();

        final float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);
        tracker = new MultiBoxTracker(this);

        try {
            // create either a new ImageClassifierQuantizedMobileNet or an ImageClassifierFloatInception
            classifierFromTensorFlow = TFLiteObjectDetectionAPIModel.create(getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE, TF_OD_API_IS_QUANTIZED);
        } catch (IOException e) {
            Log.e("donfs", "Failed to initialize an image classifier.");

        }

        ovTrackingOverlay.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(final Canvas canvas) {
                tracker.draw(canvas);
            }
        });
        tracker.setFrameConfiguration(widthDisplay, heightDisplay);
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

    /**
     * detectionForTensorFlow
     */
    @Override
    public void detectionForTensorFlow() {
        ovTrackingOverlay.postInvalidate();
        if (classifierFromTensorFlow == null) {
            CommonUtils.showToast(MainActivity.this, "Uninitialized Classifier or invalid context.");
            return;
        }

        Bitmap bitmap = tvVideoPreviewer.getBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);

        if (bitmap == null) {
            CommonUtils.showToast(MainActivity.this, "bitmap == null");
            return;
        } else {
            final List<ClassifierFromTensorFlow.Recognition> results = classifierFromTensorFlow.recognizeImage(bitmap);

            canvasWidth = tvVideoPreviewer.getWidth();
            canvasHeight = tvVideoPreviewer.getHeight();
            ivImageViewForFrame.getLayoutParams().width = tvVideoPreviewer.getWidth();
            ivImageViewForFrame.getLayoutParams().height = tvVideoPreviewer.getHeight();
            bitmap.recycle();

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
                }
            }
            tracker.trackResultsFromTensorFlow(mappedRecognitions);
            ovTrackingOverlay.postInvalidate();
        }
    }

    /**
     * FDSST初始化
     *
     * @param rectFForFrame
     * @param bitmapForTracking
     */
    public void trackingInitForFDSST(RectF rectFForFrame, Bitmap bitmapForTracking) {
        if (bitmapForTracking != null) {
//            int[] pixels = new int[bitmapForTracking.getWidth() * bitmapForTracking.getHeight()];
//            bitmapForTracking.getPixels(pixels, 0, bitmapForTracking.getWidth(),
//                    0, 0, bitmapForTracking.getWidth(), bitmapForTracking.getHeight());

            NativeHelper.getInstance().initFdsst(bitmapForTracking, rectFForFrame.left, rectFForFrame.top,
                    rectFForFrame.right, rectFForFrame.bottom, bitmapForTracking.getWidth(), bitmapForTracking.getHeight());
            CommonUtils.showToast(MainActivity.this, "init" + rectFForFrame.left + " " + rectFForFrame.top + " " +
                    rectFForFrame.right + " " + rectFForFrame.bottom);
        } else {
            CommonUtils.showToast(MainActivity.this, "bitmapForFDSST == null");
        }
    }


    /**
     * 通过FDSST进行跟踪
     */
    @Override
    public void trackingForFDSST() {
        canvasWidth = tvVideoPreviewer.getWidth();
        canvasHeight = tvVideoPreviewer.getHeight();
        ivImageViewForFrame.getLayoutParams().width = tvVideoPreviewer.getWidth();
        ivImageViewForFrame.getLayoutParams().height = tvVideoPreviewer.getHeight();

        final Bitmap croppedBitmap = Bitmap.createBitmap((int) canvasWidth, (int) canvasHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);

        Bitmap bitmap = tvVideoPreviewer.getBitmap();

        if (bitmap == null) {
            CommonUtils.showToast(MainActivity.this, "bitmap == null");
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
            CommonUtils.showToast(MainActivity.this, "ms: " + (System.currentTimeMillis() - start));
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
     * KCF初始化
     *
     * @param rectFForFrame
     * @param bitmapForTracking
     */
    public void trackingInitForKCF(RectF rectFForFrame, Bitmap bitmapForTracking) {
        if (bitmapForTracking != null) {
//            int[] pixels = new int[bitmapForTracking.getWidth() * bitmapForTracking.getHeight()];
//            bitmapForTracking.getPixels(pixels, 0, bitmapForTracking.getWidth(),
//                    0, 0, bitmapForTracking.getWidth(), bitmapForTracking.getHeight());
//            NativeHelper.getInstance().initKcf(pixels, rectFForFrame.left, rectFForFrame.top,
//                    rectFForFrame.right, rectFForFrame.bottom, bitmapForTracking.getWidth(), bitmapForTracking.getHeight());


            NativeHelper.getInstance().initKcf(bitmapForTracking, rectFForFrame.left, rectFForFrame.top,
                    rectFForFrame.right, rectFForFrame.bottom, bitmapForTracking.getWidth(), bitmapForTracking.getHeight());

            CommonUtils.showToast(MainActivity.this, "init" + rectFForFrame.left + " " + rectFForFrame.top + " " +
                    rectFForFrame.right + " " + rectFForFrame.bottom);
        } else {
            CommonUtils.showToast(MainActivity.this, "bitmapForKCF == null");
        }
    }

    /**
     * 通过KCF进行跟踪
     */
    @Override
    public void trackingForKCF() {
        canvasWidth = tvVideoPreviewer.getWidth();
        canvasHeight = tvVideoPreviewer.getHeight();
        ivImageViewForFrame.getLayoutParams().width = tvVideoPreviewer.getWidth();
        ivImageViewForFrame.getLayoutParams().height = tvVideoPreviewer.getHeight();

        final Bitmap croppedBitmap = Bitmap.createBitmap((int) canvasWidth, (int) canvasHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);

        Bitmap bitmap = tvVideoPreviewer.getBitmap();

        if (bitmap == null) {
            CommonUtils.showToast(MainActivity.this, "bitmap == null");
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
            CommonUtils.showToast(MainActivity.this, "ms: " + (System.currentTimeMillis() - start));
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
}