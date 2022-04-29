package net.kdt.pojavlaunch.modmanager;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class State {
    @SerializedName("instances")
    private List<Instance> instances;
    @SerializedName("core_mods")
    private HashMap<String, List<ModData>> coreMods;

    public void overwrite(State state) {
        this.instances = state.instances;
        this.coreMods = state.coreMods;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public Instance getInstance(String name) {
        for (Instance instance : instances) {
            if (instance.name.equalsIgnoreCase(name)) return instance;
        }
        return null;
    }

    public void addCoreMod(String version, ModData modData) {
        List<ModData> mods = coreMods.get(version);
        if (mods == null) mods = new ArrayList<>();
        mods.add(modData);
        coreMods.put(version, mods);
    }

    public List<ModData> getCoreMods(String version) {
        List<ModData> mods = coreMods.get(version);
        if (mods != null) return mods;
        return new ArrayList<>();
    }

    public void addInstance(Instance instance) {
        instances.add(instance);
    }

    public static class Instance {
        @SerializedName("name")
        private String name;
        @SerializedName("gameVersion")
        private String gameVersion;
        @SerializedName("fabricLoaderVersion")
        private String fabricLoaderVersion;
        @SerializedName("mods")
        private List<ModData> mods;

        public void setName(String name) {
            this.name = name;
        }

        public void setGameVersion(String gameVersion) {
            this.gameVersion = gameVersion;
        }

        public void setFabricLoaderVersion(String fabricLoaderVersion) {
            this.fabricLoaderVersion = fabricLoaderVersion;
        }

        public void addMod(ModData modData) {
            this.mods.add(modData);
        }

        public String getName() {
            return name;
        }

        public String getGameVersion() {
            return gameVersion;
        }

        public String getFabricLoaderVersion() {
            return fabricLoaderVersion;
        }

        public List<ModData> getMods() {
            return mods;
        }

        public ModData getMod(String slug) {
            for (ModData mod : mods) {
                if (mod.slug.equals(slug)) return mod;
            }
            return null;
        }
    }
}
