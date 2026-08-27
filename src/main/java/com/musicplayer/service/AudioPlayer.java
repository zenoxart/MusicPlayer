package com.musicplayer.service;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.nio.file.Path;

public class AudioPlayer {

    private MediaPlayer mediaPlayer;
    private Runnable onEndOfMedia;

    private final SimpleObjectProperty<Duration> currentTime = new SimpleObjectProperty<>(Duration.ZERO);
    private final SimpleObjectProperty<Duration> totalDuration = new SimpleObjectProperty<>(Duration.UNKNOWN);

    public void play(Path path) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        Media media = new Media(path.toUri().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> currentTime.set(newVal));
        mediaPlayer.setOnReady(() -> totalDuration.set(mediaPlayer.getTotalDuration()));
        mediaPlayer.setOnEndOfMedia(() -> {
            if (onEndOfMedia != null) {
                onEndOfMedia.run();
            }
        });

        mediaPlayer.play();
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public void seek(Duration duration) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(duration);
        }
    }

    public void setVolume(double volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    public void setOnEndOfMedia(Runnable onEndOfMedia) {
        this.onEndOfMedia = onEndOfMedia;
    }

    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return currentTime;
    }

    public ReadOnlyObjectProperty<Duration> totalDurationProperty() {
        return totalDuration;
    }
}
