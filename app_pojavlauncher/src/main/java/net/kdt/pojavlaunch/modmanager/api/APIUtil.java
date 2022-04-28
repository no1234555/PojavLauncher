package net.kdt.pojavlaunch.modmanager.api;

import android.os.Build;
import net.kdt.pojavlaunch.Tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;

public class APIUtil {

    public static class APIHandler {

        private final String baseUrl;

        public APIHandler(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public <T> T get(String endpoint, Class<T> tClass) {
            return Tools.GLOBAL_GSON.fromJson(getRaw(baseUrl + "/" + endpoint), (Type) tClass);
        }

        public <T> T get(String endpoint, HashMap<String, Object> query, Class<T> tClass) {
            StringBuilder params = new StringBuilder("?");
            for (String param : query.keySet()) {
                Object value = query.get(param);
                params.append(param).append("=").append(value).append("&");
            }
            String parsedParams = params.substring(0, params.length() - 1);
            return get(endpoint + parsedParams, tClass);
        }
    }

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
}
