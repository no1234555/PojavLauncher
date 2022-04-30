package net.kdt.pojavlaunch.modmanager.api;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import net.kdt.pojavlaunch.PojavLauncherActivity;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.tasks.RefreshVersionListTask;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Quilt {

    private static final String BASE_URL = "https://meta.quiltmc.org/v2/";
    private static Retrofit retrofit;
    private static String quiltLoaderVersion; //Store so we don't need to ask the api every time

    public static Retrofit getClient(){
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public interface QuiltLoaderVersionsInf {
        @GET("versions/loader")
        Call<List<Version>> getVersions();
    }

    public interface QuiltLoaderJsonInf {
        @GET("versions/loader/{gameVersion}/{loaderVersion}/profile/json")
        Call<JsonObject> getJson(@Path("gameVersion") String gameVersion, @Path("loaderVersion") String loaderVersion);
    }

    public static class Version {
        @SerializedName("version")
        public String version;
        @SerializedName("stable")
        public boolean stable;
    }

    //Will only ask api first time called, return fabricLoadVersion var every next time - save on unneeded api calls
    public static String getLatestLoaderVersion()  {
        if (quiltLoaderVersion != null) return quiltLoaderVersion;

        try {
            QuiltLoaderVersionsInf inf = getClient().create(QuiltLoaderVersionsInf.class);
            List<Version> versions = inf.getVersions().execute().body();

            if (versions != null) {
                for (Version version : versions) {
                    if (version.stable) {
                        quiltLoaderVersion = version.version;
                        return version.version;
                    }
                }
            }
        } catch (IOException e) {
            return "0.16.0-beta.15"; //Known latest as backup
        }

        quiltLoaderVersion = "0.16.0-beta.15"; //Known latest as backup
        return quiltLoaderVersion;
    }

    //Won't do anything if version is already installed
    public static void downloadJson(PojavLauncherActivity activity, String gameVersion, String loaderVersion) {
        String profileName = String.format("%s-%s-%s", "quilt-loader", loaderVersion, gameVersion);
        File path = new File(Tools.DIR_HOME_VERSION + "/" + profileName);
        if (new File(path.getPath() + "/" + profileName + ".json").exists()) return;

        try {
            QuiltLoaderJsonInf jsonInf = getClient().create(QuiltLoaderJsonInf.class);
            JsonObject json = jsonInf.getJson(gameVersion, loaderVersion).execute().body();
            if (json != null) {
                if (!path.exists()) path.mkdirs();
                json.addProperty("type", "fabric");
                Tools.write(path.getPath() + "/" + profileName + ".json", json.toString());
                new RefreshVersionListTask(activity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
