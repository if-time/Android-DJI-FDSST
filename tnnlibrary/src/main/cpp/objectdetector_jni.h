//
// Created by Dong on 2020/12/8.
//

#ifndef ANDROID_DJI_FDSST_OBJECTDETECTOR_JNI_H
#define ANDROID_DJI_FDSST_OBJECTDETECTOR_JNI_H

#include <jni.h>
#include <fstream>
#include <string>

#define TNN_OBJECT_DETECTOR(sig) Java_com_dji_FPVDemo_tnn_ObjectDetector_##sig
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jint JNICALL
TNN_OBJECT_DETECTOR(init)(JNIEnv *env, jobject thiz, jstring modelPath, jint width, jint height,
                          jfloat scoreThreshold, jfloat iouThreshold, jint topk,
                          jint computUnitType);

JNIEXPORT jint JNICALL TNN_OBJECT_DETECTOR(deinit)(JNIEnv *env, jobject thiz);

JNIEXPORT jobjectArray JNICALL
TNN_OBJECT_DETECTOR(detectFromImage)(JNIEnv *env, jobject thiz, jobject imageSource, jint width,
                                     jint height);
JNIEXPORT JNICALL jstring TNN_OBJECT_DETECTOR(getBenchResult)(JNIEnv *env, jobject thiz);

#ifdef __cplusplus

// Helper functions
static std::string gBenchResultStr = "";

void setBenchResult(std::string result) {
    gBenchResultStr = result;
}

std::string fdLoadFileForDetection(std::string path) {
    std::ifstream file(path, std::ios::in);
    if (file.is_open()) {
        file.seekg(0, file.end);
        int size = file.tellg();
        char *content = new char[size];
        file.seekg(0, file.beg);
        file.read(content, size);
        std::string fileContent;
        fileContent.assign(content, size);
        delete[] content;
        file.close();
        return fileContent;
    } else {
        return "";
    }
}

char *jstring2stringForDetection(JNIEnv *env, jstring jstr) {
    char *rtn = NULL;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte *ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char *) malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    return rtn;
}

jstring string2jstringForDetection(JNIEnv *env, const char *pat) {
    jclass strClass = (env)->FindClass("java/lang/String");
    jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = (env)->NewByteArray(strlen(pat));
    (env)->SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte *) pat);
    jstring encoding = (env)->NewStringUTF("GB2312");
    jstring r = (jstring) (env)->NewObject(strClass, ctorID, bytes, encoding);
    env->DeleteLocalRef(strClass);
    env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(encoding);
    return r;
}
}
#endif

#endif //ANDROID_DJI_FDSST_OBJECTDETECTOR_JNI_H
