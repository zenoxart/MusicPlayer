package com.musicplayer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppDataLocations {

    private AppDataLocations() {
    }

    public static Path resolve(String filename) {
        String appData = System.getenv("APPDATA");
        Path base = (appData != null && !appData.isBlank())
                ? Path.of(appData)
                : Path.of(System.getProperty("user.home"));
        Path dir = base.resolve("MusicPlayer");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir.resolve(filename);
    }
}
