package com.dji.FPVDemo.utils;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


/**
 *
 * @date 2020/10/31 16:32
 * @author dongsiyuan
 */
public class ImageUtils {

    /**
     * 使用libyuv通过Bitmap获得nv21
     * @param bitmap
     * @return
     */
    public static byte[] libyuvToNv21(Bitmap bitmap) {

        int srcYuvLength;
        int srcArgbLength;
        int dstYuvLength;

        long beginTimeMS = System.currentTimeMillis();
        long costTimeMS;
        srcYuvLength = bitmap.getWidth() * bitmap.getHeight() * 3 / 2;
        srcArgbLength = bitmap.getWidth() * bitmap.getHeight() * 4; // In format argb8888, per pixel cost 4 bytes, if it's rgb565, you should calculate the length of srcArgb by yourself.
        byte[] dstYuv;

        dstYuvLength = bitmap.getWidth() * bitmap.getHeight() * 3 / 2;
        YuvUtils.allocateMemo(srcYuvLength, srcArgbLength, dstYuvLength);
        dstYuv = new byte[dstYuvLength];
        YuvUtils.rgbToYuvBylibyuv(bitmap, dstYuv);

        costTimeMS = System.currentTimeMillis() - beginTimeMS;
        Log.i("dongpredict_image", "run: libyuvToNv21 : " + costTimeMS + "ms");
        YuvUtils.releaseMemo();

        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }

        return dstYuv;
    }

    /**
     * 封装的JNI接口方法直接接收cv::Mat
     * @param bitmap
     * @return
     */
    public static Mat getMatForBitmap(Bitmap bitmap) {
//        Mat mat = new Mat(bitmap.getWidth(), bitmap.getHeight(), Imgproc.COLOR_BGRA2BGR);
        Mat mat = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);
//        Log.i("ncnnopencv", "getMatForBitmap: " + mat.type());
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR);
        bmp32.recycle();
        return mat;
    }

}
