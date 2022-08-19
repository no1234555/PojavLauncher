package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Tools.DIR_GAME_NEW;
import static net.kdt.pojavlaunch.Tools.getFileName;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Environment;
import android.view.*;
import android.webkit.MimeTypeMap;
import android.widget.*;

import androidx.annotation.Nullable;

import java.io.*;
import java.util.Objects;

import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.prefs.*;
import net.kdt.pojavlaunch.tasks.*;

import androidx.appcompat.app.AlertDialog;
import net.kdt.pojavlaunch.value.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public abstract class BaseLauncherActivity extends BaseActivity {
	public Button mPlayButton;
    public ProgressBar mLaunchProgress;
	public Spinner mVersionSelector;
	public MultiRTConfigDialog mRuntimeConfigDialog;
	public TextView mLaunchTextStatus;
    
    public JMinecraftVersionList mVersionList = new JMinecraftVersionList();
	public MinecraftDownloaderTask mTask;
	public MinecraftAccount mProfile;
	public String[] mAvailableVersions;
    
	public boolean mIsAssetsProcessing = false;
    protected boolean canBack = false;
    
    public abstract void statusIsLaunching(boolean isLaunching);


    /**
     * Used by the reset options button from the layout_main_v4
     * @param view The view triggering the function
     */
    public void resetOptionsTXT(View view){
        try {
            Tools.copyAssetFile(view.getContext(), "options.txt", DIR_GAME_NEW, true);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Used by the install button from the layout_main_v4
     * @param view The view triggering the function
     */
    public void backup(View view) {
        try {
            String storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+ "/QuestcraftBackup";
            mPlayButton.setEnabled(false);
            File backupFolder = new File(storageDir);
            File savesFolder = new File(DIR_GAME_NEW+"/saves");
            File modsFolder = new File(DIR_GAME_NEW+"/mods");
            File configFolder = new File(DIR_GAME_NEW+"/config");
            File optionsTxt = new File(DIR_GAME_NEW+"/options.txt");

            if (backupFolder.exists()) {
                FileUtils.deleteDirectory(backupFolder);
            }
            backupFolder.mkdirs();

            if (savesFolder.exists()) {
                File f = new File(storageDir + "/saves");
                if (f.exists()) f.delete();
                f.mkdir();
                for (File file : savesFolder.listFiles()) {
                    FileUtils.copyDirectoryToDirectory(file, f);
                }
            }
            if (modsFolder.exists()) {
                File f = new File(storageDir + "/mods");
                if (f.exists()) f.delete();
                for (File file : Objects.requireNonNull(modsFolder.listFiles())) {
                    if (!file.isDirectory()) { // failsafe, should always be true
                        if (!file.getName().toLowerCase().contains("mcxr") && !file.getName().toLowerCase().contains("titleworlds")) {
                            FileUtils.copyFileToDirectory(file, backupFolder);
                        }
                    }
                }
            }
            if (configFolder.exists()) {
                File f = new File(storageDir + "/config");
                if (f.exists()) f.delete();
                f.mkdir();
                for (File file : configFolder.listFiles()) {
                    FileUtils.copyDirectoryToDirectory(file, f);
                }
            }
            if (optionsTxt.exists()) {
                File f = new File(storageDir + "/config.txt");
                if (f.exists()) f.delete();
                FileUtils.copyFileToDirectory(optionsTxt, backupFolder);
            }

            Toast.makeText(this, "Successfully completed backup!", Toast.LENGTH_LONG).show();
            mPlayButton.setEnabled(true);
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void restore(View view) {
        try {
            mPlayButton.setEnabled(false);
            String backupFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+ "/QuestcraftBackup";
            File backupFolder = new File(backupFolderPath);
            File savesFolder = new File(backupFolderPath + "/saves");
            File modsFolder = new File(backupFolderPath + "/mods");
            File configFolder = new File(backupFolderPath + "/config");
            File optionsTxt = new File(backupFolderPath + "/options.txt");
            File dirGameNew = new File(DIR_GAME_NEW);

            if (!backupFolder.exists()) {
                Toast.makeText(this, "A backup has not been previously made!", Toast.LENGTH_LONG).show();
                return;
            }

            if (savesFolder.exists()) {
                File f = new File(DIR_GAME_NEW + "/saves");
                if (f.exists()) f.delete();
                FileUtils.copyDirectoryToDirectory(savesFolder, dirGameNew);
            }
            if (modsFolder.exists()) {
                for (File file : Objects.requireNonNull(modsFolder.listFiles())) {
                    if (!file.isDirectory()) { // failsafe, should always be true
                        if (!file.getName().toLowerCase().contains("mcxr") && !file.getName().toLowerCase().contains("titleworlds")) {
                            FileUtils.copyFileToDirectory(file, backupFolder);
                        }
                    }
                }
            }
            if (configFolder.exists()) {
                File f = new File(DIR_GAME_NEW + "/config");
                if (f.exists()) f.delete();
                FileUtils.copyDirectoryToDirectory(configFolder, dirGameNew);
            }
            if (optionsTxt.exists()) {
                File f = new File(DIR_GAME_NEW + "/options.txt");
                if (f.exists()) f.delete();
                FileUtils.copyFileToDirectory(optionsTxt, dirGameNew);
            }

            Toast.makeText(this, "Successfully completed restore!", Toast.LENGTH_LONG).show();
            mPlayButton.setEnabled(true);
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    public static final int RUN_MOD_INSTALLER = 2050;
    private void installMod(boolean customJavaArgs) {
        if (MultiRTUtils.getExactJreName(8) == null) {
            Toast.makeText(this, R.string.multirt_nojava8rt, Toast.LENGTH_LONG).show();
            return;
        }
        if (customJavaArgs) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.alerttitle_installmod);
            builder.setNegativeButton(android.R.string.cancel, null);
            final AlertDialog dialog;
            final EditText edit = new EditText(this);
            edit.setSingleLine();
            edit.setHint("-jar/-cp /path/to/file.jar ...");
            builder.setPositiveButton(android.R.string.ok, (di, i) -> {
                Intent intent = new Intent(BaseLauncherActivity.this, JavaGUILauncherActivity.class);
                intent.putExtra("skipDetectMod", true);
                intent.putExtra("javaArgs", edit.getText().toString());
                startActivity(intent);
            });
            dialog = builder.create();
            dialog.setView(edit);
            dialog.show();
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("jar");
            if(mimeType == null) mimeType = "*/*";
            intent.setType(mimeType);
            startActivityForResult(intent,RUN_MOD_INSTALLER);
        }

    }

    public void launchGame(View v) {
        if (!canBack && mIsAssetsProcessing) {
            mIsAssetsProcessing = false;
            statusIsLaunching(false);
        } else if (canBack) {
            v.setEnabled(false);
            mTask = new MinecraftDownloaderTask(this);
            // TODO: better check!!!
            if (mProfile.accessToken.equals("0")) {
                File verJsonFile = new File(Tools.DIR_HOME_VERSION,
                        mProfile.selectedVersion + "/" + mProfile.selectedVersion + ".json");
                if (verJsonFile.exists()) {
                    mTask.onPostExecute(null);
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.global_error)
                            .setMessage(R.string.mcl_launch_error_localmode)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            } else {
                mTask.execute(mProfile.selectedVersion);
            }

        }
    }
    
    @Override
    public void onBackPressed() {
        if (canBack) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        System.out.println("call to onPostResume");
        Tools.updateWindowSize(this);
        System.out.println("call to onPostResume; E");
    }
    
    @Override
    protected void onResume(){
        super.onResume();
        System.out.println("call to onResume");
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
        System.out.println("call to onResume; E");
    }

    SharedPreferences.OnSharedPreferenceChangeListener listRefreshListener = null;
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if(listRefreshListener == null) {
            listRefreshListener = (sharedPreferences, key) -> {
                if(key.startsWith("vertype_")) {
                    System.out.println("Verlist update needed!");
                    new RefreshVersionListTask(this).execute();
                }
            };
        }
        LauncherPreferences.DEFAULT_PREF.registerOnSharedPreferenceChangeListener(listRefreshListener);
        new RefreshVersionListTask(this).execute();
        System.out.println("call to onResumeFragments");
        mRuntimeConfigDialog = new MultiRTConfigDialog();
        mRuntimeConfigDialog.prepare(this);

        ((Button)findViewById(R.id.installJarButton)).setOnLongClickListener(view -> {
            installMod(true);
            return true;
        });

        //TODO ADD CRASH CHECK AND FOCUS
        System.out.println("call to onResumeFragments; E");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode == Activity.RESULT_OK) {
            final ProgressDialog barrier = new ProgressDialog(this);
            barrier.setMessage(getString(R.string.global_waiting));
            barrier.setProgressStyle(barrier.STYLE_SPINNER);
            barrier.setCancelable(false);
            barrier.show();

            // Install the runtime
            if (requestCode == MultiRTConfigDialog.MULTIRT_PICK_RUNTIME) {
                if (data == null) return;

                final Uri uri = data.getData();
                Thread t = new Thread(() -> {
                    try {
                        String name = getFileName(this, uri);
                        MultiRTUtils.installRuntimeNamed(getContentResolver().openInputStream(uri), name,
                                (resid, stuff) -> BaseLauncherActivity.this.runOnUiThread(
                                        () -> barrier.setMessage(BaseLauncherActivity.this.getString(resid, stuff))));
                        MultiRTUtils.postPrepare(BaseLauncherActivity.this, name);
                    } catch (IOException e) {
                        Tools.showError(BaseLauncherActivity.this, e);
                    }
                    BaseLauncherActivity.this.runOnUiThread(() -> {
                        barrier.dismiss();
                        mRuntimeConfigDialog.refresh();
                        mRuntimeConfigDialog.mDialog.show();
                    });
                });
                t.start();
            }

            // Run a mod installer
            if (requestCode == RUN_MOD_INSTALLER) {
                if (data == null) return;

                final Uri uri = data.getData();
                barrier.setMessage(BaseLauncherActivity.this.getString(R.string.multirt_progress_caching));
                Thread t = new Thread(()->{
                    try {
                        final String name = getFileName(this, uri);
                        final File modInstallerFile = new File(getCacheDir(), name);
                        FileOutputStream fos = new FileOutputStream(modInstallerFile);
                        IOUtils.copy(getContentResolver().openInputStream(uri), fos);
                        fos.close();
                        BaseLauncherActivity.this.runOnUiThread(() -> {
                            barrier.dismiss();
                            Intent intent = new Intent(BaseLauncherActivity.this, JavaGUILauncherActivity.class);
                            intent.putExtra("modFile", modInstallerFile);
                            startActivity(intent);
                        });
                    }catch(IOException e) {
                        Tools.showError(BaseLauncherActivity.this,e);
                    }
                });
                t.start();
            }

        }
    }

    protected abstract void initTabs(int pageIndex);
}
