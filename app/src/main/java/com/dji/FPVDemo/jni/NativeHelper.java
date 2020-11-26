package com.dji.FPVDemo.jni;


import com.dji.FPVDemo.tracking.TrackingResultFormJNI;

/**
 * @author dongsiyuan
 * @date 2020年10月29日
 */
public class NativeHelper {

    static{
        System.loadLibrary("app-lib");
    }

    public static final String TAG = NativeHelper.class.getSimpleName();

    private static NativeHelper instance;

    public static NativeHelper getInstance() {
        if (instance == null) {
            instance = new NativeHelper();
        }
        return instance;
    }

    private NativeHelper() {
    }

    private NativeDataListener dataListener;

    public void setDataListener(NativeDataListener dataListener) {
        this.dataListener = dataListener;
    }

    /**********************************************KCF*****************************************************/
    public native void initKcf(Object srcBitmap, float left, float top, float right, float bottom, int width, int height);
    public native TrackingResultFormJNI usingKcf(Object srcBitmap, int width, int height);
    /**********************************************KCF*****************************************************/

    /**********************************************FDSST*****************************************************/
    public native void initFdsst(Object srcBitmap, float left, float top, float right, float bottom, int width, int height);

    public native TrackingResultFormJNI usingFdsst(Object srcBitmap, int width, int height);

    public native TrackingResultFormJNI usingFdsstMat(long matAddress, int width, int height);

    /**********************************************FDSST*****************************************************/

    public interface NativeDataListener {
        /**
         * Callback method for receiving the frame data from NativeHelper.
         * Note that this method will be invoke in framing thread, which means time consuming
         * processing should not in this thread, or the framing process will be blocked.
         * @param data
         * @param size
         * @param frameNum
         * @param isKeyFrame
         * @param width
         * @param height
         */
        void onDataRecv(byte[] data, int size, int frameNum, boolean isKeyFrame, int width, int height);
    }

    /**
     * Invoke by JNI
     * Callback the frame data.
     * @param buf
     * @param size
     * @param frameNum
     * @param isKeyFrame
     * @param width
     * @param height
     */
    public void onFrameDataRecv(byte[] buf, int size, int frameNum, boolean isKeyFrame, int width, int height) {
        if (dataListener != null) {
            dataListener.onDataRecv(buf, size, frameNum, isKeyFrame, width, height);
        }
    }
}
