package com.musicplayer.service;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ThemeStore {

    public static final String DEFAULT_ACCENT = "#1DB954";

    private final Path storeFile;
    private boolean darkMode = true;
    private String accentColor = DEFAULT_ACCENT;
    private boolean configured = false;

    public ThemeStore() {
        this.storeFile = AppDataLocations.resolve("theme.json");
        load();
    }

    private void load() {
        if (!Files.exists(storeFile)) {
            return;
        }
        try {
            String content = Files.readString(storeFile, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(content);
            darkMode = json.optBoolean("darkMode", true);
            accentColor = json.optString("accentColor", DEFAULT_ACCENT);
            configured = json.optBoolean("configured", false);
        } catch (IOException | JSONException ignored) {
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void save(boolean darkMode, String accentColor) {
        this.darkMode = darkMode;
        this.accentColor = accentColor;
        this.configured = true;

        JSONObject json = new JSONObject();
        json.put("darkMode", darkMode);
        json.put("accentColor", accentColor);
        json.put("configured", true);
        try {
            Files.writeString(storeFile, json.toString(2), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
