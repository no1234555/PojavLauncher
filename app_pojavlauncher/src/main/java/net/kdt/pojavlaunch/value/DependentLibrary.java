package net.kdt.pojavlaunch.value;

import androidx.annotation.Keep;

import net.kdt.pojavlaunch.JMinecraftVersionList;

@Keep
public class DependentLibrary {
    public String name;
	public LibraryDownloads downloads;
    public String url;
	public JMinecraftVersionList.Arguments.ArgValue.ArgRules[] rules;

    @Keep
	public static class LibraryDownloads {
		public MinecraftLibraryArtifact artifact;
		public LibraryDownloads(MinecraftLibraryArtifact artifact) {
			this.artifact = artifact;
		}
	}
}

