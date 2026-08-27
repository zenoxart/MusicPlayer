package com.musicplayer.controller;

import com.musicplayer.model.Song;
import com.musicplayer.service.AudioPlayer;
import com.musicplayer.service.MetadataService;
import com.musicplayer.service.MusicScanner;
import com.musicplayer.service.RatingStore;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class MainController {

    private static final double CARD_SIZE = 150;
    private static final double SWIPE_THRESHOLD = 60;

    private final Stage stage;
    private final MusicScanner scanner = new MusicScanner();
    private final MetadataService metadataService = new MetadataService();
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final RatingStore ratingStore = new RatingStore();

    private final ObservableList<Song> songs =
            FXCollections.observableArrayList(song -> new javafx.beans.Observable[]{song.ratingProperty()});
    private FilteredList<Song> filteredSongs;
    private SortedList<Song> sortedSongs;

    private final FlowPane cardGrid = new FlowPane();
    private final TextField searchField = new TextField();
    private final ImageView nowPlayingCover = new ImageView();
    private final Label nowPlayingTitleLabel = new Label("Kein Song ausgewählt");
    private final Label nowPlayingArtistLabel = new Label("");
    private final Label timeLabel = new Label("0:00 / 0:00");
    private final Button playPauseButton = new Button("▶");
    private final Slider progressSlider = new Slider();
    private final Slider volumeSlider = new Slider(0, 1, 0.5);

    private boolean seeking = false;
    private Song currentSong;
    private double dragStartX;

    public MainController(Stage stage) {
        this.stage = stage;
    }

    public Parent getRoot() {
        BorderPane root = new BorderPane();
        root.setTop(buildTopBar());
        root.setCenter(buildCardGrid());
        root.setBottom(buildPlayerBar());

        audioPlayer.setVolume(volumeSlider.getValue());
        audioPlayer.setOnEndOfMedia(this::playNext);

        audioPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> updateProgress(newVal));
        audioPlayer.totalDurationProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isUnknown()) {
                progressSlider.setMax(newVal.toSeconds());
            }
        });

        return root;
    }

    private HBox buildTopBar() {
        Button chooseFolderButton = new Button("📁 Ordner wählen");
        chooseFolderButton.getStyleClass().add("folder-button");
        chooseFolderButton.setOnAction(e -> chooseFolder());

        searchField.setPromptText("🔍 Suchen...");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        ComboBox<String> sortCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Keine Sortierung", "Bewertung: hoch → niedrig", "Bewertung: niedrig → hoch"));
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.getStyleClass().add("sort-combo");
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applySorting(newVal));

        HBox topBar = new HBox(10, searchField, sortCombo, chooseFolderButton);
        topBar.getStyleClass().add("top-bar");
        topBar.setPadding(new Insets(16, 24, 16, 24));
        topBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        return topBar;
    }

    private ScrollPane buildCardGrid() {
        cardGrid.getStyleClass().add("card-grid");
        cardGrid.setHgap(20);
        cardGrid.setVgap(24);
        cardGrid.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(cardGrid);
        scrollPane.getStyleClass().add("card-scroll-pane");
        scrollPane.setFitToWidth(true);

        filteredSongs = new FilteredList<>(songs, s -> true);
        sortedSongs = new SortedList<>(filteredSongs);
        sortedSongs.addListener((ListChangeListener<Song>) change -> refreshGrid());
        refreshGrid();

        return scrollPane;
    }

    private void applySorting(String option) {
        if (option == null) {
            sortedSongs.setComparator(null);
            return;
        }
        switch (option) {
            case "Bewertung: hoch → niedrig" ->
                    sortedSongs.setComparator(Comparator.comparingInt(Song::getRating).reversed());
            case "Bewertung: niedrig → hoch" ->
                    sortedSongs.setComparator(Comparator.comparingInt(Song::getRating));
            default -> sortedSongs.setComparator(null);
        }
    }

    private void refreshGrid() {
        if (sortedSongs.isEmpty()) {
            Label placeholder = new Label(songs.isEmpty()
                    ? "Kein Ordner geladen. Bitte einen Musikordner auswählen."
                    : "Keine Treffer.");
            placeholder.getStyleClass().add("grid-placeholder");
            cardGrid.getChildren().setAll(placeholder);
            return;
        }

        List<Node> cards = sortedSongs.stream().<Node>map(this::buildCard).toList();
        cardGrid.getChildren().setAll(cards);
    }

    private VBox buildCard(Song song) {
        Node cover = buildCoverNode(song, CARD_SIZE);

        Label titleLabel = new Label(song.getTitle());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setMaxWidth(CARD_SIZE);

        Label artistLabel = new Label(song.getArtist());
        artistLabel.getStyleClass().add("card-artist");
        artistLabel.setMaxWidth(CARD_SIZE);

        HBox stars = buildStarRating(song);

        Label durationLabel = new Label(song.getDurationFormatted());
        durationLabel.getStyleClass().add("card-duration");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox metaRow = new HBox(stars, spacer, durationLabel);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.setMaxWidth(CARD_SIZE);

        VBox card = new VBox(8, cover, titleLabel, artistLabel, metaRow);
        card.getStyleClass().add("song-card");
        card.setPrefWidth(CARD_SIZE);
        card.setMaxWidth(CARD_SIZE);
        card.setUserData(song);
        if (song == currentSong) {
            card.getStyleClass().add("song-card-playing");
        }
        card.setOnMouseClicked(e -> playSong(song));
        return card;
    }

    private Node buildCoverNode(Song song, double size) {
        StackPane cover = new StackPane();
        cover.getStyleClass().add("song-cover");
        cover.setPrefSize(size, size);
        cover.setMaxSize(size, size);
        cover.setMinSize(size, size);

        Image art = song.getCoverArt();
        if (art != null) {
            ImageView imageView = new ImageView(art);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setPreserveRatio(false);
            Rectangle clip = new Rectangle(size, size);
            clip.setArcWidth(8);
            clip.setArcHeight(8);
            imageView.setClip(clip);
            cover.getChildren().add(imageView);
        } else {
            Label placeholder = new Label("♪");
            placeholder.getStyleClass().add("cover-placeholder-icon");
            cover.getChildren().add(placeholder);
        }
        return cover;
    }

    private HBox buildStarRating(Song song) {
        HBox stars = new HBox(2);
        stars.getStyleClass().add("star-rating");
        Label[] starLabels = new Label[5];

        for (int i = 0; i < 5; i++) {
            int starValue = i + 1;
            Label star = new Label("★");
            star.getStyleClass().add("star");
            starLabels[i] = star;

            star.setOnMouseClicked(e -> {
                int newRating = song.getRating() == starValue ? 0 : starValue;
                song.setRating(newRating);
                ratingStore.setRating(song.getPath(), newRating);
                e.consume();
            });
            star.setOnMouseEntered(e -> updateStars(starLabels, starValue));
            star.setOnMouseExited(e -> updateStars(starLabels, song.getRating()));
            stars.getChildren().add(star);
        }

        updateStars(starLabels, song.getRating());
        return stars;
    }

    private void updateStars(Label[] starLabels, int filledCount) {
        for (int i = 0; i < starLabels.length; i++) {
            starLabels[i].getStyleClass().remove("star-filled");
            if (i < filledCount) {
                starLabels[i].getStyleClass().add("star-filled");
            }
        }
    }

    private VBox buildPlayerBar() {
        Button prevButton = new Button("⏮");
        Button nextButton = new Button("⏭");
        prevButton.getStyleClass().add("control-button");
        nextButton.getStyleClass().add("control-button");
        playPauseButton.getStyleClass().add("play-button");
        prevButton.setOnAction(e -> playPrevious());
        nextButton.setOnAction(e -> playNext());
        playPauseButton.setOnAction(e -> togglePlayPause());

        HBox controls = new HBox(18, prevButton, playPauseButton, nextButton);
        controls.setAlignment(Pos.CENTER);

        progressSlider.setMin(0);
        progressSlider.setValue(0);
        progressSlider.setOnMousePressed(e -> seeking = true);
        progressSlider.setOnMouseReleased(e -> {
            audioPlayer.seek(Duration.seconds(progressSlider.getValue()));
            seeking = false;
        });

        StackPane progressBarStack = buildFilledSlider(progressSlider, "progress-track-bg", "progress-track-fill");
        timeLabel.getStyleClass().add("time-label");

        HBox progressBar = new HBox(10, progressBarStack, timeLabel);
        progressBar.setAlignment(Pos.CENTER);
        HBox.setHgrow(progressBarStack, Priority.ALWAYS);

        Label volumeIcon = new Label("🔊");
        volumeIcon.getStyleClass().add("volume-icon");
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> audioPlayer.setVolume(newVal.doubleValue()));
        StackPane volumeBarStack = buildFilledSlider(volumeSlider, "volume-track-bg", "volume-track-fill");
        volumeBarStack.setPrefWidth(100);
        volumeBarStack.setMaxWidth(100);
        HBox volumeBox = new HBox(8, volumeIcon, volumeBarStack);
        volumeBox.setAlignment(Pos.CENTER_RIGHT);

        HBox bottomRow = new HBox(20, progressBar, volumeBox);
        bottomRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        HBox nowPlayingBox = buildNowPlayingPreview();

        VBox playerBar = new VBox(10, nowPlayingBox, controls, bottomRow);
        playerBar.getStyleClass().add("player-bar");
        playerBar.setPadding(new Insets(14, 24, 18, 24));
        playerBar.setAlignment(Pos.CENTER);
        return playerBar;
    }

    private HBox buildNowPlayingPreview() {
        nowPlayingCover.setFitWidth(44);
        nowPlayingCover.setFitHeight(44);
        Rectangle clip = new Rectangle(44, 44);
        clip.setArcWidth(6);
        clip.setArcHeight(6);
        nowPlayingCover.setClip(clip);

        StackPane coverHolder = new StackPane(nowPlayingCover);
        coverHolder.getStyleClass().add("now-playing-cover");
        coverHolder.setPrefSize(44, 44);
        coverHolder.setMaxSize(44, 44);
        coverHolder.setMinSize(44, 44);

        nowPlayingTitleLabel.getStyleClass().add("now-playing-title");
        nowPlayingArtistLabel.getStyleClass().add("now-playing-artist");
        VBox textBox = new VBox(2, nowPlayingTitleLabel, nowPlayingArtistLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox nowPlayingBox = new HBox(12, coverHolder, textBox);
        nowPlayingBox.getStyleClass().add("now-playing-preview");
        nowPlayingBox.setAlignment(Pos.CENTER_LEFT);
        nowPlayingBox.setMaxWidth(320);

        nowPlayingBox.setOnMousePressed(e -> dragStartX = e.getSceneX());
        nowPlayingBox.setOnMouseDragged(e -> {
            double delta = e.getSceneX() - dragStartX;
            nowPlayingBox.setTranslateX(clamp(delta, -60, 60));
        });
        nowPlayingBox.setOnMouseReleased(e -> {
            double delta = e.getSceneX() - dragStartX;
            nowPlayingBox.setTranslateX(0);
            if (delta <= -SWIPE_THRESHOLD) {
                playNext();
            } else if (delta >= SWIPE_THRESHOLD) {
                playPrevious();
            }
        });

        return nowPlayingBox;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Wraps a Slider in a StackPane with two Regions rendering a colored "played" segment,
     * since JavaFX's default Slider skin has no built-in fill-to-thumb style.
     */
    private StackPane buildFilledSlider(Slider slider, String backgroundStyleClass, String fillStyleClass) {
        Region background = new Region();
        background.getStyleClass().add(backgroundStyleClass);
        background.setPrefHeight(4);
        background.setMaxHeight(4);
        background.setMinHeight(4);
        StackPane.setAlignment(background, Pos.CENTER_LEFT);

        Region fill = new Region();
        fill.getStyleClass().add(fillStyleClass);
        fill.setPrefHeight(4);
        fill.setMaxHeight(4);
        fill.setMinHeight(4);
        // StackPane stretches resizable children to fill its width by default;
        // capping maxWidth to the (bound) prefWidth is what makes the fill stop short.
        fill.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        slider.getStyleClass().add("overlay-slider");
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);

        StackPane stack = new StackPane(background, fill, slider);
        background.prefWidthProperty().bind(stack.widthProperty());
        fill.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
            double range = slider.getMax() - slider.getMin();
            if (range <= 0) {
                return 0.0;
            }
            double ratio = (slider.getValue() - slider.getMin()) / range;
            return Math.max(0, stack.getWidth() * ratio);
        }, slider.valueProperty(), slider.minProperty(), slider.maxProperty(), stack.widthProperty()));

        return stack;
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Musikordner auswählen");
        File selected = chooser.showDialog(stage);
        if (selected == null) {
            return;
        }

        try {
            List<Path> audioFiles = scanner.scan(selected.toPath());
            songs.setAll(audioFiles.stream().map(path -> {
                Song song = metadataService.readSong(path);
                song.setRating(ratingStore.getRating(path));
                return song;
            }).toList());
        } catch (IOException e) {
            nowPlayingTitleLabel.setText("Fehler beim Durchsuchen des Ordners: " + e.getMessage());
        }
    }

    private void applyFilter(String query) {
        String lower = query == null ? "" : query.toLowerCase().trim();
        filteredSongs.setPredicate(song ->
                lower.isEmpty()
                        || song.getTitle().toLowerCase().contains(lower)
                        || song.getArtist().toLowerCase().contains(lower)
                        || song.getAlbum().toLowerCase().contains(lower)
        );
    }

    private void playSong(Song song) {
        currentSong = song;
        audioPlayer.play(song.getPath());
        nowPlayingTitleLabel.setText(song.getTitle());
        nowPlayingArtistLabel.setText(song.getArtist());
        nowPlayingCover.setImage(song.getCoverArt());
        playPauseButton.setText("⏸");
        refreshGrid();
    }

    private void togglePlayPause() {
        if (currentSong == null) {
            if (!sortedSongs.isEmpty()) {
                playSong(sortedSongs.get(0));
            }
            return;
        }

        if (audioPlayer.isPlaying()) {
            audioPlayer.pause();
            playPauseButton.setText("▶");
        } else {
            audioPlayer.resume();
            playPauseButton.setText("⏸");
        }
    }

    private void playNext() {
        shiftSong(1);
    }

    private void playPrevious() {
        shiftSong(-1);
    }

    private void shiftSong(int offset) {
        if (currentSong == null || sortedSongs.isEmpty()) {
            return;
        }
        int currentIndex = sortedSongs.indexOf(currentSong);
        if (currentIndex < 0) {
            return;
        }
        int nextIndex = (currentIndex + offset + sortedSongs.size()) % sortedSongs.size();
        playSong(sortedSongs.get(nextIndex));
    }

    private void updateProgress(Duration current) {
        if (!seeking) {
            progressSlider.setValue(current.toSeconds());
        }
        Duration total = audioPlayer.totalDurationProperty().get();
        String totalStr = (total == null || total.isUnknown()) ? "--:--" : formatDuration(total);
        timeLabel.setText(formatDuration(current) + " / " + totalStr);
    }

    private String formatDuration(Duration duration) {
        int totalSeconds = (int) duration.toSeconds();
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
