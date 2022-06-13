package net.kdt.pojavlaunch.utils;

import android.os.Build;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.kdt.pojavlaunch.Tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;

//Kinda cursed? that's all part of the fun 😎
public class APIUtils {

    public static class APIHandler {

        private final String baseUrl;

        public APIHandler(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        private String parseQueries(HashMap<String, Object> query) {
            StringBuilder params = new StringBuilder("?");
            for (String param : query.keySet()) {
                Object value = query.get(param);
                params.append(param).append("=").append(value).append("&");
            }
            return params.substring(0, params.length() - 1);
        }

        public <T> T get(String endpoint, Class<T> tClass) {
            return Tools.GLOBAL_GSON.fromJson(getRaw(baseUrl + "/" + endpoint), tClass);
        }

        public <T> T get(String endpoint, HashMap<String, Object> query, Class<T> tClass) {
            return get(endpoint + parseQueries(query), tClass);
        }
    }

    //Make a get request and return the response as a raw string;
    public static String getRaw(String url) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) return null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            InputStream inputStream = conn.getInputStream();
            String data = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            inputStream.close();
            conn.disconnect();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Make a get request and return the response as a raw string;
    public static String getCurseforgeJsonURL(String raw) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) return null;
        JsonObject jsonObject = Tools.GLOBAL_GSON.fromJson(getRaw(raw), JsonObject.class);
        raw = String.valueOf(jsonObject.get("data")).replaceAll("\"", "");
        return raw;
    }
}