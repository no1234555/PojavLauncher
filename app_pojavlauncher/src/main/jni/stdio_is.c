#include <jni.h>
#include <sys/types.h>
#include <stdbool.h>
#include <unistd.h>
#include <pthread.h>
#include <stdio.h>
#include <xhook.h>
#include <string.h>

//
// Created by maks on 17.02.21.
//
static JavaVM *_______jvm;
static volatile jmethodID _______method;
static volatile jobject _______obj;
static volatile jobject exitTrap_ctx;
static volatile jclass exitTrap_exitClass;
static volatile jmethodID exitTrap_staticMethod;
static JavaVM *exitTrap_jvm;
static int pfd[2];
static pthread_t logger;

int chr_isvalid(uint32_t c)
{
    if (c <= 0x7F) return 1;
    if (0xC080 == c) return 1;   // Accept 0xC080 as representation for '\0'
    if (0xC280 <= c && c <= 0xDFBF) return ((c & 0xE0C0) == 0xC080);
    if (0xEDA080 <= c && c <= 0xEDBFBF) return 0; // Reject UTF-16 surrogates
    if (0xE0A080 <= c && c <= 0xEFBFBF) return ((c & 0xF0C0C0) == 0xE08080);
    if (0xF0908080 <= c && c <= 0xF48FBFBF) return ((c & 0xF8C0C0C0) == 0xF0808080);
    return 0;
}

static void *logger_thread() {
    JNIEnv *env;
    jstring str;
    (*_______jvm)->AttachCurrentThread(_______jvm,&env,NULL);
    ssize_t  rsize;
    char buf[2048];
    while((rsize = read(pfd[0], buf, sizeof(buf)-1)) > 0) {
        if(buf[rsize-1]=='\n') {
            rsize=rsize-1;
        }
        buf[rsize]=0x00;

        bool isValid;
        for(int i = 0; i < 2048; i++) {
            if(strcmp(&buf[i], "\0") == 0) {

            }
            isValid = chr_isvalid(buf[i]);
        }

        if(isValid) {
            str = (*env)->NewStringUTF(env, buf);
            (*env)->CallVoidMethod(env, _______obj, _______method, str);
        }
        (*env)->DeleteLocalRef(env,str);
    }
    (*env)->DeleteGlobalRef(env,_______method);
    (*env)->DeleteGlobalRef(env,_______obj);
    (*_______jvm)->DetachCurrentThread(_______jvm);
}
JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_utils_JREUtils_logToLogger(JNIEnv *env, jclass clazz, jobject javaLogger) {
    // TODO: implement logToActivity()
    jclass loggableActivityClass = (*env)->FindClass(env,"net/kdt/pojavlaunch/Logger");
    _______method = (*env)->GetMethodID(env,loggableActivityClass,"appendToLog", "(Ljava/lang/String;)V");
    (*env)->GetJavaVM(env,&_______jvm);
    _______obj = (*env)->NewGlobalRef(env, javaLogger);

    setvbuf(stdout, 0, _IOLBF, 0); // make stdout line-buffered
    setvbuf(stderr, 0, _IONBF, 0); // make stderr unbuffered

    /* create the pipe and redirect stdout and stderr */
    pipe(pfd);
    dup2(pfd[1], 1);
    dup2(pfd[1], 2);

    /* spawn the logging thread */
    if(pthread_create(&logger, 0, logger_thread, 0) == -1) {
        jstring str = (*env)->NewStringUTF(env,"Failed to start logging!");
        (*env)->CallVoidMethod(env,_______obj,_______method,str);
        (*env)->DeleteLocalRef(env,str);
        (*env)->DeleteGlobalRef(env,_______method);
        (*env)->DeleteGlobalRef(env,_______obj);
    }
    pthread_detach(logger);

}
void (*old_exit)(int code);
void custom_exit(int code) {
    if(code != 0) {
        JNIEnv *env;
        (*exitTrap_jvm)->AttachCurrentThread(exitTrap_jvm, &env, NULL);
        (*env)->CallStaticVoidMethod(env, exitTrap_exitClass, exitTrap_staticMethod, exitTrap_ctx,
                                     code);
        (*env)->DeleteGlobalRef(env, exitTrap_ctx);
        (*env)->DeleteGlobalRef(env, exitTrap_exitClass);
        (*exitTrap_jvm)->DetachCurrentThread(exitTrap_jvm);
    }
    old_exit(code);
}
JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_utils_JREUtils_setupExitTrap(JNIEnv *env, jclass clazz, jobject context) {
    exitTrap_ctx = (*env)->NewGlobalRef(env,context);
    (*env)->GetJavaVM(env,&exitTrap_jvm);
    exitTrap_exitClass = (*env)->NewGlobalRef(env,(*env)->FindClass(env,"net/kdt/pojavlaunch/ExitActivity"));
    exitTrap_staticMethod = (*env)->GetStaticMethodID(env,exitTrap_exitClass,"showExitMessage","(Landroid/content/Context;I)V");
    xhook_enable_debug(1);
    xhook_register(".*\\.so$","exit",custom_exit,&old_exit);
    xhook_refresh(1);
}
