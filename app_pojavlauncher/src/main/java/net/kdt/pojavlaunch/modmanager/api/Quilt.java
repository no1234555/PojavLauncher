package net.kdt.pojavlaunch.modmanager.api;

import com.google.gson.annotations.SerializedName;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.APIUtils;

import java.io.File;
import java.io.IOException;

public class Quilt {

    private static final APIUtils.APIHandler handler = new APIUtils.APIHandler("https://meta.quiltmc.org/v3");
    private static String quiltLoaderVersion; //Store so we don't need to ask the api every time

    public static class Version {
        @SerializedName("version")
        public String version;
    }

    //Will only ask api first time called, return quiltLoadVersion var every next time - save on unneeded api calls
    public static String getLatestLoaderVersion()  {
        if (quiltLoaderVersion != null) return quiltLoaderVersion;

        Version[] versions = handler.get("versions/loader", Version[].class);
        if (versions != null && versions.length > 0) return versions[0].version;
        quiltLoaderVersion = "0.16.0-beta.15"; //Known latest as backup
        return quiltLoaderVersion;
    }

    //Won't do anything if version is already installed
    public static void downloadJson(String gameVersion, String loaderVersion) {
        String profileName = String.format("%s-%s-%s", "quilt-loader", loaderVersion, gameVersion);
        File path = new File(Tools.DIR_HOME_VERSION + "/" + profileName);
        if (new File(path.getPath() + "/" + profileName + ".json").exists()) return;

        try {
            String json = APIUtils.getRaw(String.format(handler.getBaseUrl() + "/versions/loader/%s/%s/profile/json", gameVersion, loaderVersion));
            if (json != null) {
                if (!path.exists()) path.mkdirs();
                Tools.write(path.getPath() + "/" + profileName + ".json", json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}