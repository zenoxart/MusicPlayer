package com.musicplayer.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.image.Image;
import javafx.util.Duration;

import java.nio.file.Path;

public class Song {

    private final Path path;
    private final String title;
    private final String artist;
    private final String album;
    private final Duration duration;
    private final Image coverArt;
    private final IntegerProperty rating = new SimpleIntegerProperty(0);

    public Song(Path path, String title, String artist, String album, Duration duration, Image coverArt) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.coverArt = coverArt;
    }

    public Path getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Duration getDuration() {
        return duration;
    }

    public Image getCoverArt() {
        return coverArt;
    }

    public int getRating() {
        return rating.get();
    }

    public void setRating(int value) {
        rating.set(value);
    }

    public IntegerProperty ratingProperty() {
        return rating;
    }

    public String getDurationFormatted() {
        if (duration == null || duration.isUnknown()) {
            return "--:--";
        }
        int totalSeconds = (int) duration.toSeconds();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
