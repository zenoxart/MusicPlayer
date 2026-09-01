package com.musicplayer.service;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class RatingStore {

    private final Path storeFile;
    private final Map<String, Integer> ratings = new HashMap<>();

    public RatingStore() {
        this.storeFile = AppDataLocations.resolve("ratings.json");
        load();
    }

    private void load() {
        if (!Files.exists(storeFile)) {
            return;
        }
        try {
            String content = Files.readString(storeFile, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(content);
            for (String key : json.keySet()) {
                ratings.put(key, json.getInt(key));
            }
        } catch (IOException | JSONException ignored) {
        }
    }

    public int getRating(Path songPath) {
        return ratings.getOrDefault(key(songPath), 0);
    }

    public void setRating(Path songPath, int rating) {
        if (rating <= 0) {
            ratings.remove(key(songPath));
        } else {
            ratings.put(key(songPath), rating);
        }
        save();
    }

    private void save() {
        JSONObject json = new JSONObject(ratings);
        try {
            Files.writeString(storeFile, json.toString(2), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private String key(Path path) {
        return path.toAbsolutePath().toString();
    }
}
