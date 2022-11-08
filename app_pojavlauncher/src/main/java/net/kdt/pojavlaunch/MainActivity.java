package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.MCOptionUtils;

import java.io.IOException;

public class MainActivity extends BaseMainActivity {
    public static ControlLayout mControlLayout;
    private MCOptionUtils.MCOptionListener optionListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        MCXRLoader.setActivity(this);
        MCXRLoader.launch(this);
        super.onCreate(savedInstanceState);
        setFullScreen();
    }

    protected void setFullScreen() {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;


        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // Reload PREF_DEFAULTCTRL_PATH
            LauncherPreferences.loadPreferences(getApplicationContext());
            try {
                mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        //if(isInEditor) CustomControlsActivity.save(true,mControlLayout);
    }
}
