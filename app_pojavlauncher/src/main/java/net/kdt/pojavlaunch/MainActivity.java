package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Tools.DIR_GAME_NEW;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.MCOptionUtils;

import java.io.File;
import java.io.IOException;

public class MainActivity extends BaseMainActivity {
    public static ControlLayout mControlLayout;
    private MCOptionUtils.MCOptionListener optionListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        MCXRLoader.setActivity(this);

        mProfile = PojavProfile.getCurrentProfileContent(this);
        mVersionInfo = Tools.getVersionInfo(null,mProfile.selectedVersion);

        try {
            if (mVersionInfo.id.equals("fabric-loader-0.14.6-1.18.2")) {
                File[] files = new File[]{
                        new File(DIR_GAME_NEW + "mcxr-core-0.2.2+1.19.jar"),
                        new File(DIR_GAME_NEW + "mcxr-play-0.2.2+1.19.jar"),
                        new File(DIR_GAME_NEW + "titleworlds-0.1.0+1.19.jar"),
                        new File(DIR_GAME_NEW + "fabric-api-0.55.1+1.19.jar")
                };
                for(File file : files) {
                    if(file.exists()) {
                        file.delete();
                    }
                }
                Tools.copyAssetFile(this, "artifacts/mcxr-core-0.2.2+1.18.2.jar", DIR_GAME_NEW + "/mods", false);
                Tools.copyAssetFile(this, "artifacts/mcxr-play-0.2.2+1.18.2.jar", DIR_GAME_NEW + "/mods", false);
                Tools.copyAssetFile(this, "artifacts/titleworlds-0.1.0+1.18.2.jar", DIR_GAME_NEW + "/mods", false);
                Tools.copyAssetFile(this, "artifacts/lazydfu-0.1.3-SNAPSHOT.jar", DIR_GAME_NEW + "/mods", false);
                Tools.copyAssetFile(this, "artifacts/fabric-api-0.55.1+1.18.2.jar", DIR_GAME_NEW + "/mods", false);
            } else {
                File[] files = new File[]{
                        new File(DIR_GAME_NEW + "mcxr-core-0.2.2+1.18.2.jar"),
                        new File(DIR_GAME_NEW + "mcxr-play-0.2.2+1.18.2.jar"),
                        new File(DIR_GAME_NEW + "titleworlds-0.1.0+1.18.2.jar"),
                        new File(DIR_GAME_NEW + "fabric-api-0.55.1+1.18.2.jar")
                };
                for(File file : files) {
                    if(file.exists()) {
                        file.delete();
                    }
                }
                Tools.copyAssetFile(this, "artifacts/mcxr-core-0.2.2+1.19.jar", DIR_GAME_NEW + "/mods", false);
                Tools.copyAssetFile(this, "artifacts/mcxr-play-0.2.2+1.19.jar", DIR_GAME_NEW + "/mods", false);
                Tools.copyAssetFile(this, "artifacts/titleworlds-0.1.0+1.19.jar", DIR_GAME_NEW + "/mods", false);
                Tools.copyAssetFile(this, "artifacts/lazydfu-0.1.3-SNAPSHOT.jar", DIR_GAME_NEW + "/mods", false);
                Tools.copyAssetFile(this, "artifacts/fabric-api-0.55.1+1.19.jar", DIR_GAME_NEW + "/mods", false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        MCXRLoader.launch(this);
        super.onCreate(savedInstanceState);
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
