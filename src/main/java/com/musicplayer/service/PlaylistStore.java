package com.musicplayer.service;

import com.musicplayer.model.Playlist;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaylistStore {

    private final Path storeFile;
    private final List<Playlist> playlists = new ArrayList<>();

    public PlaylistStore() {
        this.storeFile = AppDataLocations.resolve("playlists.json");
        load();
    }

    private void load() {
        if (!Files.exists(storeFile)) {
            return;
        }
        try {
            String content = Files.readString(storeFile, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(content);
            JSONArray array = json.optJSONArray("playlists");
            if (array == null) {
                return;
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject entry = array.getJSONObject(i);
                String id = entry.getString("id");
                String name = entry.getString("name");
                JSONArray songsJson = entry.optJSONArray("songs");
                List<Path> songs = new ArrayList<>();
                if (songsJson != null) {
                    for (int j = 0; j < songsJson.length(); j++) {
                        songs.add(Path.of(songsJson.getString(j)));
                    }
                }
                playlists.add(new Playlist(id, name, songs));
            }
        } catch (IOException | JSONException ignored) {
        }
    }

    public List<Playlist> getPlaylists() {
        return List.copyOf(playlists);
    }

    public Playlist createPlaylist(String name) {
        Playlist playlist = new Playlist(UUID.randomUUID().toString(), name, List.of());
        playlists.add(playlist);
        save();
        return playlist;
    }

    public void deletePlaylist(String id) {
        playlists.removeIf(p -> p.getId().equals(id));
        save();
    }

    public void addSongToPlaylist(String playlistId, Path songPath) {
        findById(playlistId).ifPresent(playlist -> {
            if (!playlist.containsSong(songPath)) {
                playlist.getSongPaths().add(songPath.toAbsolutePath());
                save();
            }
        });
    }

    public void removeSongFromPlaylist(String playlistId, Path songPath) {
        findById(playlistId).ifPresent(playlist -> {
            Path absolute = songPath.toAbsolutePath();
            if (playlist.getSongPaths().removeIf(p -> p.toAbsolutePath().equals(absolute))) {
                save();
            }
        });
    }

    public void reorderPlaylist(String playlistId, List<Path> newOrder) {
        findById(playlistId).ifPresent(playlist -> {
            List<Path> songPaths = playlist.getSongPaths();
            songPaths.clear();
            songPaths.addAll(newOrder);
            save();
        });
    }

    private java.util.Optional<Playlist> findById(String id) {
        return playlists.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    private void save() {
        JSONArray array = new JSONArray();
        for (Playlist playlist : playlists) {
            JSONObject entry = new JSONObject();
            entry.put("id", playlist.getId());
            entry.put("name", playlist.getName());
            JSONArray songs = new JSONArray();
            for (Path path : playlist.getSongPaths()) {
                songs.put(path.toString());
            }
            entry.put("songs", songs);
            array.put(entry);
        }
        JSONObject json = new JSONObject();
        json.put("playlists", array);
        try {
            Files.writeString(storeFile, json.toString(2), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
