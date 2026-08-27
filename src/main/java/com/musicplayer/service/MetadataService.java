package com.musicplayer.service;

import com.musicplayer.model.Song;
import javafx.scene.image.Image;
import javafx.util.Duration;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

public class MetadataService {

    private static final double COVER_SIZE = 150;

    public Song readSong(Path path) {
        String fallbackTitle = stripExtension(path.getFileName().toString());

        try {
            AudioFile audioFile = AudioFileIO.read(path.toFile());
            Tag tag = audioFile.getTag();

            String title = fallbackTitle;
            String artist = "Unbekannt";
            String album = "Unbekannt";
            Image coverArt = null;

            if (tag != null) {
                title = firstNonBlank(tag.getFirst(FieldKey.TITLE), fallbackTitle);
                artist = firstNonBlank(tag.getFirst(FieldKey.ARTIST), "Unbekannt");
                album = firstNonBlank(tag.getFirst(FieldKey.ALBUM), "Unbekannt");
                coverArt = readCoverArt(tag);
            }

            int lengthSeconds = audioFile.getAudioHeader().getTrackLength();
            Duration duration = lengthSeconds > 0
                    ? Duration.seconds(lengthSeconds)
                    : Duration.UNKNOWN;

            return new Song(path, title, artist, album, duration, coverArt);
        } catch (Exception e) {
            return new Song(path, fallbackTitle, "Unbekannt", "Unbekannt", Duration.UNKNOWN, null);
        }
    }

    private Image readCoverArt(Tag tag) {
        try {
            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null) {
                return null;
            }
            byte[] data = artwork.getBinaryData();
            if (data == null || data.length == 0) {
                return null;
            }
            return new Image(new ByteArrayInputStream(data), COVER_SIZE, COVER_SIZE, false, true);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
