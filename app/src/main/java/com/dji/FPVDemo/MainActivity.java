package com.dji.FPVDemo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;

import com.dji.FPVDemo.detection.ClassifierFromTensorFlow;
import com.dji.FPVDemo.jni.NativeHelper;
import com.dji.FPVDemo.tnn.ObjectInfo;
import com.dji.FPVDemo.tracking.TrackingResultFormJNI;
import com.dji.FPVDemo.utils.CommonUtils;
import com.dji.FPVDemo.utils.ImageUtils;
import com.dji.FPVDemo.utils.LogUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * 在这个类里进行识别跟踪
 *
 * @author dongsiyuan
 * @date 2020/11/12 15:42
 */
public class MainActivity extends DJIMainActivity {

    private static float canvasWidth = 0;
    private static float canvasHeight = 0;

    private float controlValueIncX;
    private float controlValueIncY;
    private float dxCenterScreenObject;
    private float dyCenterScreenObject;

    double total_fps = 0;
    int fps_count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void objectDetectForTNN() {
        long start = System.currentTimeMillis();
        Bitmap bitmap = tvVideoPreviewer.getBitmap();

        ObjectInfo[] objectInfoList;

        objectInfoList = mObjectDetector.detectFromImage(bitmap, bitmap.getWidth(), bitmap.getHeight());
        setFPS(1000 / (System.currentTimeMillis() - start));
        CommonUtils.showToast(this, (System.currentTimeMillis() - start) + "");
        if (objectInfoList != null) {
            for (int i = 0; i < objectInfoList.length; i++) {
//            rects.add(new Rect((int)objectInfoList[i].x1, (int)objectInfoList[i].y1, (int)objectInfoList[i].x2, (int)objectInfoList[i].y2));
//            labels.add(String.format("%s : %f", label_list[objectInfoList[i].class_id], objectInfoList[i].score));
            }
        }
    }

    @Override
    public void imageClassifyForTNN() {
        Bitmap bitmap = tvVideoPreviewer.getBitmap();
        try {
            // 预测图像
            long start = System.currentTimeMillis();
            float[] result = imageClassifyUtil.predictForTNN(bitmap);

            bitmap.recycle();
            Log.i("imageClassifyUtil", "imageClassifyForTNN: " +
                    "预测结果标签：" + (int) result[0] +
                    "名称：" + classNames.get((int) result[0]) +
                    "概率：" + result[1]);

            StringBuffer sb = new StringBuffer();
            LogUtil.addLineToSB(sb, "预测结果标签: ", (int) result[0]);
            LogUtil.addLineToSB(sb, "名称：", classNames.get((int) result[0]));
            LogUtil.addLineToSB(sb, "概率：", result[1]);
            long end = System.currentTimeMillis();
            float fps = (float) (1000.0 / (end - start));
            total_fps = (total_fps == 0) ? fps : (total_fps + fps);
            fps_count++;

            LogUtil.addLineToSB(sb, "AVG_FPS: ", ((float) total_fps / fps_count));
            setResultToText(sb.toString());

            setFPS(1000 / (System.currentTimeMillis() - start));
        } catch (Exception e) {
            e.printStackTrace();
        }
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

            bitmap.recycle();

            final List<ClassifierFromTensorFlow.Recognition> mappedRecognitions = new LinkedList<ClassifierFromTensorFlow.Recognition>();

            for (final ClassifierFromTensorFlow.Recognition result : results) {
                final RectF location = result.getLocation();

                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {

                    RectF locationDisplay = new RectF(tvVideoPreviewer.getWidth() * location.left / TF_OD_API_INPUT_SIZE,
                            tvVideoPreviewer.getHeight() * location.top / TF_OD_API_INPUT_SIZE,
                            tvVideoPreviewer.getWidth() * location.right / TF_OD_API_INPUT_SIZE,
                            tvVideoPreviewer.getHeight() * location.bottom / TF_OD_API_INPUT_SIZE);
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
    @Override
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

        Bitmap bitmap = tvVideoPreviewer.getBitmap();

        if (bitmap == null) {
            CommonUtils.showToast(MainActivity.this, "bitmap == null");
            return;
        } else {
            // 获取到识别出的位置并画框
            long start = System.currentTimeMillis();
//            int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
//            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
//            TrackingResultFormJNI result = NativeHelper.getInstance().usingFdsst(bitmap, bitmap.getWidth(), bitmap.getHeight());
            TrackingResultFormJNI result = NativeHelper.getInstance().usingFdsstMat(ImageUtils.getMatForBitmap(bitmap).getNativeObjAddr(), bitmap.getWidth(), bitmap.getHeight());

            bitmap.recycle();

            setFPS(1000 / (System.currentTimeMillis() - start));
            writeAprilTagsStatus(result.x, result.y, result.width + result.x, result.height + result.y);
            pictureFrame(result);
        }
    }

    /**
     * KCF初始化
     *
     * @param rectFForFrame
     * @param bitmapForTracking
     */
    @Override
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
//            TrackingResultFormJNI result = NativeHelper.getInstance().usingKcf(pixels, bitmap.getWidth(), bitmap.getHeight());
            TrackingResultFormJNI result = NativeHelper.getInstance().usingKcf(bitmap, bitmap.getWidth(), bitmap.getHeight());
            bitmap.recycle();
            setFPS(1000 / (System.currentTimeMillis() - start));
            pictureFrame(result);
        }
    }

    private void pictureFrame(TrackingResultFormJNI result) {
        initCanvasAndImageView();

        final Bitmap croppedBitmap = Bitmap.createBitmap((int) canvasWidth, (int) canvasHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);
        paint.setAntiAlias(true);

        canvas.drawRect(result.x, result.y, result.width + result.x, result.height + result.y, paint);

        ivImageViewForFrame.post(new Runnable() {
            @Override
            public void run() {
                ivImageViewForFrame.setImageBitmap(croppedBitmap);
            }
        });
    }

    private void initCanvasAndImageView() {
        canvasWidth = tvVideoPreviewer.getWidth();
        canvasHeight = tvVideoPreviewer.getHeight();

        if (tvVideoPreviewer.getWidth() != ivImageViewForFrame.getWidth() || tvVideoPreviewer.getHeight() != ivImageViewForFrame.getHeight()) {
            canvasWidth = tvVideoPreviewer.getWidth();
            canvasHeight = tvVideoPreviewer.getHeight();
            ivImageViewForFrame.getLayoutParams().width = tvVideoPreviewer.getWidth();
            ivImageViewForFrame.getLayoutParams().height = tvVideoPreviewer.getHeight();
        }
    }

    private void clearFrame() {
        initCanvasAndImageView();
        final Bitmap croppedBitmap = Bitmap.createBitmap((int) canvasWidth, (int) canvasHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);
        ivImageViewForFrame.post(new Runnable() {
            @Override
            public void run() {
                ivImageViewForFrame.setImageBitmap(croppedBitmap);
            }
        });
    }

    /**
     * 控制飞行
     *
     * @param result
     */
    private void controlDroneFly(TrackingResultFormJNI result) {
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
    }
}