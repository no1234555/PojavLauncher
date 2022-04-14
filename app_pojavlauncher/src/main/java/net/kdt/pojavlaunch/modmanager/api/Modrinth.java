package net.kdt.pojavlaunch.modmanager.api;

import android.util.Log;
import com.google.gson.annotations.SerializedName;
import net.kdt.pojavlaunch.fragments.ModsFragment;
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

public class Modrinth {

    private static final String BASE_URL = "https://api.modrinth.com/v2/";
    private static Retrofit retrofit;

    public static Retrofit getClient(){
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public interface ProjectInf {
        @GET("project/{slug}")
        Call<Project> getProject(@Path("slug") String slug);
    }

    public interface VersionsInf {
        @GET("project/{slug}/version")
        Call<List<Version>> getVersions(@Path("slug") String slug);
    }

    public interface SearchInf {
        @GET("search")
        Call<SearchResult> searchMods(@Query("query") String query, @Query("offset") int offset, @Query("limit") int limit, @Query("facets") String[][] facets);
    }

    public static class Project {
        @SerializedName("title")
        public String title;
        @SerializedName("slug")
        public String slug;
        @SerializedName("icon_url")
        public String iconUrl;
        @SerializedName("body")
        public String body;
    }

    public static class Version {
        @SerializedName("id")
        public String id;
        @SerializedName("loaders")
        public List<String> loaders;
        @SerializedName("game_versions")
        public List<String> gameVersions;
        @SerializedName("files")
        public List<File> files;

        public static class File {
            @SerializedName("url")
            public String url;
            @SerializedName("filename")
            public String filename;
        }
    }

    public static class SearchResult {
        @SerializedName("hits")
        public List<ModData> hits;
    }

    public static ModData getModFileData(String slug, String gameVersion) throws IOException {
        ProjectInf projectInf = getClient().create(ProjectInf.class);
        Project project = projectInf.getProject(slug).execute().body();

        VersionsInf versionsInf = getClient().create(VersionsInf.class);
        List<Version> versions = versionsInf.getVersions(slug).execute().body();

        if (project == null || versions == null) {
            return null;
        }

        for (Version modVersion : versions) {
            for (String loader : modVersion.loaders) {
                if (loader.equals("fabric")) {
                    for (String modGameVersion : modVersion.gameVersions) {
                        if (modGameVersion.equals(gameVersion)) {
                            Version.File file = modVersion.files.get(0);

                            ModData modData = new ModData();
                            modData.platform = "modrinth";
                            modData.title = project.title;
                            modData.slug = project.slug;
                            modData.iconUrl = project.iconUrl;
                            modData.fileData.id = modVersion.id;
                            modData.fileData.url = file.url;
                            modData.fileData.filename = file.filename;
                            return modData;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static void addProjectsToRecycler(ModsFragment.ModAdapter adapter, String version, int offset, String query) {
        SearchInf searchInf = getClient().create(SearchInf.class);
        searchInf.searchMods(query, offset, 50, new String[][]{{"categories:fabric"}, {"versions:" + version}}).enqueue(new Callback<SearchResult>() {
            @Override
            public void onResponse(Call<SearchResult> call, Response<SearchResult> response) {
                SearchResult result = response.body();
                if (result != null) adapter.addMods((ArrayList<ModData>) result.hits);
            }

            @Override
            public void onFailure(Call<SearchResult> call, Throwable t) {
                Log.d("MODRINTH", String.valueOf(t));
            }
        });
    }

    public static void loadProjectPage(MarkdownView view, String slug) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    ProjectInf projectInf = getClient().create(ProjectInf.class);
                    Project project = projectInf.getProject(slug).execute().body();
                    if (project != null) UiUitls.runOnUI(() -> view.loadMarkdown(project.body, "file:///android_asset/ModDescription.css"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }
}