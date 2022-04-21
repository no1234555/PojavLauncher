package net.kdt.pojavlaunch.modmanager.api;

import android.os.Build;
import android.util.Log;
import com.google.gson.annotations.SerializedName;
import net.kdt.pojavlaunch.fragments.ModsFragment;
import net.kdt.pojavlaunch.modmanager.ModData;
import net.kdt.pojavlaunch.modmanager.ModManager;
import net.kdt.pojavlaunch.utils.UiUitls;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import us.feras.mdv.MarkdownView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Curseforge {

        private static final String BASE_URL = "https://addons-ecs.forgesvc.net/api/v2/addon/";
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
        @GET("{id}")
        Call<Project> getProject(@Path("id") String id);
    }

    public interface VersionsInf {
        @GET("{id}/files")
        Call<List<Version>> getVersions(@Path("id") String id);
    }

    public interface SearchInf {
        @GET("search")
        Call<List<Project>> searchMods(@Query("categoryID") int categoryID, @Query("gameId") int gameId,
                                               @Query("gameVersion") String gameVersion, @Query("index") int index,
                                               @Query("pageSize") int pageSize, @Query("searchFilter") String searchFilter,
                                               @Query("sectionId") int sectionId, @Query("sort") int sort);
    }

    public interface DescriptionInf {
        @GET("{id}/description")
        Call<String> getDescription(@Path("id") String id);
    }

    public static class Project {
        @SerializedName("name")
        public String name;
        @SerializedName("id")
        public String id;
        @SerializedName("attachments")
        public List<Attachment> attachments;
    }

    public static class Attachment {
        @SerializedName("thumbnailUrl")
        public String thumbnailUrl;
    }

    public static class Version {
        @SerializedName("id")
        public int id;
        @SerializedName("gameVersion")
        public List<String> gameVersions;
        @SerializedName("downloadUrl")
        public String downloadUrl;
        @SerializedName("fileName")
        public String filename;
        @SerializedName("fileDate")
        public String fileDate;
        @SerializedName("modules")
        public List<Module> modules;
    }

    public static class Module {
        @SerializedName("foldername")
        public String folderName;
    }

    public static ModData getModFileData(String id, String gameVersion) throws IOException {
        ProjectInf projectInf = getClient().create(ProjectInf.class);
        Project project = projectInf.getProject(id).execute().body();

        VersionsInf versionsInf = getClient().create(VersionsInf.class);
        List<Version> versions = versionsInf.getVersions(id).execute().body();

        if (project == null || versions == null || Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) return null;

        Version version = null;
        Instant instant = Instant.now();
        for (Version v : versions) {
            for (Module module : v.modules) {
                if (module.folderName.equals("fabric.mod.json")) {
                    for (String gVersion : v.gameVersions) {
                        if (gVersion.equals(gameVersion)) {
                            Instant i = Instant.parse(v.fileDate);

                            if (instant.isBefore(i)) {
                                version = v;
                                instant = i;
                            }
                        }
                    }
                }
            }
        }

        if (version == null) return null;

        ModData modData = new ModData();
        modData.platform = "curse";
        modData.title = project.name;
        modData.slug = String.valueOf(id);
        modData.iconUrl = project.attachments.get(0).thumbnailUrl;
        modData.fileData.id = String.valueOf(version.id);
        modData.fileData.url = version.downloadUrl;
        modData.fileData.filename = version.filename;
        return modData;
    }

    public static void addProjectsToRecycler(ModsFragment.ModAdapter adapter, String version, int offset, String query) {
        SearchInf searchInf = getClient().create(SearchInf.class);
        searchInf.searchMods(0, 432, version, offset, 50, query, 0, 0).enqueue(new Callback<List<Project>>() {

            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                List<Project> projects = response.body();
                if (projects == null) return;

                ArrayList<ModData> mods = new ArrayList<>();
                for (Project project : projects) {
                    ModData modData = new ModData();
                    modData.title = project.name;
                    modData.slug = String.valueOf(project.id);
                    modData.iconUrl = project.attachments.get(0).thumbnailUrl;

                    for (ModData installedMod : ModManager.listInstalledMods("fabric-loader-" + Fabric.getLatestLoaderVersion() + "-1.18.2")) {
                        if (installedMod.isActive && project.id.equals(installedMod.slug)) {
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
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    //Temp jank fix
                    URL u = new URL(BASE_URL + id + "/description");
                    URLConnection conn = u.openConnection();
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder buffer = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) buffer.append(inputLine);
                    in.close();

                    /*DescriptionInf descriptionInf = getScalarClient().create(DescriptionInf.class);
                    String description = descriptionInf.getDescription(id).execute().body();*/
                    UiUitls.runOnUI(() -> view.loadMarkdown(String.valueOf(buffer), "file:///android_asset/ModDescription.css"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }
}
