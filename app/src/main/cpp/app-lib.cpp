#include <jni.h>
#include <math.h>

#include <exception>
#include <string>
#include <iostream>

#include <android/bitmap.h>
#include <android/log.h>
#include <opencv2/video/tracking.hpp>
#include <opencv2/tracking/tracker.hpp>
#include <opencv2/opencv.hpp>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fdssttracker.hpp>

using namespace cv;
using namespace std;

#define TAG "jni-log-libyuv" // 这个是自定义的LOG的标识
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型

void BitmapToMat2(JNIEnv *env, jobject &bitmap, Mat &mat, jboolean needUnPremultiplyAlpha);

void writeImage(const Mat &frame);

jmethodID dataCallbackMID;

/**
 * Invoke the java callback method
 */
void invokeFrameDataCallback(JNIEnv *env, jobject obj, uint8_t *buf, int size, int frameNum,
                             int isKeyFrame, int width, int height) {
    jbyte *buff = (jbyte *) buf;
    jbyteArray jarray = (env)->NewByteArray(size);
    (env)->SetByteArrayRegion(jarray, 0, size, buff);
    (env)->CallVoidMethod(obj, dataCallbackMID, jarray, size, frameNum, isKeyFrame != 0, width,
                          height);
}

void MatToBitmap2
        (JNIEnv *env, Mat &mat, jobject &bitmap, jboolean needPremultiplyAlpha) {
    AndroidBitmapInfo info;
    void *pixels = 0;
    Mat &src = mat;

    try {
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                  info.format == ANDROID_BITMAP_FORMAT_RGB_565);
        CV_Assert(src.dims == 2 && info.height == (uint32_t) src.rows &&
                  info.width == (uint32_t) src.cols);
        CV_Assert(src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if (src.type() == CV_8UC1) {
                cvtColor(src, tmp, COLOR_GRAY2RGBA);
            } else if (src.type() == CV_8UC3) {
                cvtColor(src, tmp, COLOR_RGB2RGBA);
            } else if (src.type() == CV_8UC4) {
                if (needPremultiplyAlpha)
                    cvtColor(src, tmp, COLOR_RGBA2mRGBA);
                else
                    src.copyTo(tmp);
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if (src.type() == CV_8UC1) {
                cvtColor(src, tmp, COLOR_GRAY2BGR565);
            } else if (src.type() == CV_8UC3) {
                cvtColor(src, tmp, COLOR_RGB2BGR565);
            } else if (src.type() == CV_8UC4) {
                cvtColor(src, tmp, COLOR_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch (const cv::Exception &e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("org/opencv/core/CvException");
        if (!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return;
    }
}

void BitmapToMat(JNIEnv *env, jobject &bitmap, Mat &mat) {
    BitmapToMat2(env, bitmap, mat, false);
}

void BitmapToMat2(JNIEnv *env, jobject &bitmap, Mat &mat, jboolean needUnPremultiplyAlpha) {
    AndroidBitmapInfo info;
    void *pixels = 0;
    Mat &dst = mat;

    try {
        LOGD("nBitmapToMat");
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                  info.format == ANDROID_BITMAP_FORMAT_RGB_565);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);
        dst.create(info.height, info.width, CV_8UC4);
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            LOGD("nBitmapToMat: RGBA_8888 -> CV_8UC4");
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if (needUnPremultiplyAlpha) cvtColor(tmp, dst, COLOR_mRGBA2RGBA);
            else tmp.copyTo(dst);
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            LOGD("nBitmapToMat: RGB_565 -> CV_8UC4");
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(tmp, dst, COLOR_BGR5652RGBA);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch (const cv::Exception &e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("nBitmapToMat catched cv::Exception: %s", e.what());
        jclass je = env->FindClass("org/opencv/core/CvException");
        if (!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("nBitmapToMat catched unknown exception (...)");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nBitmapToMat}");
        return;
    }
}

void MatToBitmap(JNIEnv *env, Mat &mat, jobject &bitmap) {
    MatToBitmap2(env, mat, bitmap, false);
}

/**********************************************KCF*****************************************************/
//初始化跟踪器
Ptr<Tracker> tracker;
Rect2d bbox;

int countKcf = 0;

extern "C"
JNIEXPORT void JNICALL
Java_com_dji_FPVDemo_jni_NativeHelper_initKcf(JNIEnv *env, jobject thiz, jobject src_bitmap,
                                              jfloat left, jfloat top,
                                              jfloat right, jfloat bottom, jint width,
                                              jint height) {
    // TODO: implement initKcf()
    LOGE("tracker initing in JNI...");

    Mat frame;
    BitmapToMat(env, src_bitmap, frame);//图片转化成mat
    cvtColor(frame, frame, COLOR_BGRA2RGB);

    bbox.x = left;
    bbox.y = top;
    bbox.width = right - left;
    bbox.height = bottom - top;

    rectangle(frame, bbox, Scalar(255, 0, 0), 2, 1);
//    imwrite("/storage/emulated/0/result/readYuvkcf.jpg", frame);
    __android_log_print(ANDROID_LOG_ERROR, "mat_jni",
                        "frame.rows: %d, frame.cols: %d, frame.type(): %d",
                        frame.rows, frame.cols, frame.type());
    tracker = TrackerKCF::create();
    LOGE("tracker init finished in JNI...");

    //跟踪器初始化
    tracker->init(frame, bbox);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_dji_FPVDemo_jni_NativeHelper_usingKcf(JNIEnv *env, jobject thiz,
                                               jobject src_bitmap,
                                               jint width, jint height) {
    // TODO: implement usingKcf()

    jclass cSructInfo = env->FindClass(
            "com/dji/FPVDemo/tracking/TrackingResultFormJNI");
    jfieldID cXLoc = env->GetFieldID(cSructInfo, "x", "I");
    jfieldID cYLoc = env->GetFieldID(cSructInfo, "y", "I");
    jfieldID cWidthLoc = env->GetFieldID(cSructInfo, "width", "I");
    jfieldID cHeightLoc = env->GetFieldID(cSructInfo, "height", "I");
    //新建Jni类对象
    jobject oStructInfo = env->AllocObject(cSructInfo);

    Mat frame;
    BitmapToMat(env, src_bitmap, frame);//图片转化成mat
    cvtColor(frame, frame, COLOR_BGRA2RGB);
    __android_log_print(ANDROID_LOG_ERROR, "mat_jni",
                        "frame.rows: %d, frame.cols: %d, frame.type(): %d",
                        frame.rows, frame.cols, frame.type());
    bool ok = tracker->update(frame, bbox);

    if (ok) {
//        rectangle(frame, bbox, Scalar(255, 0, 0), 2, 1);
//        ostringstream oss;
//        oss << "/storage/emulated/0/result/readYuvrectangle" << countKcf++ << ".jpg";
//        cout << oss.str() << endl;
//
//        imwrite(oss.str(), frame);
        LOGE("update is finish,status is ok.");
        int x = bbox.x;
        int y = bbox.y;
        int width = bbox.width;
        int height = bbox.height;
        env->SetIntField(oStructInfo, cXLoc, x);
        env->SetIntField(oStructInfo, cYLoc, y);
        env->SetIntField(oStructInfo, cWidthLoc, width);
        env->SetIntField(oStructInfo, cHeightLoc, height);
    } else {
        LOGE("update is finish,status is not ok.");
        env->SetIntField(oStructInfo, cXLoc, 0);
        env->SetIntField(oStructInfo, cYLoc, 0);
        env->SetIntField(oStructInfo, cWidthLoc, 0);
        env->SetIntField(oStructInfo, cHeightLoc, 0);
    }

    return oStructInfo;
}
/**********************************************KCF*****************************************************/

/**********************************************FDSST*****************************************************/
//初始化跟踪器
Rect2d bboxForF;
Point center;
//动态数组存储坐标点
vector<Point2d> points;
Mat tmp, dst;

int countFdsst = 0;

bool HOG = true;
bool FIXEDWINDOW = false;
bool MULTISCALE = true;
bool SILENT = true;
bool LAB = false;
// Create DSSTTracker tracker object
FDSSTTracker trackerForF(HOG, FIXEDWINDOW, MULTISCALE, LAB);

extern "C"
JNIEXPORT void JNICALL
Java_com_dji_FPVDemo_jni_NativeHelper_initFdsst(JNIEnv *env, jobject thiz,
                                                jobject src_bitmap,
                                                jfloat left, jfloat top,
                                                jfloat right, jfloat bottom,
                                                jint width,
                                                jint height) {
    // TODO: implement initFdsst()
    LOGE("tracker initing in JNI...");

    Mat frame;
    BitmapToMat(env, src_bitmap, frame);//图片转化成mat
    cvtColor(frame, frame, COLOR_BGRA2RGB);

    __android_log_print(ANDROID_LOG_ERROR, "mat_jni",
                        "frame.rows: %d, frame.cols: %d, frame.type(): %d",
                        frame.rows, frame.cols, frame.type());
    tracker = TrackerKCF::create();
    LOGE("tracker init finished in JNI...");
    bboxForF.x = left;
    bboxForF.y = top;
    bboxForF.width = right - left;
    bboxForF.height = bottom - top;
    rectangle(frame, bboxForF, Scalar(255, 0, 0), 2, 1);

    imwrite("/storage/emulated/0/result/readYuvfdsst.jpg", frame);
    //跟踪器初始化
    cvtColor(frame, dst, CV_BGR2GRAY);
    trackerForF.init(bboxForF, dst);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_dji_FPVDemo_jni_NativeHelper_usingFdsst(JNIEnv *env, jobject thiz,
                                                 jobject src_bitmap,
                                                 jint width, jint height) {
    // TODO: implement usingFdsst()

    jclass cSructInfo = env->FindClass(
            "com/dji/FPVDemo/tracking/TrackingResultFormJNI");
    jfieldID cXLoc = env->GetFieldID(cSructInfo, "x", "I");
    jfieldID cYLoc = env->GetFieldID(cSructInfo, "y", "I");
    jfieldID cWidthLoc = env->GetFieldID(cSructInfo, "width", "I");
    jfieldID cHeightLoc = env->GetFieldID(cSructInfo, "height", "I");
    //新建Jni类对象
    jobject oStructInfo = env->AllocObject(cSructInfo);


    Mat frame;
    BitmapToMat(env, src_bitmap, frame);//图片转化成mat
    cvtColor(frame, frame, COLOR_BGRA2RGB);

    __android_log_print(ANDROID_LOG_ERROR, "mat_jni",
                        "frame.rows: %d, frame.cols: %d, frame.type(): %d",
                        frame.rows, frame.cols, frame.type());

    cvtColor(frame, dst, CV_RGB2GRAY);
    bboxForF = trackerForF.update(dst);

    LOGE("update is finish,status is ok.");
    int bboxx = bboxForF.x;
    int bboxy = bboxForF.y;
    int bboxwidth = bboxForF.width;
    int bboxheight = bboxForF.height;
    env->SetIntField(oStructInfo, cXLoc, bboxx);
    env->SetIntField(oStructInfo, cYLoc, bboxy);
    env->SetIntField(oStructInfo, cWidthLoc, bboxwidth);
    env->SetIntField(oStructInfo, cHeightLoc, bboxheight);

//    ostringstream oss;
//    oss << "/storage/emulated/0/result/readFdsstRectangle" << countFdsst++ << ".jpg";
//    cout << oss.str() << endl;
//    imwrite(oss.str(), frame);


//    writeImage(frame);

    return oStructInfo;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_dji_FPVDemo_jni_NativeHelper_usingFdsstMat(JNIEnv *env, jobject thiz, jlong mat_address,
                                                    jint width, jint height) {
    // TODO: implement usingFdsstMat()

    jclass cSructInfo = env->FindClass(
            "com/dji/FPVDemo/tracking/TrackingResultFormJNI");
    jfieldID cXLoc = env->GetFieldID(cSructInfo, "x", "I");
    jfieldID cYLoc = env->GetFieldID(cSructInfo, "y", "I");
    jfieldID cWidthLoc = env->GetFieldID(cSructInfo, "width", "I");
    jfieldID cHeightLoc = env->GetFieldID(cSructInfo, "height", "I");
    //新建Jni类对象
    jobject oStructInfo = env->AllocObject(cSructInfo);

    Mat &frame = *(Mat *) mat_address;

    __android_log_print(ANDROID_LOG_ERROR, "mat_jni",
                        "frame.rows: %d, frame.cols: %d, frame.type(): %d",
                        frame.rows, frame.cols, frame.type());

    cvtColor(frame, dst, CV_RGB2GRAY);
    bboxForF = trackerForF.update(dst);

    LOGE("update is finish,status is ok.");
    int bboxx = bboxForF.x;
    int bboxy = bboxForF.y;
    int bboxwidth = bboxForF.width;
    int bboxheight = bboxForF.height;
    env->SetIntField(oStructInfo, cXLoc, bboxx);
    env->SetIntField(oStructInfo, cYLoc, bboxy);
    env->SetIntField(oStructInfo, cWidthLoc, bboxwidth);
    env->SetIntField(oStructInfo, cHeightLoc, bboxheight);

//    ostringstream oss;
//    oss << "/storage/emulated/0/result/readFdsstRectangle" << countFdsst++ << ".jpg";
//    cout << oss.str() << endl;
//    imwrite(oss.str(), frame);


//    writeImage(frame);
//    frame.release();
    return oStructInfo;

}

/**********************************************FDSST*****************************************************/

void writeImage(const Mat &frame) {
    char p_str[128] = "/storage/emulated/0/ResultForFDSST/";
    if (0 == access(p_str, 0)) {
        printf("[ %s ] live !", p_str);
    } else {
        if (0 == mkdir(p_str, 777)) {
            printf("[ %s ] mkdir success !", p_str);
        } else {
            printf("[ %s ] mkdir error !", p_str);
        }
    }

    const int len = strlen(p_str);
    sprintf(p_str + len, "cv_mat_%lf_ms_%dx%d.jpg", getTickCount() * 1000. / getTickFrequency(),
            frame.cols, frame.rows);
    imwrite(p_str, frame);
}

/**********************************************TEST*****************************************************/
extern "C"
JNIEXPORT jintArray JNICALL
Java_com_dji_FPVDemo_test_PictureConversionTestActivity_gray(JNIEnv *env,
                                                             jobject thiz,
                                                             jintArray pix_,
                                                             jint w,
                                                             jint h) {
    // TODO: implement gray()
    jint *pix = env->GetIntArrayElements(pix_, NULL);
    if (pix == NULL) {
        return 0;
    }
#if 1
    //将c++图片转成Opencv图片
    Mat imgData(h, w, CV_8UC4, (unsigned char *) pix);
    uchar *ptr = imgData.ptr(0);
    for (int i = 0; i < w * h; i++) {
        //计算公式：Y(亮度) = 0.299*R + 0.587*G + 0.114*B
        //对于一个int四字节，其彩色值存储方式为：BGRA
        int grayScale = (int) (ptr[4 * i + 2] * 0.299 + ptr[4 * i + 1] * 0.587 +
                               ptr[4 * i + 0] * 0.114);
        ptr[4 * i + 1] = grayScale;
        ptr[4 * i + 2] = grayScale;
        ptr[4 * i + 0] = grayScale;
    }
#endif
    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, pix);
    env->ReleaseIntArrayElements(pix_, pix, 0);
    return result;

}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_dji_FPVDemo_test_imageopencv_PictureConversionTestActivity_gray(JNIEnv *env, jobject thiz,
                                                                         jintArray pix_, jint w,
                                                                         jint h) {
    // TODO: implement gray()
    jint *pix = env->GetIntArrayElements(pix_, NULL);
    if (pix == NULL) {
        return 0;
    }
#if 1
    //将c++图片转成Opencv图片
    Mat imgData(h, w, CV_8UC4, (unsigned char *) pix);
    uchar *ptr = imgData.ptr(0);
    for (int i = 0; i < w * h; i++) {
        //计算公式：Y(亮度) = 0.299*R + 0.587*G + 0.114*B
        //对于一个int四字节，其彩色值存储方式为：BGRA
        int grayScale = (int) (ptr[4 * i + 2] * 0.299 + ptr[4 * i + 1] * 0.587 +
                               ptr[4 * i + 0] * 0.114);
        ptr[4 * i + 1] = grayScale;
        ptr[4 * i + 2] = grayScale;
        ptr[4 * i + 0] = grayScale;
    }
#endif
    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, pix);
    env->ReleaseIntArrayElements(pix_, pix, 0);

    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dji_FPVDemo_test_imageopencv_PictureConversionTestActivity_imwriter(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jobject src_bitmap) {
    // TODO: implement imwriter()
    Mat frame;
    BitmapToMat(env, src_bitmap, frame);//图片转化成mat
    cvtColor(frame, frame, COLOR_BGRA2RGB);

    __android_log_print(ANDROID_LOG_ERROR, "mat_jni",
                        "frame.rows: %d, frame.cols: %d, frame.type(): %d",
                        frame.rows, frame.cols, frame.type());

    ostringstream oss;
    oss << "/storage/emulated/0/result/readFdsstRectangle.jpg";
    cout << oss.str() << endl;
    bool iswrite = imwrite(oss.str(), frame);
    __android_log_print(ANDROID_LOG_ERROR, "mat_jni",
                        "iswrite: %d",
                        iswrite);
}