#ifdef __cplusplus
extern "C" {
void setEGLGlobal(void* display, void* config, void* context);
}
#else
void setEGLGlobal(void* display, void* config, void* context);
#endif