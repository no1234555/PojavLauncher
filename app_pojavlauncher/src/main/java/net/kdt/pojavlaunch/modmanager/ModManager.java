package net.kdt.pojavlaunch.modmanager;

import android.util.Pair;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modmanager.State.Instance;
import net.kdt.pojavlaunch.modmanager.api.*;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ModManager {

    public static final String workDir = Tools.DIR_GAME_NEW + "/modmanager";
    public static State state = new State();
    private static JsonObject modCompats = new JsonObject();
    private static JsonObject modmanagerJson = new JsonObject();
    private static final ArrayList<String> currentDownloadSlugs = new ArrayList<>();
    private static boolean saveStateCalled = false;

    public static void init() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    File path = new File(workDir);
                    if (!path.exists()) path.mkdir();

                    File modsJson = new File(workDir + "/mods.json");
                    String flVersion = Fabric.getLatestLoaderVersion(); //Init outside to cache version (see Fabric.java)
                    if (!modsJson.exists()) {
                        String gameVersion = Tools.getCompatibleVersions("releases").get(0);
                        Fabric.downloadJson(gameVersion, flVersion);

                        String profileName = String.format("%s-%s-%s", "fabric-loader", flVersion, gameVersion);
                        Instance instance = new Instance();
                        instance.setName("fabric-loader-0.13.3-1.18.2");
                        instance.setGameVersion(gameVersion);
                        instance.setFabricLoaderVersion(profileName);
                        state.addInstance(instance);
                        Tools.write(modsJson.getPath(), Tools.GLOBAL_GSON.toJson(state)); //Cant use save state cause async issues
                    } else state = Tools.GLOBAL_GSON.fromJson(Tools.read(modsJson.getPath()), net.kdt.pojavlaunch.modmanager.State.class);

                    InputStream modmanagerFile = PojavApplication.assetManager.open("jsons/modmanager.json");
                    modmanagerJson = Tools.GLOBAL_GSON.fromJson(Tools.read(modmanagerFile), JsonObject.class);
                    InputStream compatFile = PojavApplication.assetManager.open("jsons/mod-compat.json");
                    modCompats = Tools.GLOBAL_GSON.fromJson(Tools.read(compatFile), JsonObject.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public static ArrayList<Pair<String, String>> getCoreModsFromJson(String version) {
        ArrayList<Pair<String, String>> mods = new ArrayList<>();
        for (JsonElement element : modmanagerJson.get("core_mods").getAsJsonObject().getAsJsonArray(version)) {
            JsonObject mod = element.getAsJsonObject();
            mods.add(new Pair<>(mod.get("slug").getAsString(), mod.get("platform").getAsString()));
        }
        return mods;
    }

    public static String getModCompat(String slug) {
        JsonElement compatLevel = modCompats.get(slug);
        if (compatLevel != null) return compatLevel.getAsString();
        return "Untested";
    }

    public static String getWorkDir() {
        return workDir;
    }

    //Will only save the state if there is nothing currently happening
    public static void saveState() {
        Thread thread = new Thread() {
            public void run() {
                while (currentDownloadSlugs.size() > 0) {
                    synchronized (state) {
                        try {
                            state.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    Tools.write(workDir + "/mods.json", Tools.GLOBAL_GSON.toJson(state));
                    saveStateCalled = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        if (!saveStateCalled) {
            saveStateCalled = true;
            thread.start();
        }
    }

    public static boolean isDownloading(String slug) {
        return currentDownloadSlugs.contains(slug);
    }

    public static void createInstance(String name, String gameVersion) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                String flVersion = Fabric.getLatestLoaderVersion();
                Fabric.downloadJson(gameVersion, flVersion);

                String profileName = String.format("%s-%s-%s", "fabric-loader", flVersion, gameVersion);
                Instance instance = new Instance();
                instance.setName(name);
                instance.setGameVersion(gameVersion);
                instance.setFabricLoaderVersion(profileName);
                state.addInstance(instance);
                saveState();
            }
        };
        thread.start();
    }

    public static void addMod(String instanceName, String platform, String slug, String gameVersion, boolean isCoreMod) {
        Thread thread = new Thread() {
            public void run() {
                currentDownloadSlugs.add(slug);

                File path;
                if (isCoreMod) path = new File(workDir + "/core/" + gameVersion);
                else path = new File(workDir + "/instances/" + instanceName);
                if (!path.exists()) path.mkdir();

                try {
                    ModData modData = null;
                    if (platform.equals("modrinth")) modData = Modrinth.getModFileData(slug, gameVersion);
                    else if (platform.equals("curseforge")) modData = Curseforge.getModFileData(slug, gameVersion);
                    else if (platform.equals("github")) modData = Github.getModFileData(modmanagerJson.getAsJsonArray("repos"), slug, gameVersion);
                    if (modData == null) return;
                    modData.isActive = true;

                    //No duplicate mods allowed
                    if (isCoreMod) {
                        for (ModData mod : state.getCoreMods(gameVersion)) {
                            if (mod.slug.equals(modData.slug)) return;
                        }
                        state.addCoreMod(gameVersion, modData);
                    } else {
                        Instance instance = state.getInstance(instanceName);
                        for (ModData mod : instance.getMods()) {
                            if (mod.slug.equals(modData.slug)) return;
                        }
                        instance.addMod(modData);
                    }

                    DownloadUtils.downloadFile(modData.fileData.url, new File(path.getPath() + "/" + modData.fileData.filename));
                    currentDownloadSlugs.remove(slug);

                    saveState();
                    synchronized (state) {
                        state.notifyAll();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public static void removeMod(String instanceName, String slug) {
        Thread thread = new Thread() {
            public void run() {
                Instance instance = state.getInstance(instanceName);
                ModData modData = getMod(instanceName, slug);
                if (modData == null) return;

                File modJar = new File(workDir + "/instances/" + instanceName + "/" + modData.fileData.filename);
                if (modJar.delete()) {
                    instance.getMods().remove(modData);
                    saveState();
                }
            }
        };
        if (!isDownloading(slug)) thread.start();
    }

    public static ModData getMod(String instanceName, String slug) {
        Instance instance = state.getInstance(instanceName);
        for (ModData mod : instance.getMods()) {
            if (mod.slug.equals(slug)) return mod;
        }
        return null;
    }

    //Will return modData if there is an update, otherwise null
    public static ModData checkModForUpdate(String instanceName, String slug) {
        try {
            Instance instance = state.getInstance(instanceName);
            for (ModData mod : instance.getMods()) {
                if (mod.slug.equals(slug)) {
                    ModData modData = Modrinth.getModFileData(slug, instance.getGameVersion());
                    if (modData != null && !mod.fileData.id.equals(modData.fileData.id)) return modData;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setModActive(String instanceName, String slug, boolean active) {
        Thread thread = new Thread() {
            public void run() {
                if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.O) return;

                ModData modData = getMod(instanceName, slug);
                if (modData == null) return;

                modData.isActive = active;
                String suffix = "";
                if (!active) suffix = ".disabled";

                File path = new File(workDir + "/instances/" + instanceName);
                for (File modJar : path.listFiles()) {
                    if (modJar.getName().replace(".disabled", "").equals(modData.fileData.filename)) {
                        try {
                            Path source = Paths.get(modJar.getPath());
                            Files.move(source, source.resolveSibling(modData.fileData.filename + suffix));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                saveState();
            }
        };
        thread.start();
    }

    public static ArrayList<ModData> listInstalledMods(String instanceName) {
        return (ArrayList<ModData>) state.getInstance(instanceName).getMods();
    }

    public static ArrayList<ModData> listCoreMods(String gameVersion) {
        return (ArrayList<ModData>) state.getCoreMods(gameVersion);
    }
}