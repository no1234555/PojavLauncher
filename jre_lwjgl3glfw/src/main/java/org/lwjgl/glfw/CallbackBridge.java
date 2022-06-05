package org.lwjgl.glfw;

public class CallbackBridge {
    public static native long getEGLContextPtr();
    public static native long getEGLDisplayPtr();
    public static native long getEGLConfigPtr();
    static {
        System.loadLibrary("pojavexec");
    }
}
