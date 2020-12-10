package com.dji.FPVDemo.tnn;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;

/**
 * TNN的操作, 加载模型、预测
 * 
 * @author dongsiyuan
 * @since 2020/11/27 11:28
 */
public class ImageClassifyUtil {
    private static final int WIDTH = 224;
    private static final int HEIGHT = 224;

    public ImageClassifyUtil() {
        System.loadLibrary("tnn_wrapper");
    }

    public int initTNN(String modelPath, String protoPath, int computeUnitType) {
        return init(modelPath, protoPath, computeUnitType);
    }

    /**
     * 根据图片路径转Bitmap预测
     * @param image_path
     * @return
     * @throws Exception
     */
    public float[] predictForTNN(String image_path) throws Exception {
        if (!new File(image_path).exists()) {
            throw new Exception("image file is not exists!");
        }
        FileInputStream fis = new FileInputStream(image_path);
        Bitmap bitmap = BitmapFactory.decodeStream(fis);
        Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, false);
        float[] result = predictForTNN(scaleBitmap);
        if (bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return result;
    }

    /**
     * 直接使用Bitmap预测
     * @param bitmap
     * @return
     */
    public float[] predictForTNN(Bitmap bitmap) {
        Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, false);
        float[] results = predict(scaleBitmap, WIDTH, HEIGHT);
        int l = getMaxResult(results);
        return new float[]{l, results[l] * 0.01f};
    }


    /**
     * 获取概率最大的标签
     * @param result
     * @return
     */
    public static int getMaxResult(float[] result) {
        float probability = 0;
        int r = 0;
        for (int i = 0; i < result.length; i++) {
            if (probability < result[i]) {
                probability = result[i];
                r = i;
            }
        }
        return r;
    }

    public int deinitTNN() {
        return deinit();
    }


    private native int init(String modelPath, String protoPath, int computeUnitType);

    private native float[] predict(Bitmap image, int width, int height);

    private native int deinit();
}
