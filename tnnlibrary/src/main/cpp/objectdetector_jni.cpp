//
// Created by Dong on 2020/12/8.
//
#include "objectdetector_jni.h"
#include "object_detector_yolo.h"
#include <jni.h>
#include <android/bitmap.h>
#include "tnn/utils/mat_utils.h"

static std::shared_ptr<TNN_NS::ObjectDetectorYolo> gDetector;
static int gComputeUnitType = 0; // 0 is cpu, 1 is gpu, 2 is huawei_npu
static jclass clsObjectInfo;
static jmethodID midconstructorObjectInfo;
static jfieldID fidx1;
static jfieldID fidy1;
static jfieldID fidx2;
static jfieldID fidy2;
static jfieldID fidscore;
static jfieldID fidlandmarks;
static jfieldID fidcls;

extern "C"
JNIEXPORT jint JNICALL
TNN_OBJECT_DETECTOR(init)(JNIEnv *env, jobject thiz, jstring modelPath, jint width, jint height,
                          jfloat scoreThreshold, jfloat iouThreshold, jint topk,
                          jint computUnitType) {
    // TODO: implement init()
// Reset bench description
    setBenchResult("");
    std::vector<int> nchw = {1, 3, height, width};
    gDetector = std::make_shared<TNN_NS::ObjectDetectorYolo>();
    std::string protoContent, modelContent;
    std::string modelPathStr(jstring2stringForDetection(env, modelPath));
    protoContent = fdLoadFileForDetection(modelPathStr + "/yolov5s-permute.tnnproto");
    modelContent = fdLoadFileForDetection(modelPathStr + "/yolov5s.tnnmodel");
    LOGI("proto content size %d model content size %d", protoContent.length(),
         modelContent.length());
    gComputeUnitType = computUnitType;

    TNN_NS::Status status = TNN_NS::TNN_OK;
    auto option = std::make_shared<TNN_NS::TNNSDKOption>();
    option->compute_units = TNN_NS::TNNComputeUnitsCPU;
    option->library_path = "";
    option->proto_content = protoContent;
    option->model_content = modelContent;
    LOGI("the device type  %d device huawei_npu", gComputeUnitType);
    if (gComputeUnitType == 1) {
        option->compute_units = TNN_NS::TNNComputeUnitsGPU;
        status = gDetector->Init(option);
    } else if (gComputeUnitType == 2) {
        //add for huawei_npu store the om file
        option->compute_units = TNN_NS::TNNComputeUnitsHuaweiNPU;
        gDetector->setNpuModelPath(modelPathStr + "/");
        gDetector->setCheckNpuSwitch(false);
        status = gDetector->Init(option);
    } else {
        option->compute_units = TNN_NS::TNNComputeUnitsCPU;
        status = gDetector->Init(option);
    }

    if (status != TNN_NS::TNN_OK) {
        LOGE("detector init failed %d", (int) status);
        return -1;
    }

    if (clsObjectInfo == NULL) {
        clsObjectInfo = static_cast<jclass>(env->NewGlobalRef(
                env->FindClass("com/dji/FPVDemo/tnn/ObjectInfo")));
        midconstructorObjectInfo = env->GetMethodID(clsObjectInfo, "<init>", "()V");
        fidx1 = env->GetFieldID(clsObjectInfo, "x1", "F");
        fidy1 = env->GetFieldID(clsObjectInfo, "y1", "F");
        fidx2 = env->GetFieldID(clsObjectInfo, "x2", "F");
        fidy2 = env->GetFieldID(clsObjectInfo, "y2", "F");
        fidscore = env->GetFieldID(clsObjectInfo, "score", "F");
        fidlandmarks = env->GetFieldID(clsObjectInfo, "landmarks", "[F");
        fidcls = env->GetFieldID(clsObjectInfo, "class_id", "I");
    }

    return 0;
}

