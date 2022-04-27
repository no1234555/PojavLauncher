package net.kdt.pojavlaunch.modmanager.api;

import android.os.Build;
import com.google.gson.JsonElement;
import net.kdt.pojavlaunch.Tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class APIUtil {

    //Make a get request and return the response as a raw string;
    public static String getRaw(String url) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) return null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            InputStream inputStream = conn.getInputStream();
            return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Make a get request and return the response as json
    public static JsonElement getJson(String url) {
        return Tools.GLOBAL_GSON.fromJson(getRaw(url), JsonElement.class);
    }
}
