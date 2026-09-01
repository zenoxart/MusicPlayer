package com.musicplayer.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class SettingsStore {

    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".wav", ".mp3", ".m4a", ".aac");
    private static final Set<String> DEFAULT_EXTENSIONS = Set.of(".wav");

    private final Path storeFile;
    private Set<String> extensions = new LinkedHashSet<>(DEFAULT_EXTENSIONS);

    public SettingsStore() {
        this.storeFile = AppDataLocations.resolve("settings.json");
        load();
    }

    private void load() {
        if (!Files.exists(storeFile)) {
            return;
        }
        try {
            String content = Files.readString(storeFile, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(content);
            JSONArray array = json.optJSONArray("extensions");
            if (array == null) {
                return;
            }
            Set<String> loaded = new LinkedHashSet<>();
            for (int i = 0; i < array.length(); i++) {
                loaded.add(array.getString(i));
            }
            if (!loaded.isEmpty()) {
                extensions = loaded;
            }
        } catch (IOException | JSONException ignored) {
        }
    }

    public Set<String> getExtensions() {
        return Set.copyOf(extensions);
    }

    public void setExtensions(Set<String> newExtensions) {
        extensions = newExtensions.isEmpty()
                ? new LinkedHashSet<>(DEFAULT_EXTENSIONS)
                : new LinkedHashSet<>(newExtensions);
        save();
    }

    private void save() {
        JSONObject json = new JSONObject();
        json.put("extensions", new JSONArray(extensions));
        try {
            Files.writeString(storeFile, json.toString(2), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