JNIEXPORT jobjectArray JNICALL
TNN_OBJECT_DETECTOR(detectFromImage)(JNIEnv *env, jobject thiz, jobject imageSource, jint width,
                                     jint height) {
    jobjectArray objectInfoArray;
    int ret = -1;
    AndroidBitmapInfo sourceInfocolor;
    void *sourcePixelscolor;

    if (AndroidBitmap_getInfo(env, imageSource, &sourceInfocolor) < 0) {
        return 0;
    }

    if (sourceInfocolor.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return 0;
    }

    if (AndroidBitmap_lockPixels(env, imageSource, &sourcePixelscolor) < 0) {
        return 0;
    }
    TNN_NS::BenchOption bench_option;
    bench_option.forward_count = 20;
    gDetector->SetBenchOption(bench_option);
    TNN_NS::DeviceType dt = TNN_NS::DEVICE_ARM;
    TNN_NS::DimsVector input_dims = {1, 4, height, width};
    auto input_mat = std::make_shared<TNN_NS::Mat>(dt, TNN_NS::N8UC4, input_dims,
                                                   sourcePixelscolor);

    TNN_NS::DimsVector target_dims = {1, 4, 448, 640};
    auto resize_mat = std::make_shared<TNN_NS::Mat>(dt, TNN_NS::N8UC4, target_dims);

    TNN_NS::ResizeParam param;
    TNN_NS::MatUtils::Resize(*input_mat, *resize_mat, param, NULL);

    float scale_h = height / 448.0f;
    float scale_w = width / 640.0f;

    auto asyncRefDetector = gDetector;
    std::vector<TNN_NS::ObjectInfo> objectInfoList;

    std::shared_ptr<TNN_NS::TNNSDKInput> input = std::make_shared<TNN_NS::TNNSDKInput>(resize_mat);
    std::shared_ptr<TNN_NS::TNNSDKOutput> output = std::make_shared<TNN_NS::TNNSDKOutput>();

    TNN_NS::Status status = asyncRefDetector->Predict(input, output);
    AndroidBitmap_unlockPixels(env, imageSource);

    asyncRefDetector->ProcessSDKOutput(output);
    objectInfoList = dynamic_cast<TNN_NS::ObjectDetectorYoloOutput *>(output.get())->object_list;

    if (status != TNN_NS::TNN_OK) {
        LOGE("failed to detect %d", (int) status);
        return 0;
    }

    LOGI("object info list size %d", objectInfoList.size());
    // TODO: copy object info list
    if (objectInfoList.size() > 0) {
        objectInfoArray = env->NewObjectArray(objectInfoList.size(), clsObjectInfo, NULL);
        for (int i = 0; i < objectInfoList.size(); i++) {
            jobject objObjectInfo = env->NewObject(clsObjectInfo, midconstructorObjectInfo);
            int landmarkNum = objectInfoList[i].key_points.size();
            LOGI("object[%d] %f %f %f %f score %f landmark size %d", i, objectInfoList[i].x1,
                 objectInfoList[i].y1, objectInfoList[i].x2, objectInfoList[i].y2,
                 objectInfoList[i].score, landmarkNum);
            env->SetFloatField(objObjectInfo, fidx1, objectInfoList[i].x1 * scale_w);
            env->SetFloatField(objObjectInfo, fidy1, objectInfoList[i].y1 * scale_h);
            env->SetFloatField(objObjectInfo, fidx2, objectInfoList[i].x2 * scale_w);
            env->SetFloatField(objObjectInfo, fidy2, objectInfoList[i].y2 * scale_h);
            env->SetFloatField(objObjectInfo, fidscore, objectInfoList[i].score);
            env->SetIntField(objObjectInfo, fidcls, objectInfoList[i].class_id);
            env->SetObjectArrayElement(objectInfoArray, i, objObjectInfo);
            env->DeleteLocalRef(objObjectInfo);
        }
        return objectInfoArray;
    } else {
        return 0;
    }
}

JNIEXPORT jint JNICALL TNN_OBJECT_DETECTOR(deinit)(JNIEnv *env, jobject thiz) {

    gDetector = nullptr;
    return 0;
}

extern "C"
JNIEXPORT jstring JNICALL
TNN_OBJECT_DETECTOR(getBenchResult)(JNIEnv *env, jobject thiz) {
    // TODO: implement getBenchResult()
    return string2jstringForDetection(env, gBenchResultStr.c_str());
}