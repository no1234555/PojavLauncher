package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Architecture.archAsString;
import static net.kdt.pojavlaunch.Tools.DIR_GAME_NEW;
import static net.kdt.pojavlaunch.Tools.getFileName;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.kdt.pojavlaunch.authenticator.microsoft.MicrosoftAuthTask;
import net.kdt.pojavlaunch.authenticator.microsoft.ui.MicrosoftLoginGUIActivity;
import net.kdt.pojavlaunch.authenticator.mojang.InvalidateTokenTask;
import net.kdt.pojavlaunch.authenticator.mojang.RefreshListener;
import net.kdt.pojavlaunch.customcontrols.CustomControls;
import net.kdt.pojavlaunch.modmanager.ModManager;
import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.tasks.RefreshVersionListTask;
import net.kdt.pojavlaunch.utils.LocaleUtils;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import org.apache.commons.io.FileUtils;
import top.defaults.checkerboarddrawable.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;

public class PojavLoginActivity extends BaseActivity {
    private final Object mLockStoragePerm = new Object();
    private final Object mLockSelectJRE = new Object();

    private EditText edit2;
    private final int REQUEST_STORAGE_REQUEST_CODE = 1;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 2;
    private final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 3;
    private CheckBox sRemember;
    private TextView startupTextView;
    private SharedPreferences firstLaunchPrefs;
    private MinecraftAccount mProfile = null;

    private boolean isSkipInit = false;
    private boolean isStarting = false;

