package net.kdt.pojavlaunch.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.widget.TextView;

public class UiUitls extends Activity {

    public static Handler UIHandler = new Handler(Looper.getMainLooper());

    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }
}