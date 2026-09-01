package com.musicplayer.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Playlist {

    private final String id;
    private String name;
    private final List<Path> songPaths;

    public Playlist(String id, String name, List<Path> songPaths) {
        this.id = id;
        this.name = name;
        this.songPaths = new ArrayList<>(songPaths);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Path> getSongPaths() {
        return songPaths;
    }

    public boolean containsSong(Path path) {
        Path absolute = path.toAbsolutePath();
        return songPaths.stream().anyMatch(p -> p.toAbsolutePath().equals(absolute));
    }
}