    public static final String PREF_IS_INSTALLED_JAVARUNTIME = "isJavaRuntimeInstalled";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); // false;
        if(savedInstanceState != null) {
            isStarting = savedInstanceState.getBoolean("isStarting");
            isSkipInit = savedInstanceState.getBoolean("isSkipInit");
        }
        Tools.updateWindowSize(this);
        firstLaunchPrefs = getSharedPreferences("pojav_extract", MODE_PRIVATE);
        new Thread(new InitRunnable()).start();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isStarting",isStarting);
        outState.putBoolean("isSkipInit",isSkipInit);
    }

    public class InitRunnable implements Runnable{
        private int revokeCount = -1;
        private int proceedState = 0;
        private ProgressBar progress;
        public InitRunnable() {
        }
        public void initLocalUi() {
            LinearLayout startScr = new LinearLayout(PojavLoginActivity.this);
            LayoutInflater.from(PojavLoginActivity.this).inflate(R.layout.start_screen,startScr);
            PojavLoginActivity.this.setContentView(startScr);

            progress = (ProgressBar) findViewById(R.id.startscreenProgress);
            if(isStarting) progress.setVisibility(View.VISIBLE);
        }

        public int _start() {
            Log.i("UITest","START initialization");
            if(!isStarting) {
                //try { Thread.sleep(2000); } catch (InterruptedException e) { }
                runOnUiThread(() -> progress.setVisibility(View.VISIBLE));
                while (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29 && !isStorageAllowed()) { //Do not ask for storage at all on Android 10+
                    try {
                        revokeCount++;
                        if (revokeCount >= 3) {
                            Toast.makeText(PojavLoginActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                            return 2;
                        }
                        requestPermissions();

                        synchronized (mLockStoragePerm) {
                            mLockStoragePerm.wait();
                        }
                    } catch (InterruptedException e) {
                    }
                }
                isStarting = true;
            }
            try {
                initMain();
            } catch (Throwable th) {
                Tools.showError(PojavLoginActivity.this, th, true);
                return 1;
            }
            return 0;
        }
        public void proceed() {
            isStarting = false;
            switch(proceedState) {
                case 2:
                    finish();
                    break;
                case 0:
                    uiInit();
                    break;
            }
        }
        @Override
        public void run() {
            if(!isSkipInit) {
                PojavLoginActivity.this.runOnUiThread(this::initLocalUi);
                proceedState = _start();
            }
            PojavLoginActivity.this.runOnUiThread(this::proceed);
        }
    }
    private void uiInit() {
        setContentView(R.layout.activity_pojav_login);

        Spinner spinnerChgLang = findViewById(R.id.login_spinner_language);

        String defaultLang = LocaleUtils.DEFAULT_LOCALE.getDisplayName();
        SpannableString defaultLangChar = new SpannableString(defaultLang);
        defaultLangChar.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, defaultLang.length(), 0);

        final ArrayAdapter<DisplayableLocale> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        langAdapter.add(new DisplayableLocale(LocaleUtils.DEFAULT_LOCALE, defaultLangChar));
        langAdapter.add(new DisplayableLocale(Locale.ENGLISH));

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("language_list.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                File currFile = new File("/" + line);
                // System.out.println(currFile.getAbsolutePath());
                if (currFile.getAbsolutePath().contains("/values-") || currFile.getName().startsWith("values-")) {
                    // TODO use regex(?)
                    langAdapter.add(new DisplayableLocale(currFile.getName().replace("values-", "").replace("-r", "-")));
                }
            }
        } catch (IOException e) {
            Tools.showError(this, e);
        }

        langAdapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);

        int selectedLang = 0;
        for (int i = 0; i < langAdapter.getCount(); i++) {
            if (Locale.getDefault().getDisplayLanguage().equals(langAdapter.getItem(i).mLocale.getDisplayLanguage())) {
                selectedLang = i;
                break;
            }
        }

        spinnerChgLang.setAdapter(langAdapter);
        spinnerChgLang.setSelection(selectedLang);
        spinnerChgLang.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            private boolean isInitCalled;
            @Override
            public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
                if (!isInitCalled) {
                    isInitCalled = true;
                    return;
                }

                Locale locale;
                if (position == 0) {
                    locale = LocaleUtils.DEFAULT_LOCALE;
                } else if (position == 1) {
                    locale = Locale.ENGLISH;
                } else {
                    locale = langAdapter.getItem(position).mLocale;
                }

                LauncherPreferences.PREF_LANGUAGE = locale.getLanguage();
                LauncherPreferences.DEFAULT_PREF.edit().putString("language", LauncherPreferences.PREF_LANGUAGE).apply();

                // Restart to apply language change
                finish();
                startActivity(getIntent());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {}
        });

        sRemember = findViewById(R.id.login_switch_remember);
        isSkipInit = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        Tools.updateWindowSize(this);

        // Clear current profile
        PojavProfile.setCurrentProfile(this, null);
    }


    private void unpackComponent(AssetManager am, String component) throws IOException {
        File versionFile = new File(Tools.DIR_GAME_HOME + "/" + component + "/version");
        InputStream is = am.open("components/" + component + "/version");
        if(!versionFile.exists()) {
            if (versionFile.getParentFile().exists() && versionFile.getParentFile().isDirectory()) {
                FileUtils.deleteDirectory(versionFile.getParentFile());
            }
            versionFile.getParentFile().mkdir();

            Log.i("UnpackPrep", component + ": Pack was installed manually, or does not exist, unpacking new...");
            String[] fileList = am.list("components/" + component);
            for(String s : fileList) {
                Tools.copyAssetFile(this, "components/" + component + "/" + s, Tools.DIR_GAME_HOME + "/" + component, true);
            }
        } else {
            FileInputStream fis = new FileInputStream(versionFile);
            String release1 = Tools.read(is);
            String release2 = Tools.read(fis);
            if (!release1.equals(release2)) {
                if (versionFile.getParentFile().exists() && versionFile.getParentFile().isDirectory()) {
                    FileUtils.deleteDirectory(versionFile.getParentFile());
                }
                versionFile.getParentFile().mkdir();

                String[] fileList = am.list("components/" + component);
                for (String s : fileList) {
                    Tools.copyAssetFile(this, "components/" + component + "/" + s, Tools.DIR_GAME_HOME + "/" + component, true);
                }
            } else {
                Log.i("UnpackPrep", component + ": Pack is up-to-date with the launcher, continuing...");
            }
        }
    }
    public static void disableSplash(String dir) {
        mkdirs(dir + "/config");
        File forgeSplashFile = new File(dir, "config/splash.properties");
        String forgeSplashContent = "enabled=true";
        try {
            if (forgeSplashFile.exists()) {
                forgeSplashContent = Tools.read(forgeSplashFile.getAbsolutePath());
            }
            if (forgeSplashContent.contains("enabled=true")) {
                Tools.write(forgeSplashFile.getAbsolutePath(),
                        forgeSplashContent.replace("enabled=true", "enabled=false"));
            }
        } catch (IOException e) {
            Log.w(Tools.APP_NAME, "Could not disable Forge 1.12.2 and below splash screen!", e);
        }
    }
    private void initMain() throws Throwable {
        mkdirs(Tools.DIR_ACCOUNT_NEW);
        PojavMigrator.migrateAccountData(this);

        mkdirs(Tools.DIR_GAME_HOME);
        mkdirs(ModManager.getWorkDir());
        mkdirs(Tools.DIR_GAME_HOME + "/lwjgl3");
        mkdirs(Tools.DIR_GAME_HOME + "/config");
        if (!PojavMigrator.migrateGameDir()) {
            mkdirs(Tools.DIR_GAME_NEW);
            mkdirs(DIR_GAME_NEW + "/resourcepacks");

            mkdirs(Tools.DIR_HOME_VERSION);
            mkdirs(Tools.DIR_HOME_LIBRARY);
        }

        mkdirs(Tools.CTRLMAP_PATH);

        try {
            new CustomControls(this).save(Tools.CTRLDEF_FILE);
            Tools.copyAssetFile(this, "components/security/pro-grade.jar", Tools.DIR_DATA, true);
            Tools.copyAssetFile(this, "components/security/java_sandbox.policy", Tools.DIR_DATA, true);
            Tools.copyAssetFile(this, "options.txt", Tools.DIR_GAME_NEW, false);

            // TODO: Remove after implement.
            Tools.copyAssetFile(this, "launcher_profiles.json", Tools.DIR_GAME_NEW, false);
            Tools.copyAssetFile(this,"resolv.conf",Tools.DIR_DATA, true);
            Tools.copyAssetFile(this,"arc_dns_injector.jar",Tools.DIR_DATA, true);
            Tools.copyAssetFile(this,"servers.dat",DIR_GAME_NEW, false);
            Tools.copyAssetFile(this,"sodium-extra.properties",DIR_GAME_NEW + "/config", false);
            Tools.copyAssetFile(this,"sodium-mixins.properties",DIR_GAME_NEW + "/config", false);
            Tools.copyAssetFile(this,"vivecraft-config.properties",DIR_GAME_NEW + "/config", false);
            
            // Install Mod Manager Jsons
            Tools.copyAssetFile(this, "jsons/modrinth-compat.json", ModManager.getWorkDir(), false);
            Tools.copyAssetFile(this, "jsons/curseforge-compat.json", ModManager.getWorkDir(), false);
            Tools.copyAssetFile(this, "jsons/modmanager.json", ModManager.getWorkDir(), false);

            // Install TitleWorlds
            copyAssetFolder(this, "TitleWorld", DIR_GAME_NEW + "/titleworlds/TitleWorld");

            AssetManager am = this.getAssets();

            unpackComponent(am, "caciocavallo");
            unpackComponent(am, "lwjgl3");
            if(!installRuntimeAutomatically(am,MultiRTUtils.getRuntimes().size() > 0)) {
                MultiRTConfigDialog.openRuntimeSelector(this, MultiRTConfigDialog.MULTIRT_PICK_RUNTIME_STARTUP);
                synchronized (mLockSelectJRE) {
                    mLockSelectJRE.wait();
                }
            }
            if(Build.VERSION.SDK_INT > 28) runOnUiThread(this::showStorageDialog);
            LauncherPreferences.loadPreferences(getApplicationContext());
        }
        catch(Throwable e){
            Tools.showError(this, e);
        }

        ModManager.init();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Looper.prepare();
                Toast.makeText(this, "This permission is for voice chat.", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Looper.prepare();
                Toast.makeText(this, "This permission is for backup and log storage.", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            }
        }
    }


    public static boolean copyAssetFolder(Context context, String srcName, String dstName) {
        try {
            boolean result;
            String fileList[] = context.getAssets().list(srcName);
            if (fileList == null) return false;

            if (fileList.length == 0) {
                result = copyAssetFile(context, srcName, dstName);
            } else {
                File file = new File(dstName);
                result = file.mkdirs();
                for (String filename : fileList) {
                    result &= copyAssetFolder(context, srcName + File.separator + filename, dstName + File.separator + filename);
                }
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copyAssetFile(Context context, String srcName, String dstName) {
        try {
            InputStream in = context.getAssets().open(srcName);
            File outFile = new File(dstName);
            OutputStream out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showStorageDialog() {
        if(!firstLaunchPrefs.getBoolean("storageDialogShown",false)) {
            AlertDialog.Builder bldr = new AlertDialog.Builder(this);
            bldr.setTitle(R.string.storage_warning_title);
            Spanned sp = Html.fromHtml(getString(R.string.storage_warning_text, BuildConfig.APPLICATION_ID));
            bldr.setMessage(sp);
            bldr.setCancelable(false);
            bldr.setPositiveButton(android.R.string.ok, (dialog, which)->{
                firstLaunchPrefs.edit().putBoolean("storageDialogShown",true).apply();
                dialog.dismiss();
            });
            bldr.show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            if (requestCode == MultiRTConfigDialog.MULTIRT_PICK_RUNTIME_STARTUP) {
                if (data != null) {
                    final Uri uri = data.getData();
                    Thread t = new Thread(() -> {
                        try {
                            MultiRTUtils.installRuntimeNamed(getContentResolver().openInputStream(uri), getFileName(this, uri),
                                    (resid, stuff) -> PojavLoginActivity.this.runOnUiThread(
                                            () -> {
                                                if (startupTextView != null)
                                                    startupTextView.setText(PojavLoginActivity.this.getString(resid, stuff));
                                            }));
                            synchronized (mLockSelectJRE) {
                                mLockSelectJRE.notifyAll();
                            }
                        } catch (IOException e) {
                            Tools.showError(PojavLoginActivity.this
                                    , e);
                        }
                    });
                    t.start();
                }
            }else if(requestCode == MicrosoftLoginGUIActivity.AUTHENTICATE_MICROSOFT_REQUEST) {
                //Log.i("MicroLoginWrap","Got microsoft login result:" + data);
                performMicroLogin(data);
            }
        }
    }
    private boolean installRuntimeAutomatically(AssetManager am, boolean otherRuntimesAvailable) {
        /* Check if JRE is included */
        String rt_version = null;
        String current_rt_version = MultiRTUtils.__internal__readBinpackVersion("Internal");
        try {
            rt_version = Tools.read(am.open("components/jre/version"));
        } catch (IOException e) {
            Log.e("JREAuto", "JRE was not included on this APK.", e);
        }
        if(current_rt_version == null && otherRuntimesAvailable) return true; //Assume user maintains his own runtime
        if(rt_version == null) return false;
        if(!rt_version.equals(current_rt_version)) { //If we already have an integrated one installed, check if it's up-to-date
            try {
                MultiRTUtils.installRuntimeNamedBinpack(am.open("components/jre/universal.tar.xz"), am.open("components/jre/bin-" + archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz"), "Internal", rt_version,
                        (resid, vararg) -> runOnUiThread(()->{if(startupTextView!=null)startupTextView.setText(getString(resid,vararg));}));
                MultiRTUtils.postPrepare(PojavLoginActivity.this,"Internal");
                return true;
            }catch (IOException e) {
                Log.e("JREAuto", "Internal JRE unpack failed", e);
                return false;
            }
        }else return true; // we have at least one runtime, and it's compartible, good to go
    }

    private static boolean mkdirs(String path)
    {
        File file = new File(path);
        // check necessary???
        if(file.getParentFile().exists())
            return file.mkdir();
        else return file.mkdirs();
    }


    public void loginMicrosoft(View view) {
        Intent i = new Intent(this,MicrosoftLoginGUIActivity.class);
        startActivityForResult(i,MicrosoftLoginGUIActivity.AUTHENTICATE_MICROSOFT_REQUEST);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
    public void performMicroLogin(Intent intent) {
        Uri data = intent.getData();
        //Log.i("MicroAuth", data.toString());
        if (data != null && data.getScheme().equals("ms-xal-00000000402b5328") && data.getHost().equals("auth")) {
            String error = data.getQueryParameter("error");
            String error_description = data.getQueryParameter("error_description");
            if (error != null) {
                // "The user has denied access to the scope requested by the client application": user pressed Cancel button, skip it
                if (!error_description.startsWith("The user has denied access to the scope requested by the client application")) {
                    Toast.makeText(this, "Error: " + error + ": " + error_description, Toast.LENGTH_LONG).show();
                }
            } else {
                String code = data.getQueryParameter("code");
                new MicrosoftAuthTask(this, new RefreshListener(){
                    @Override
                    public void onFailed(Throwable e) {
                        Tools.showError(PojavLoginActivity.this, e);
                    }

                    @Override
                    public void onSuccess(MinecraftAccount b) {
                        mProfile = b;
                        playProfile(false);
                    }
                }).execute("false", code);
                // Toast.makeText(this, "Logged in to Microsoft account, but NYI", Toast.LENGTH_LONG).show();
            }
        }
    }
    private View getViewFromList(int pos, ListView listView) {
        final int firstItemPos = listView.getFirstVisiblePosition();
        final int lastItemPos = firstItemPos + listView.getChildCount() - 1;

        if (pos < firstItemPos || pos > lastItemPos ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstItemPos;
            return listView.getChildAt(childIndex);
        }
    }

    public void loginSavedAcc(View view) {
        String[] accountArr = new File(Tools.DIR_ACCOUNT_NEW).list();
        if(accountArr.length == 0){
            showNoAccountDialog();
            return;
        }

        final Dialog accountDialog = new Dialog(PojavLoginActivity.this);

        accountDialog.setContentView(R.layout.dialog_select_account);

        LinearLayout accountListLayout = accountDialog.findViewById(R.id.accountListLayout);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);

        for (int accountIndex = 0; accountIndex < accountArr.length; accountIndex++) {
            String s = accountArr[accountIndex];
            View child = inflater.inflate(R.layout.item_minecraft_account, accountListLayout,false);
            TextView accountName = child.findViewById(R.id.accountitem_text_name);
            ImageButton removeButton = child.findViewById(R.id.accountitem_button_remove);
            ImageView imageView = child.findViewById(R.id.account_head);

            String accNameStr = s.substring(0, s.length() - 5);
            imageView.setImageBitmap(MinecraftAccount.load(accNameStr).getSkinFace());

            accountName.setText(accNameStr);

            accountListLayout.addView(child);

            accountName.setOnClickListener(new View.OnClickListener() {
                final String selectedAccName = accountName.getText().toString();
                @Override
                public void onClick(View v) {
                    try {
                        RefreshListener authListener = new RefreshListener(){
                            @Override
                            public void onFailed(Throwable e) {
                                Tools.showError(PojavLoginActivity.this, e);
                            }

                            @Override
                            public void onSuccess(MinecraftAccount out) {
                                accountDialog.dismiss();
                                mProfile = out;
                                playProfile(true);
                            }
                        };

                        MinecraftAccount acc = MinecraftAccount.load(selectedAccName);
                        if (acc.accessToken.length() >= 5){
                            new MicrosoftAuthTask(PojavLoginActivity.this, authListener)
                                    .execute("true", acc.msaRefreshToken);
                        } else {
                            accountDialog.dismiss();
                            PojavProfile.launch(PojavLoginActivity.this, selectedAccName);
                        }
                    } catch (Exception e) {
                        Tools.showError(PojavLoginActivity.this, e);
                    }
                }
            });

            final int accountIndex_final = accountIndex;
            removeButton.setOnClickListener(new View.OnClickListener() {
                final String selectedAccName = accountName.getText().toString();
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(PojavLoginActivity.this);
                    builder2.setTitle(selectedAccName);
                    builder2.setMessage(R.string.warning_remove_account);
                    builder2.setPositiveButton(android.R.string.ok, (p1, p2) -> {
                        new InvalidateTokenTask(PojavLoginActivity.this).execute(selectedAccName);
                        accountListLayout.removeViewsInLayout(accountIndex_final, 1);

                        if (accountListLayout.getChildCount() == 0) {
                            accountDialog.dismiss(); //No need to keep it, since there is no account
                            return;
                        }
                        //Refreshes the layout with the same settings so it take the missing child into account.
                        accountListLayout.setLayoutParams(accountListLayout.getLayoutParams());

                    });
                    builder2.setNegativeButton(android.R.string.cancel, null);
                    builder2.show();
                }
            });

        }
        accountDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        accountDialog.show();
    }

    private MinecraftAccount loginLocal() {
        new File(Tools.DIR_ACCOUNT_OLD).mkdir();

        String text = edit2.getText().toString();
        if (text.isEmpty()) {
            edit2.setError(getString(R.string.global_error_field_empty));
        } else if (text.length() < 3 || text.length() > 16 || !text.matches("\\w+")) {
            edit2.setError(getString(R.string.login_error_invalid_username));
        } else if (new File(Tools.DIR_ACCOUNT_NEW + "/" + text + ".json").exists()) {
            edit2.setError(getString(R.string.login_error_exist_username));
        } else {
            MinecraftAccount builder = new MinecraftAccount();
            builder.isMicrosoft = false;
            builder.username = text;

            return builder;
        }
        return null;
    }


    public void loginMC(final View v)
    {
        mProfile = loginLocal();
        playProfile(false);
    }

    private void playProfile(boolean notOnLogin) {
        if (mProfile != null) {
            try {
                String profileName = null;
                if (sRemember.isChecked() || notOnLogin) {
                    profileName = mProfile.save();
                }

                PojavProfile.launch(PojavLoginActivity.this, profileName == null ? mProfile : profileName);
            } catch (IOException e) {
                Tools.showError(this, e);
            }
        }
    }

    public static String strArrToString(String[] strArr)
    {
        String[] strArrEdit = strArr.clone();
        strArrEdit[0] = "";

        String str = Arrays.toString(strArrEdit);
        str = str.substring(1, str.length() - 1).replace(",", "\n");

        return str;
    }
    //We are calling this method to check the permission status
    private boolean isStorageAllowed() {
        //Getting the permission status
        int result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);


        //If permission is granted returning true
        return result1 == PackageManager.PERMISSION_GRANTED &&
                result2 == PackageManager.PERMISSION_GRANTED;
    }

    //Requesting permission
    private void requestPermissions()
    {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_REQUEST_CODE);
    }

    // This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_REQUEST_CODE) {
            synchronized (mLockStoragePerm) {
                mLockStoragePerm.notifyAll();
            }
        }
    }

    //When the user have no saved account, you can show him this dialog
    private void showNoAccountDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(PojavLoginActivity.this);

        builder.setMessage(R.string.login_dialog_no_saved_account)
                .setTitle(R.string.login_title_no_saved_account)
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
