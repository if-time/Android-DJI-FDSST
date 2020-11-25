package com.dji.FPVDemo.tracking;

/**
 * KCF、FDSST等追踪返回结果
 *
 * @author dongsiyuan
 * @since 2020/11/24 14:48
 */
public class TrackingResultFormJNI {
    public int x;
    public int y;
    public int width;
    public int height;

    @Override
    public String toString() {
        return "ResultFormJNI{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
