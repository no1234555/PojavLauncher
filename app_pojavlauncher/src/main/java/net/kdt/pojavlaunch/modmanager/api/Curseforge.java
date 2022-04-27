package net.kdt.pojavlaunch.modmanager.api;

import android.util.Log;
import com.google.gson.annotations.SerializedName;
import net.kdt.pojavlaunch.fragments.ModsFragment;
import net.kdt.pojavlaunch.modmanager.ModData;
import net.kdt.pojavlaunch.modmanager.ModManager;
import net.kdt.pojavlaunch.utils.UiUitls;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import us.feras.mdv.MarkdownView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Curseforge {

    private static final String BASE_URL = "https://qcxr-modmanager-curseforge-api.herokuapp.com/";
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public interface ProjectInf {
        @GET("getMod/{id}")
        Call<Project> getProject(@Path("id") String id);
    }

    public interface SearchInf {
        @GET("searchMods")
        Call<List<Project>> searchMods(@Query("gameId") int gameId, @Query("gameVersion") String gameVersion, @Query("searchFilter") String searchFilter,
                                       @Query("modLoaderType") int modLoaderType, @Query("index") int index, @Query("pageSize") int pageSize);
    }

    public interface DescriptionInf {
        @GET("getModDescription/{id}")
        Call<Description> getDescription(@Path("id") String id);
    }

    public static class Project {
        @SerializedName("name")
        public String name;
        @SerializedName("id")
        public int id;
        @SerializedName("logo")
        public Logo logo;
        @SerializedName("latestFilesIndexes")
        public List<FileIndex> latestFilesIndexes;
    }

    public static class Logo {
        @SerializedName("thumbnailUrl")
        public String thumbnailUrl;
    }

    public static class FileIndex {
        @SerializedName("gameVersion")
        public String gameVersion;
        @SerializedName("fileId")
        public int fileId;
        @SerializedName("filename")
        public String filename;
        @SerializedName("modLoader")
        public int modLoader;
    }

    public static class Description {
        @SerializedName("data")
        public String data;
    }

    public static ModData getModFileData(String id, String gameVersion) throws IOException {
        ProjectInf projectInf = getClient().create(ProjectInf.class);
        Project project = projectInf.getProject(id).execute().body();
        if (project == null) return null;

        for (FileIndex file : project.latestFilesIndexes) {
            if (file.modLoader == 4 && file.gameVersion.equals(gameVersion)) {
                ModData modData = new ModData();
                modData.platform = "curse";
                modData.title = project.name;
                modData.slug = String.valueOf(project.id);
                modData.iconUrl = project.logo.thumbnailUrl;

                modData.fileData.id = String.valueOf(file.fileId);
                modData.fileData.url = APIUtil.getRaw("https://addons-ecs.forgesvc.net/api/v2/addon/" + project.id + "/file/" + file.fileId + "/download-url");
                modData.fileData.filename = file.filename;
                return modData;
            }
        }
        return null;
    }

    public static void addProjectsToRecycler(ModsFragment.ModAdapter adapter, String version, int offset, String query) {
        SearchInf searchInf = getClient().create(SearchInf.class);
        searchInf.searchMods(432, version, query, 4, offset, 50).enqueue(new Callback<List<Project>>() {

            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                List<Project> projects = response.body();
                if (projects == null) return;

                ArrayList<ModData> mods = new ArrayList<>();
                for (Project project : projects) {
                    ModData modData = new ModData();
                    modData.title = project.name;
                    modData.slug = String.valueOf(project.id);
                    modData.iconUrl = project.logo.thumbnailUrl;

                    for (ModData installedMod : ModManager.listInstalledMods("fabric-loader-" + Fabric.getLatestLoaderVersion() + "-1.18.2")) {
                        if (installedMod.isActive && String.valueOf(project.id).equals(installedMod.slug)) {
                            modData.isActive = true;
                            break;
                        }
                    }

                    mods.add(modData);
                }
                adapter.addMods(mods);
                if (offset == 0) adapter.loadProjectPage(mods.get(0), null);
            }

            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {
                Log.d("CURSE", String.valueOf(t));
            }
        });
    }

    public static void loadProjectPage(MarkdownView view, String id) {
        view.loadMarkdown("");
        DescriptionInf descriptionInf = getClient().create(DescriptionInf.class);
        descriptionInf.getDescription(id).enqueue(new Callback<Description>() {
            @Override
            public void onResponse(Call<Description> call, Response<Description> response) {
                Description description = response.body();
                if (description != null) UiUitls.runOnUI(() -> view.loadMarkdown(description.data, "file:///android_asset/ModDescription.css"));
            }

            @Override
            public void onFailure(Call<Description> call, Throwable t) {
                Log.d("CURSE", String.valueOf(t));
            }
        });


        /*Thread thread = new Thread() {
            @Override
            public void run() {
                String description = APIUtil.getRaw("https://addons-ecs.forgesvc.net/api/v2/addon/" + id + "/description");
                UiUitls.runOnUI(() -> view.loadMarkdown(String.valueOf(description), "file:///android_asset/ModDescription.css"));
            }
        };
        thread.start();*/
    }
}
