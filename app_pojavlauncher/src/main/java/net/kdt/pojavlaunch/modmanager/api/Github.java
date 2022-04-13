package net.kdt.pojavlaunch.modmanager.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import net.kdt.pojavlaunch.utils.UiUitls;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import us.feras.mdv.MarkdownView;

import java.io.IOException;
import java.util.List;

public class Github {

    private static final String BASE_URL = "https://api.github.com/repos/";
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

    public interface GithubReleasesInf {
        @GET("{user}/{repo}/releases")
        Call<List<Release>> getReleases(@Path("user") String user, @Path("repo") String repo);
    }

    public static class Release {
        @SerializedName("name")
        public String name;
        @SerializedName("id")
        public String id;
        @SerializedName("assets")
        public List<Asset> assets;
    }

    public static class Asset {
        @SerializedName("name")
        public String name;
        @SerializedName("browser_download_url")
        public String url;
    }

    public static ModData getModFileData(JsonArray repoList, String slug, String gameVersion) throws IOException {
        for (JsonElement repo : repoList) {
            String[] repoData = repo.getAsString().split("/");

            GithubReleasesInf releasesInf = getClient().create(GithubReleasesInf.class);
            List<Release> releases = releasesInf.getReleases(repoData[0], repoData[1]).execute().body();

            if (releases == null) {
                return null;
            }

            for (Release release : releases) {
                if (release.name.split("-")[1].equals(gameVersion)) {
                    for (Asset asset : release.assets) {
                        if (asset.name.replace(".jar", "").equals(slug)) {
                            ModData modData = new ModData();

                            modData.platform = "github";
                            modData.title = slug;
                            modData.slug = slug;
                            modData.fileData.id = release.id;
                            modData.fileData.url = asset.url;
                            modData.fileData.filename = asset.name;
                            return modData;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static void loadProjectPage(MarkdownView view, String repo) {
        UiUitls.runOnUI(() -> view.loadMarkdownFile("https://raw.githubusercontent.com/" + repo + "/master/README.md", "file:///assets/ModDescription.css"));
    }
}
