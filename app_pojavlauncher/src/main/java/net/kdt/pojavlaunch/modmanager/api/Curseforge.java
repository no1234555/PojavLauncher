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
        Call<ProjectResult> getProject(@Path("id") String id);
    }

    public interface SearchInf {
        @GET("searchMods")
        Call<SearchResult> searchMods(@Query("gameId") int gameId, @Query("gameVersion") String gameVersion, @Query("searchFilter") String searchFilter,
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

    public static class ProjectResult {
        @SerializedName("data")
        public Project data;
    }

    public static class Description {
        @SerializedName("data")
        public String data;
    }

    public static class SearchResult {
        @SerializedName("data")
        public List<Project> data;
    }

    public static ModData getModFileData(String id, String gameVersion) throws IOException {
        ProjectInf projectInf = getClient().create(ProjectInf.class);
        ProjectResult projectResult = projectInf.getProject(id).execute().body();
        if (projectResult == null) return null;

        Project project = projectResult.data;
        for (FileIndex file : project.latestFilesIndexes) {
            if (file.modLoader == 4 && file.gameVersion.equals(gameVersion)) {
                ModData modData = new ModData();
                modData.platform = "curseforge";
                modData.title = project.name;
                modData.slug = String.valueOf(project.id);
                modData.iconUrl = project.logo.thumbnailUrl;

                modData.fileData.id = String.valueOf(file.fileId);
                modData.fileData.filename = file.filename;

                //Work around for curse restricting mods outside CurseForge platform
                modData.fileData.url = APIUtil.getRaw("https://addons-ecs.forgesvc.net/api/v2/addon/" + project.id + "/file/" + file.fileId + "/download-url");
                return modData;
            }
        }
        return null;
    }

    public static void addProjectsToRecycler(ModsFragment.ModAdapter adapter, String version, int offset, String query) {
        SearchInf searchInf = getClient().create(SearchInf.class);
        searchInf.searchMods(432, version, query, 4, offset, 50).enqueue(new Callback<SearchResult>() {

            @Override
            public void onResponse(Call<SearchResult> call, Response<SearchResult> response) {
                SearchResult searchResult = response.body();
                if (searchResult == null) return;

                ArrayList<ModData> mods = new ArrayList<>();
                for (Project project : searchResult.data) {
                    ModData modData = new ModData();
                    modData.title = project.name;
                    modData.slug = String.valueOf(project.id);
                    if (project.logo != null) modData.iconUrl = project.logo.thumbnailUrl;
                    else modData.iconUrl = "";

                    for (ModData installedMod : ModManager.listInstalledMods("Default")) {
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
            public void onFailure(Call<SearchResult> call, Throwable t) {
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
    }
}
