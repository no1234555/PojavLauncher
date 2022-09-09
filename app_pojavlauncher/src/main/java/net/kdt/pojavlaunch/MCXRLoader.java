package net.kdt.pojavlaunch;

import android.content.Context;

public class MCXRLoader {
    public static native void setAndroidInitInfo(Context ctx);

    static {
        System.loadLibrary("openvr_api");
    }

    public static native void launch(MainActivity activity);
    public static native void setEGLGlobal(long ctx, long display, long cfg);
}
