package com.musicplayer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MusicScanner {

    public List<Path> scan(Path folder, Set<String> extensions) throws IOException {
        Set<String> lowerExtensions = extensions.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        try (Stream<Path> paths = Files.walk(folder)) {
            Map<String, Path> uniqueByFilename = new LinkedHashMap<>();
            paths.filter(Files::isRegularFile)
                    .filter(path -> isAudioFile(path, lowerExtensions))
                    .forEach(path -> uniqueByFilename.putIfAbsent(
                            path.getFileName().toString().toLowerCase(), path));
            return List.copyOf(uniqueByFilename.values());
        }
    }

    private boolean isAudioFile(Path path, Set<String> extensions) {
        String name = path.getFileName().toString().toLowerCase();
        return extensions.stream().anyMatch(name::endsWith);
    }
}
