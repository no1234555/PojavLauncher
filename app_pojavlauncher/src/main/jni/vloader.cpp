//
// Created by Judge on 12/23/2021.
//
#include <thread>
#include <fstream>
#include <sstream>
#include <string>
#include <fcntl.h>
#include <unistd.h>
#include <EGL/egl.h>
#include <OpenOVR/openxr_platform.h>
#include <jni.h>
#include <dlfcn.h>
#include "GL/gl.h"
#include <GLES3/gl32.h>
#include "log.h"

static JavaVM* jvm;
XrInstanceCreateInfoAndroidKHR* OpenComposite_Android_Create_Info;
XrGraphicsBindingOpenGLESAndroidKHR* OpenComposite_Android_GLES_Binding_Info;

void* handle;
char *native_dir;

EGLBoolean (*eglExportVkImageANGLE_p) (EGLDisplay dpy, EGLImage image, void* vk_image, void* vk_image_create_info);
EGLDisplay (*eglGetCurrentDisplay_p) ();
std::string (*OpenComposite_Android_Load_Input_File)(const char *path);

static std::string load_file(const char *path);

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    if (jvm == nullptr) {
        jvm = vm;
    }
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_MCXRLoader_launch(JNIEnv *env, jclass clazz, jobject main) {
    main = (*env).NewGlobalRef(main);
    jclass clazz1 = (*env).GetObjectClass(main);
    jmethodID id = (*env).GetMethodID(clazz1, "runCraft", "()V");
    std::thread thread([=]() {
        JNIEnv* threadEnv;
        jvm->AttachCurrentThread(&threadEnv, nullptr);
        threadEnv->CallVoidMethod(main, id);
    });
    thread.detach();
}

extern "C"
JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_MCXRLoader_setAndroidInitInfo(JNIEnv *env, jclass clazz, jobject ctx) {
    OpenComposite_Android_Load_Input_File = load_file;

    env->GetJavaVM(&jvm);
    ctx = env->NewGlobalRef(ctx);
    OpenComposite_Android_Create_Info = new XrInstanceCreateInfoAndroidKHR{
            XR_TYPE_INSTANCE_CREATE_INFO_ANDROID_KHR,
            nullptr,
            jvm,
            ctx
    };

    PFN_xrInitializeLoaderKHR initializeLoader = nullptr;
    XrResult res;

    res = xrGetInstanceProcAddr(XR_NULL_HANDLE, "xrInitializeLoaderKHR",
                                (PFN_xrVoidFunction *) (&initializeLoader));

    if(!XR_SUCCEEDED(res)) {
        printf("Error!");
    }

    XrLoaderInitInfoAndroidKHR loaderInitInfoAndroidKhr = {
            XR_TYPE_LOADER_INIT_INFO_ANDROID_KHR,
            nullptr,
            jvm,
            ctx
    };

    initializeLoader((const XrLoaderInitInfoBaseHeaderKHR *) &loaderInitInfoAndroidKhr);
}

extern "C"
JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_MCXRLoader_setEGLGlobal(JNIEnv* env, jclass clazz, jlong ctx, jlong display, jlong cfg) {
    OpenComposite_Android_GLES_Binding_Info = new XrGraphicsBindingOpenGLESAndroidKHR {
            XR_TYPE_GRAPHICS_BINDING_OPENGL_ES_ANDROID_KHR,
            nullptr,
            (void*)display,
            (void*)cfg,
            (void*)ctx
    };
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_vivecraft_provider_VLoader_convertImgToEGLBuffer(JNIEnv* env, jclass clazz, jint image) {
    return reinterpret_cast<jlong>((EGLClientBuffer) (size_t) image);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_vivecraft_provider_VLoader_getNativeImage(JNIEnv* env, jclass clazz, jlong eglImage, jint width, jint height) {
    GLuint image;
    glGenTextures(1, &image);
    glBindTexture(GL_TEXTURE_2D, image);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, reinterpret_cast<GLeglImageOES>(eglImage));
    return image;
}

static std::string load_file(const char *path) {
    std::ifstream in(path, std::ios::in | std::ios::binary);
    if (in)
    {
        std::ostringstream contents;
        contents << in.rdbuf();
        in.close();
        return(contents.str());
    }
}
