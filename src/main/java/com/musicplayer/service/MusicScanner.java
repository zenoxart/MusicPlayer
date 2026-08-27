package com.musicplayer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MusicScanner {

    private static final String SUPPORTED_EXTENSION = ".wav";

    public List<Path> scan(Path folder) throws IOException {
        try (Stream<Path> paths = Files.walk(folder)) {
            Map<String, Path> uniqueByFilename = new LinkedHashMap<>();
            paths.filter(Files::isRegularFile)
                    .filter(this::isAudioFile)
                    .forEach(path -> uniqueByFilename.putIfAbsent(
                            path.getFileName().toString().toLowerCase(), path));
            return List.copyOf(uniqueByFilename.values());
        }
    }

    private boolean isAudioFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(SUPPORTED_EXTENSION);
    }
}
