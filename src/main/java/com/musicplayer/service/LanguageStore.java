package com.musicplayer.service;

import com.musicplayer.model.Language;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LanguageStore {

    private final Path storeFile;
    private Language language = Language.ENGLISH;
    private boolean configured = false;

    public LanguageStore() {
        this.storeFile = AppDataLocations.resolve("language.json");
        load();
    }

    private void load() {
        if (!Files.exists(storeFile)) {
            return;
        }
        try {
            String content = Files.readString(storeFile, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(content);
            language = Language.fromCode(json.optString("language", "en"));
            configured = json.optBoolean("configured", false);
        } catch (IOException | JSONException ignored) {
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public Language getLanguage() {
        return language;
    }

    public void save(Language language) {
        this.language = language;
        this.configured = true;

        JSONObject json = new JSONObject();
        json.put("language", language.getCode());
        json.put("configured", true);
        try {
            Files.writeString(storeFile, json.toString(2), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
