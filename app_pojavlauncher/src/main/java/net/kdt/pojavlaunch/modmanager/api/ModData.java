package net.kdt.pojavlaunch.modmanager.api;

import com.google.gson.annotations.SerializedName;

public class ModData {
    @SerializedName("title")
    public String title;
    @SerializedName("slug")
    public String slug;
    @SerializedName("icon_url")
    public String iconUrl;
    @SerializedName("description")
    public String description;
    @SerializedName("body")
    public String body;

    public String platform;
    public boolean isActive = false;
    public FileData fileData;

    //Only set when calling a getModFileData method
    public static class FileData {
        public String id;
        public String url;
        public String filename;
    }
}