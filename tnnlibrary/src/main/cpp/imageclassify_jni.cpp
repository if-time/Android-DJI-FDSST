#include <jni.h>
#include <string>
#include "imageclassify_jni.h"
#include "tnn/core/tnn.h"
#include "tnn/core/blob.h"
#include <android/bitmap.h>
#include "tnn/core/common.h"
#include <cmath>

// shared_ptr（智能指针）
// 只要将 new 运算符返回的指针 p 交给一个 shared_ptr 对象“托管”，就不必担心在哪里写delete p语句
// ——实际上根本不需要编写这条语句，托管 p 的 shared_ptr 对象在消亡时会自动执行delete p。
// 而且，该 shared_ptr 对象能像指针 p —样使用，即假设托管 p 的 shared_ptr 对象叫作 ptr，那么 *ptr 就是 p 指向的对象。
std::shared_ptr<TNN_NS::TNN> net_ = nullptr;

// TNN网络构建需配置NetworkConfig，device_type可配置ARM， OPENCL， METAL等多种加速方式，通过CreateInst接口完成网络的构建。
// 华为NPU需要特殊指定network类型以及一个可选的cache路径。
// cache路径为存om文件的path,如("/data/local/tmp/")，空则表示不存om文件，每次运行都使用IR翻译并从内存读入模型。
std::shared_ptr<TNN_NS::Instance> instance_ = nullptr;
TNN_NS::DeviceType device_type_ = TNN_NS::DEVICE_ARM;

extern "C"
JNIEXPORT jint JNICALL
TNN_CLASSIFY(init)(JNIEnv *env, jobject thiz, jstring modelPath, jstring protoPath,
                   jint computeUnitType) {
    // TODO: implement init()
    std::string protoContent, modelContent;
    std::string modelPathStr(jstring2string(env, modelPath));
    std::string protoPathStr(jstring2string(env, protoPath));
    protoContent = fdLoadFile(protoPathStr);
    modelContent = fdLoadFile(modelPathStr);

    // TNN模型解析需配置ModelConfig params参数，传入proto和model文件内容，并调用TNN Init接口即可完成模型解析。
    TNN_NS::ModelConfig config;
    config.model_type = TNN_NS::MODEL_TYPE_TNN;
    config.params = {protoContent, modelContent};

    TNN_NS::Status status;
    auto net = std::make_shared<TNN_NS::TNN>();
    status = net->Init(config);
    net_ = net;

    device_type_ = TNN_NS::DEVICE_ARM;
    if (computeUnitType >= 1) {
        device_type_ = TNN_NS::DEVICE_OPENCL;
    }

    TNN_NS::InputShapesMap shapeMap;
    TNN_NS::NetworkConfig network_config;
    network_config.library_path = {""};
    network_config.device_type = device_type_;
    auto instance = net_->CreateInst(network_config, status, shapeMap);
    if (status != TNN_NS::TNN_OK || !instance) {
        // 如何出现GPU加载失败，自动切换CPU
        network_config.device_type = TNN_NS::DEVICE_ARM;
        instance = net_->CreateInst(network_config, status, shapeMap);
    }
    instance_ = instance;

    if (status != TNN_NS::TNN_OK) {
        LOGE("TNN init failed %d", (int) status);
        return -1;
    }
    return 0;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
TNN_CLASSIFY(predict)(JNIEnv *env, jobject thiz, jobject imageSource, jint width, jint height) {
    // TODO: implement predict()
    AndroidBitmapInfo sourceInfocolor;
    void *sourcePixelscolor;

    if (AndroidBitmap_getInfo(env, imageSource, &sourceInfocolor) < 0) {
        return nullptr;
    }

    if (sourceInfocolor.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return nullptr;
    }

    if (AndroidBitmap_lockPixels(env, imageSource, &sourcePixelscolor) < 0) {
        return nullptr;
    }

    TNN_NS::DeviceType dt = TNN_NS::DEVICE_ARM;
    TNN_NS::DimsVector target_dims = {1, 3, height, width};
    auto input_mat = std::make_shared<TNN_NS::Mat>(dt, TNN_NS::N8UC4, target_dims,
                                                   sourcePixelscolor);
    // step 1. set input mat
    TNN_NS::MatConvertParam input_cvt_param;
    input_cvt_param.scale = {1.0 / (255 * 0.229), 1.0 / (255 * 0.224), 1.0 / (255 * 0.225), 0.0};
    input_cvt_param.bias = {-0.485 / 0.229, -0.456 / 0.224, -0.406 / 0.225, 0.0};
    auto status = instance_->SetInputMat(input_mat, input_cvt_param);

    // step 2. Forward
    status = instance_->ForwardAsync(nullptr);

    // step 3. get output mat
    std::shared_ptr<TNN_NS::Mat> output_mat_scores = nullptr;
    status = instance_->GetOutputMat(output_mat_scores);

    if (status != TNN_NS::TNN_OK) {
        return nullptr;
    }

    // 返回预测结果
    auto *scores_data = (float *) output_mat_scores->GetData();

    jfloatArray result;
    result = env->NewFloatArray(output_mat_scores->GetChannel());
    env->SetFloatArrayRegion(result, 0, output_mat_scores->GetChannel(), scores_data);
    return result;
}

extern "C"
JNIEXPORT jint JNICALL
TNN_CLASSIFY(deinit)(JNIEnv *env, jobject thiz) {
    // TODO: implement deinit()
    net_ = nullptr;
    instance_ = nullptr;
    return 0;
}