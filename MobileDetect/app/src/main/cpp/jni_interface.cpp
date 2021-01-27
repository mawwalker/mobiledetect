#include <jni.h>
#include <string>
#include <gpu.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include "native.h"
#include "YoloV4.h"
#include "NanoDet.h"


JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    ncnn::create_gpu_instance();
    if (ncnn::get_gpu_count() > 0) {
        // YoloV5::hasGPU = true;
        YoloV4::hasGPU = true;
        NanoDet::hasGPU = true;
    }
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    ncnn::destroy_gpu_instance();
}


/*********************************************************************************************
                                         NanoDet
 ********************************************************************************************/

extern "C" JNIEXPORT void JNICALL
Java_com_dsm_mobiledetect_NanoDet_init(JNIEnv *env, jobject thiz, jobject assetManager, jboolean useGPU) {
    if (NanoDet::detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        NanoDet::detector = new NanoDet(mgr, "nanodet_m.param", "nanodet_m.bin", useGPU);
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_dsm_mobiledetect_NanoDet_detect(JNIEnv *env, jobject thiz, jobject image, jdouble threshold, jdouble nms_threshold) {
    auto result = NanoDet::detector->detect(env, image, threshold, nms_threshold);

    auto box_cls = env->FindClass("com/dsm/mobiledetect/Box");
    auto cid = env->GetMethodID(box_cls, "<init>", "(FFFFIF)V");
    jobjectArray ret = env->NewObjectArray(result.size(), box_cls, nullptr);
    int i = 0;
    for (auto &box:result) {
        env->PushLocalFrame(1);
        jobject obj = env->NewObject(box_cls, cid, box.x1, box.y1, box.x2, box.y2, box.label, box.score);
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }
    return ret;
}

/*********************************************************************************************
                                         YOLOv4-tiny
 ********************************************************************************************/
extern "C" JNIEXPORT void JNICALL
Java_com_dsm_mobiledetect_YOLOv4_init(JNIEnv *env, jobject thiz, jobject assetManager, jboolean useGPU) {
    if (YoloV4::detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        YoloV4::detector = new YoloV4(mgr, "yolov4-tiny-opt.param", "yolov4-tiny-opt.bin", useGPU);
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_dsm_mobiledetect_YOLOv4_detect(JNIEnv *env, jobject thiz, jobject image, jdouble threshold, jdouble nms_threshold) {
    auto result = YoloV4::detector->detect(env, image, threshold, nms_threshold);

    auto box_cls = env->FindClass("com/dsm/mobiledetect/Box");
    auto cid = env->GetMethodID(box_cls, "<init>", "(FFFFIF)V");
    jobjectArray ret = env->NewObjectArray(result.size(), box_cls, nullptr);
    int i = 0;
    for (auto &box:result) {
        env->PushLocalFrame(1);
        jobject obj = env->NewObject(box_cls, cid, box.x1, box.y1, box.x2, box.y2, box.label, box.score);
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }
    return ret;
}
