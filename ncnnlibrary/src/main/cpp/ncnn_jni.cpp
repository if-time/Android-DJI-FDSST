// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>

#include <ncnn/gpu.h>
#include <android/asset_manager_jni.h>
#include "YoloV5.h"
#include "YoloV4.h"

#define TAG "jni-log-ncnn" // 这个是自定义的LOG的标识
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    ncnn::create_gpu_instance();
    if (ncnn::get_gpu_count() > 0) {
        YoloV5::hasGPU = true;
        YoloV4::hasGPU = true;
    }
    LOGD("get_gpu_count %d", ncnn::get_gpu_count());
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    ncnn::destroy_gpu_instance();
    delete YoloV5::detector;
    delete YoloV4::detector;
    LOGD("jni onunload");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_ncnnlibrary_NcnnJni_nativeInitNcnn(JNIEnv *env, jclass clazz, jobject assetManager,
                                                    jboolean v4tiny, jboolean useGPU) {
    // TODO: implement nativeInitNcnn()
    if (YoloV4::detector != nullptr) {
        delete YoloV4::detector;
        YoloV4::detector = nullptr;
    }
    if (YoloV4::detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        if (v4tiny == 1) {
            YoloV4::detector = new YoloV4(mgr, "yolov4-tiny-person-opt.param",
                                          "yolov4-tiny-person-opt.bin", useGPU);
        }
    }
    LOGE("YoloV4 init is finish, status is ok.");

}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_example_ncnnlibrary_NcnnJni_nativeDetectForNcnn(JNIEnv *env, jclass clazz,
                                                         jobject bitmap) {
//     TODO: implement nativeDetectForNcnn()
    auto result = YoloV4::detector->detect(env, bitmap);
    auto box_cls = env->FindClass("com/example/ncnnlibrary/Box");
    auto cid = env->GetMethodID(box_cls, "<init>", "(FFFFIF)V");
    jobjectArray ret = env->NewObjectArray(result.size(), box_cls, nullptr);
    int i = 0;
    for (auto &box:result) {
        env->PushLocalFrame(1);
        jobject obj = env->NewObject(box_cls, cid, box.x1, box.y1, box.x2, box.y2, box.label,
                                     box.score);
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }
    return ret;
}

// ncnn


//static ncnn::UnlockedPoolAllocator g_blob_pool_allocator;
//static ncnn::PoolAllocator g_workspace_pool_allocator;
//
//static ncnn::Mat ncnn_param;
//static ncnn::Mat ncnn_bin;
//static ncnn::Net ncnn_net;


//extern "C"
//JNIEXPORT jboolean JNICALL
//Java_com_example_ncnnlibrary_NcnnJni_init(JNIEnv *env, jobject thiz, jbyteArray param,
//                                          jbyteArray bin) {
//    // TODO: implement init()
//    // init param
//    {
//        int len = env->GetArrayLength(param);
//        ncnn_param.create(len, (size_t) 1u);
//        env->GetByteArrayRegion(param, 0, len, (jbyte *) ncnn_param);
//        int ret = ncnn_net.load_param((const unsigned char *) ncnn_param);
//        __android_log_print(ANDROID_LOG_DEBUG, "NcnnJni", "load_param %d %d", ret, len);
//    }
//
//    // init bin
//    {
//        int len = env->GetArrayLength(bin);
//        ncnn_bin.create(len, (size_t) 1u);
//        env->GetByteArrayRegion(bin, 0, len, (jbyte *) ncnn_bin);
//        int ret = ncnn_net.load_model((const unsigned char *) ncnn_bin);
//        __android_log_print(ANDROID_LOG_DEBUG, "NcnnJni", "load_model %d %d", ret, len);
//    }
//
//    ncnn::Option opt;
//    opt.lightmode = true;
//    opt.num_threads = 4;
//    opt.blob_allocator = &g_blob_pool_allocator;
//    opt.workspace_allocator = &g_workspace_pool_allocator;
//
//    ncnn::set_default_option(opt);
//
//    return JNI_TRUE;
//}
//
//extern "C"
//JNIEXPORT jfloatArray JNICALL
//Java_com_example_ncnnlibrary_NcnnJni_detect(JNIEnv *env, jobject thiz, jobject bitmap) {
//    // TODO: implement detect()
//    // ncnn from bitmap
//    ncnn::Mat in;
//    {
//        AndroidBitmapInfo info;
//        AndroidBitmap_getInfo(env, bitmap, &info);
//        int width = info.width;
//        int height = info.height;
//        if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
//            return NULL;
//
//        void *indata;
//        AndroidBitmap_lockPixels(env, bitmap, &indata);
//        // 把像素转换成data，并指定通道顺序
//        in = ncnn::Mat::from_pixels((const unsigned char *) indata, ncnn::Mat::PIXEL_RGBA2BGR,
//                                    width, height);
//
//        AndroidBitmap_unlockPixels(env, bitmap);
//    }
//
//    // ncnn_net
//    std::vector<float> cls_scores;
//    {
//        // 减去均值和乘上比例
//        const float mean_vals[3] = {103.94f, 116.78f, 123.68f};
//        const float scale[3] = {0.017f, 0.017f, 0.017f};
//
//        in.substract_mean_normalize(mean_vals, scale);
//
//        ncnn::Extractor ex = ncnn_net.create_extractor();
//        // 如果时不加密是使用ex.input("data", in);
//        ex.input(mobilenet_v2_param_id::BLOB_data, in);
//
//        ncnn::Mat out;
//        // 如果时不加密是使用ex.extract("prob", out);
//        ex.extract(mobilenet_v2_param_id::BLOB_prob, out);
//
//        int output_size = out.w;
//        jfloat *output[output_size];
//        for (int j = 0; j < out.w; j++) {
//            output[j] = &out[j];
//        }
//
//        jfloatArray jOutputData = env->NewFloatArray(output_size);
//        if (jOutputData == nullptr) return nullptr;
//        env->SetFloatArrayRegion(jOutputData, 0, output_size,
//                                 reinterpret_cast<const jfloat *>(*output));  // copy
//
//        return jOutputData;
//    }
//}
