package com.musicplayer.controller;

import com.musicplayer.model.Language;
import com.musicplayer.model.Playlist;
import com.musicplayer.model.Song;
import com.musicplayer.model.SortDirection;
import com.musicplayer.model.SortKey;
import com.musicplayer.service.AudioPlayer;
import com.musicplayer.service.LanguageStore;
import com.musicplayer.service.Messages;
import com.musicplayer.service.MetadataService;
import com.musicplayer.service.MusicScanner;
import com.musicplayer.service.PlaylistStore;
import com.musicplayer.service.RatingStore;
import com.musicplayer.service.SettingsStore;
import com.musicplayer.service.ThemeStore;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MainController {

    private static final double DEFAULT_CENTER_CARD_SIZE = 220;
    private static final double MIN_CENTER_CARD_SIZE = 160;
    private static final double MAX_CENTER_CARD_SIZE = 320;
    private static final double SIDE_CARD_RATIO = 150.0 / 220.0;
    private static final double CAROUSEL_ARROW_SIZE = 44;
    private static final double CAROUSEL_SPACING = 24;
    private static final double SWIPE_THRESHOLD = 60;

    private final Stage stage;
    private final MusicScanner scanner = new MusicScanner();
    private final MetadataService metadataService = new MetadataService();
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final RatingStore ratingStore = new RatingStore();
    private final PlaylistStore playlistStore = new PlaylistStore();
    private final SettingsStore settingsStore = new SettingsStore();
    private final ThemeStore themeStore = new ThemeStore();
    private final LanguageStore languageStore = new LanguageStore();

    private final ObservableList<Song> songs =
            FXCollections.observableArrayList(song -> new javafx.beans.Observable[]{song.ratingProperty()});
    private FilteredList<Song> filteredSongs;
    private SortedList<Song> sortedSongs;

    private final HBox cardsRow = new HBox(24);
    private final Button leftArrowButton = new Button("◀");
    private final Button rightArrowButton = new Button("▶");
    private final TextField searchField = new TextField();
    private final Button filterButton = new Button();
    private final Button shuffleButton = new Button("🔀");
    private final VBox sidebar = new VBox();
    private final ImageView nowPlayingCover = new ImageView();
    private final Label nowPlayingTitleLabel = new Label();
    private final Label nowPlayingArtistLabel = new Label("");
    private final Label timeLabel = new Label("0:00 / 0:00");
    private final Button playPauseButton = new Button("▶");
    private final Slider progressSlider = new Slider();
    private final Slider volumeSlider = new Slider(0, 1, 0.5);

    private boolean seeking = false;
    private boolean shuffleEnabled = false;
    private boolean chooseFolderHighlightPlayed = false;
    private final List<Song> shuffleOrder = new ArrayList<>();
    private Song currentSong;
    private Song draggedSong;
    private Playlist currentPlaylist;
    private File currentFolder;
    private double dragStartX;
    private double carouselDragStartX;
    private int focusIndex = 0;
    private double centerCardSize = DEFAULT_CENTER_CARD_SIZE;
    private double sideCardSize = DEFAULT_CENTER_CARD_SIZE * SIDE_CARD_RATIO;
    private Toast toast;
    private StackPane rootOverlay;

    private Map<SortKey, SortDirection> activeSortCriteria = new LinkedHashMap<>();
    private Set<String> formatFilter = new LinkedHashSet<>(SettingsStore.SUPPORTED_EXTENSIONS);

    public MainController(Stage stage) {
        this.stage = stage;
    }

    public Parent getRoot() {
        Messages.setLanguage(languageStore.getLanguage());

        if (!languageStore.isConfigured()) {
            Language selected = LanguageDialog.showAndWait(stage, languageStore.getLanguage(), themeStore, false)
                    .orElse(Language.ENGLISH);
            languageStore.save(selected);
            Messages.setLanguage(selected);
        }

        if (!themeStore.isConfigured()) {
            ThemeDialog.Selection selection = ThemeDialog.showAndWait(stage, themeStore, false)
                    .orElse(new ThemeDialog.Selection(true, ThemeStore.DEFAULT_ACCENT));
            themeStore.save(selection.darkMode(), selection.accentColor());
        }

        nowPlayingTitleLabel.setText(Messages.get("nowPlaying.none"));

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(buildTopBar());
        mainLayout.setLeft(buildSidebar());
        mainLayout.setCenter(buildCardGrid());
        mainLayout.setBottom(buildPlayerBar());

        audioPlayer.setVolume(volumeSlider.getValue());
        audioPlayer.setOnEndOfMedia(this::playNext);

        audioPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> updateProgress(newVal));
        audioPlayer.totalDurationProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isUnknown()) {
                progressSlider.setMax(newVal.toSeconds());
            }
        });

        StackPane overlay = new StackPane(mainLayout);
        toast = new Toast(overlay);
        rootOverlay = overlay;
        applyCurrentTheme();
        return overlay;
    }

    private void applyCurrentTheme() {
        if (rootOverlay != null) {
            ThemeSupport.apply(rootOverlay, themeStore);
        }
    }

    /**
     * Draws attention to the folder-picker empty-state CTA on first launch (the app
     * never starts with a folder already loaded), then settles back to a plain button.
     * Guarded to play only once, even if the empty state gets rebuilt again while
     * still empty (e.g. a listener firing before the user has acted).
     */
    private void playChooseFolderHighlight(Button target) {
        if (chooseFolderHighlightPlayed) {
            return;
        }
        chooseFolderHighlightPlayed = true;

        DropShadow highlight = new DropShadow();
        highlight.setColor(Color.web("#1DB954"));
        highlight.setSpread(0.4);
        target.setEffect(highlight);

        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(highlight.radiusProperty(), 4)),
                new KeyFrame(Duration.seconds(0.7), new KeyValue(highlight.radiusProperty(), 22)),
                new KeyFrame(Duration.seconds(1.4), new KeyValue(highlight.radiusProperty(), 4))
        );
        pulse.setCycleCount(3);
        pulse.setOnFinished(e -> target.setEffect(null));
        pulse.play();
    }

    private HBox buildTopBar() {
        Button settingsButton = new Button("⚙");
        settingsButton.getStyleClass().add("settings-button");
        settingsButton.setOnAction(e -> openSettings());

        searchField.setPromptText("🔍 " + Messages.get("search.prompt"));
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        filterButton.getStyleClass().add("filter-button");
        filterButton.setOnAction(e -> openFilterSortDialog());
        updateFilterButtonLabel();

        HBox topBar = new HBox(10, searchField, filterButton, settingsButton);
        topBar.getStyleClass().add("top-bar");
        topBar.setPadding(new Insets(16, 24, 16, 24));
        topBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        return topBar;
    }

    private void openFilterSortDialog() {
        FilterSortDialog.showAndWait(stage, activeSortCriteria, formatFilter, themeStore).ifPresent(selection -> {
            activeSortCriteria = selection.sortCriteria();
            formatFilter = selection.formats();
            updateFilterButtonLabel();
            applyFilter(searchField.getText());
            refreshSortComparator();
        });
    }

    private void updateFilterButtonLabel() {
        filterButton.setText("▼");

        StringBuilder sortPart = new StringBuilder();
        for (SortKey key : SortKey.values()) {
            SortDirection direction = activeSortCriteria.get(key);
            if (direction != null) {
                if (sortPart.length() > 0) {
                    sortPart.append(", ");
                }
                sortPart.append(Messages.get(key.getMessageKey())).append(" ").append(direction.getSymbol());
            }
        }
        String tooltipText = sortPart.length() == 0
                ? Messages.get("filter.tooltip.default")
                : Messages.get("filter.tooltip.active", sortPart.toString());
        filterButton.setTooltip(new Tooltip(tooltipText));

        boolean active = !activeSortCriteria.isEmpty() || formatFilter.size() < SettingsStore.SUPPORTED_EXTENSIONS.size();
        filterButton.getStyleClass().remove("filter-button-active");
        if (active) {
            filterButton.getStyleClass().add("filter-button-active");
        }
    }

    private VBox buildSidebar() {
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        refreshSidebar();
        return sidebar;
    }

    private void refreshSidebar() {
        List<Node> rows = new ArrayList<>();

        rows.add(buildSidebarRow("🎵  " + Messages.get("sidebar.library"), currentPlaylist == null,
                () -> selectPlaylist(null)));

        Label sectionLabel = new Label(Messages.get("sidebar.playlistsHeader"));
        sectionLabel.getStyleClass().add("sidebar-section-label");
        rows.add(sectionLabel);

        for (Playlist playlist : playlistStore.getPlaylists()) {
            boolean active = currentPlaylist != null && currentPlaylist.getId().equals(playlist.getId());
            rows.add(buildSidebarPlaylistRow(playlist, active));
        }

        Label newPlaylistRow = buildSidebarRow("➕  " + Messages.get("sidebar.newPlaylist"), false,
                this::createPlaylistViaPrompt);
        newPlaylistRow.getStyleClass().add("sidebar-new-playlist");
        rows.add(newPlaylistRow);

        sidebar.getChildren().setAll(rows);
    }

    private Label buildSidebarRow(String text, boolean active, Runnable onClick) {
        Label row = new Label(text);
        row.getStyleClass().add("sidebar-row");
        if (active) {
            row.getStyleClass().add("sidebar-row-active");
        }
        row.setMaxWidth(Double.MAX_VALUE);
        row.setOnMouseClicked(e -> onClick.run());
        return row;
    }

    private Node buildSidebarPlaylistRow(Playlist playlist, boolean active) {
        Label text = new Label("🎧  " + Messages.get("sidebar.playlistCount", playlist.getName(),
                playlist.getSongPaths().size()));
        text.getStyleClass().add("sidebar-row-text");
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMaxWidth(Double.MAX_VALUE);

        Button menuButton = new Button("⋮");
        menuButton.getStyleClass().add("sidebar-menu-button");
        menuButton.setOnMouseClicked(e -> {
            showPlaylistMenu(playlist, menuButton);
            e.consume();
        });

        HBox row = new HBox(text, menuButton);
        row.getStyleClass().add("sidebar-row");
        if (active) {
            row.getStyleClass().add("sidebar-row-active");
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setOnMouseClicked(e -> selectPlaylist(playlist));
        row.setOnContextMenuRequested(e ->
                showPlaylistMenu(playlist, row, e.getScreenX(), e.getScreenY()));

        row.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        row.setOnDragEntered(e -> {
            if (e.getDragboard().hasString()) {
                row.getStyleClass().add("sidebar-row-drop-target");
            }
        });
        row.setOnDragExited(e -> row.getStyleClass().remove("sidebar-row-drop-target"));
        row.setOnDragDropped(e -> {
            boolean success = false;
            if (draggedSong != null) {
                playlistStore.addSongToPlaylist(playlist.getId(), draggedSong.getPath());
                refreshSidebar();
                toast.show(Messages.get("toast.addedToPlaylist", playlist.getName()));
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });

        return row;
    }

    private void showPlaylistMenu(Playlist playlist, Node anchor) {
        buildPlaylistMenu(playlist).show(anchor, Side.BOTTOM, 0, 4);
    }

    private void showPlaylistMenu(Playlist playlist, Node anchor, double screenX, double screenY) {
        buildPlaylistMenu(playlist).show(anchor, screenX, screenY);
    }

    private ContextMenu buildPlaylistMenu(Playlist playlist) {
        ContextMenu menu = new ContextMenu();
        ThemeSupport.apply(menu, themeStore);
        MenuItem delete = new MenuItem(Messages.get("menu.deletePlaylist"));
        delete.setOnAction(ev -> deletePlaylist(playlist));
        menu.getItems().add(delete);
        return menu;
    }

    private void deletePlaylist(Playlist playlist) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(Messages.get("dialog.deletePlaylist.title"));
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().addAll("app-dialog", "delete-dialog");
        ThemeSupport.apply(dialog.getDialogPane(), themeStore);

        Label warningIcon = new Label("⚠");
        warningIcon.getStyleClass().add("delete-warning-icon");
        StackPane warningBadge = new StackPane(warningIcon);
        warningBadge.getStyleClass().add("delete-warning-badge");

        Label headline = new Label(Messages.get("dialog.deletePlaylist.headline"));
        headline.getStyleClass().add("delete-headline");

        Label description = new Label(Messages.get("dialog.deletePlaylist.description", playlist.getName()));
        description.getStyleClass().add("delete-description");
        description.setWrapText(true);
        description.setMaxWidth(300);
        description.setTextAlignment(TextAlignment.CENTER);

        List<Path> songPaths = playlist.getSongPaths();
        VBox riskBox = new VBox(6);
        riskBox.getStyleClass().add("delete-risk-box");
        Label riskLabel = new Label(Messages.get("dialog.deletePlaylist.risk.label"));
        riskLabel.getStyleClass().add("delete-risk-label");
        riskBox.getChildren().add(riskLabel);
        int previewCount = Math.min(2, songPaths.size());
        for (int i = 0; i < previewCount; i++) {
            Label item = new Label(songPaths.get(i).getFileName().toString());
            item.getStyleClass().add("delete-risk-item");
            riskBox.getChildren().add(item);
        }
        if (songPaths.size() > previewCount) {
            Label more = new Label(Messages.get("dialog.deletePlaylist.risk.more", songPaths.size() - previewCount));
            more.getStyleClass().add("delete-risk-item");
            riskBox.getChildren().add(more);
        }
        String totalKey = songPaths.size() == 1
                ? "dialog.deletePlaylist.risk.total.one"
                : "dialog.deletePlaylist.risk.total.other";
        Label total = new Label(Messages.get(totalKey, songPaths.size()));
        total.getStyleClass().add("delete-risk-total");
        riskBox.getChildren().add(total);

        Button protectButton = new Button(Messages.get("dialog.deletePlaylist.protect"));
        protectButton.getStyleClass().add("delete-protect-button");
        protectButton.setMaxWidth(Double.MAX_VALUE);
        protectButton.setOnAction(e -> {
            dialog.setResult(false);
            dialog.close();
        });

        Label riskItLink = new Label(Messages.get("dialog.deletePlaylist.riskIt"));
        riskItLink.getStyleClass().add("delete-risk-it-link");
        riskItLink.setOnMouseClicked(e -> {
            dialog.setResult(true);
            dialog.close();
        });

        VBox content = new VBox(14, warningBadge, headline, description, riskBox, protectButton, riskItLink);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24, 28, 20, 28));
        dialog.getDialogPane().setContent(content);

        boolean confirmedDelete = dialog.showAndWait().orElse(false);
        if (confirmedDelete) {
            boolean wasActive = currentPlaylist != null && currentPlaylist.getId().equals(playlist.getId());
            playlistStore.deletePlaylist(playlist.getId());
            if (wasActive) {
                currentPlaylist = null;
                applyFilter(searchField.getText());
                refreshSortComparator();
            }
            refreshSidebar();
            toast.show(Messages.get("toast.playlistDeleted"));
        }
    }

    private Optional<String> promptPlaylistName() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(stage);
        dialog.setTitle(Messages.get("dialog.newPlaylist.title"));
        dialog.setHeaderText(null);
        dialog.setContentText(Messages.get("dialog.newPlaylist.name"));
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-dialog");
        ThemeSupport.apply(dialog.getDialogPane(), themeStore);
        return dialog.showAndWait().map(String::trim).filter(name -> !name.isEmpty());
    }

    private void createPlaylistViaPrompt() {
        promptPlaylistName().ifPresent(name -> {
            playlistStore.createPlaylist(name);
            refreshSidebar();
            toast.show(Messages.get("toast.playlistCreated", name));
        });
    }

    private void createPlaylistAndAddSong(Song song) {
        promptPlaylistName().ifPresent(name -> {
            Playlist playlist = playlistStore.createPlaylist(name);
            playlistStore.addSongToPlaylist(playlist.getId(), song.getPath());
            refreshSidebar();
            toast.show(Messages.get("toast.playlistCreatedWithSong", name));
        });
    }

    private void selectPlaylist(Playlist playlist) {
        this.currentPlaylist = playlist;
        if (playlist != null) {
            ensurePlaylistSongsLoaded(playlist);
        }
        refreshSidebar();
        applyFilter(searchField.getText());
        refreshSortComparator();
    }

    /**
     * Playlist songs are stored by absolute path and may belong to a folder the user
     * hasn't (re)scanned this session — load any that are missing directly, so opening
     * a playlist never requires picking its folder first.
     */
    private void ensurePlaylistSongsLoaded(Playlist playlist) {
        List<Song> newSongs = new ArrayList<>();
        for (Path path : playlist.getSongPaths()) {
            Path absolute = path.toAbsolutePath();
            boolean alreadyLoaded = songs.stream()
                    .anyMatch(song -> song.getPath().toAbsolutePath().equals(absolute));
            if (!alreadyLoaded && Files.exists(absolute)) {
                Song song = metadataService.readSong(absolute);
                song.setRating(ratingStore.getRating(absolute));
                newSongs.add(song);
            }
        }
        if (!newSongs.isEmpty()) {
            songs.addAll(newSongs);
        }
    }

    private ScrollPane buildCardGrid() {
        cardsRow.getStyleClass().add("carousel-cards");
        cardsRow.setAlignment(Pos.CENTER);
        // Without this, HBox stretches the card VBoxes to fill the row's full height
        // (e.g. in a maximized window) instead of sizing them to their own content.
        cardsRow.setFillHeight(false);
        cardsRow.setCursor(javafx.scene.Cursor.HAND);

        // Swipe-to-navigate: same press/drag/release + clamp-preview pattern as the
        // now-playing preview below, but shifting carousel focus instead of the track.
        cardsRow.setOnMousePressed(e -> carouselDragStartX = e.getSceneX());
        cardsRow.setOnMouseDragged(e -> {
            double delta = e.getSceneX() - carouselDragStartX;
            cardsRow.setTranslateX(clamp(delta, -80, 80));
        });
        cardsRow.setOnMouseReleased(e -> {
            double delta = e.getSceneX() - carouselDragStartX;
            cardsRow.setTranslateX(0);
            if (delta <= -SWIPE_THRESHOLD) {
                shiftFocus(1);
            } else if (delta >= SWIPE_THRESHOLD) {
                shiftFocus(-1);
            }
        });

        leftArrowButton.getStyleClass().add("carousel-arrow");
        leftArrowButton.setOnAction(e -> shiftFocus(-1));
        rightArrowButton.getStyleClass().add("carousel-arrow");
        rightArrowButton.setOnAction(e -> shiftFocus(1));

        HBox carousel = new HBox(CAROUSEL_SPACING, leftArrowButton, cardsRow, rightArrowButton);
        carousel.getStyleClass().add("card-grid");
        carousel.setAlignment(Pos.CENTER);
        carousel.setFillHeight(false);

        ScrollPane scrollPane = new ScrollPane(carousel);
        scrollPane.getStyleClass().add("card-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Card size scales with the available width (e.g. maximizing the window),
        // instead of staying fixed while only the surrounding whitespace grows.
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (newBounds != null) {
                updateCardSizes(newBounds.getWidth());
            }
        });

        filteredSongs = new FilteredList<>(songs, s -> true);
        sortedSongs = new SortedList<>(filteredSongs);
        sortedSongs.addListener((ListChangeListener<Song>) change -> {
            if (shuffleEnabled) {
                regenerateShuffleOrder();
            }
            refreshGrid();
        });
        refreshGrid();

        return scrollPane;
    }

    private void updateCardSizes(double viewportWidth) {
        double reserved = 2 * CAROUSEL_ARROW_SIZE + 4 * CAROUSEL_SPACING + 40;
        double available = Math.max(0, viewportWidth - reserved);
        double newCenter = (available - 2 * CAROUSEL_SPACING) / (1 + 2 * SIDE_CARD_RATIO);
        newCenter = Math.max(MIN_CENTER_CARD_SIZE, Math.min(MAX_CENTER_CARD_SIZE, newCenter));
        if (Math.abs(newCenter - centerCardSize) > 2) {
            centerCardSize = newCenter;
            sideCardSize = newCenter * SIDE_CARD_RATIO;
            refreshGrid();
        }
    }

    private void shiftFocus(int delta) {
        focusIndex = clampFocusIndex(focusIndex + delta);
        refreshGrid();
    }

    private int clampFocusIndex(int index) {
        if (sortedSongs.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(index, sortedSongs.size() - 1));
    }

    private void refreshSortComparator() {
        sortedSongs.setComparator(buildComparator());
    }

    private Comparator<Song> buildComparator() {
        Comparator<Song> comparator = null;
        for (SortKey key : SortKey.values()) {
            SortDirection direction = activeSortCriteria.get(key);
            if (direction == null) {
                continue;
            }
            Comparator<Song> keyComparator = comparatorFor(key, direction);
            comparator = comparator == null ? keyComparator : comparator.thenComparing(keyComparator);
        }
        if (comparator != null) {
            return comparator;
        }
        if (currentPlaylist != null) {
            List<Path> order = currentPlaylist.getSongPaths();
            return Comparator.comparingInt(song -> {
                int index = indexOfPath(order, song.getPath());
                return index < 0 ? Integer.MAX_VALUE : index;
            });
        }
        return null;
    }

    private Comparator<Song> comparatorFor(SortKey key, SortDirection direction) {
        Comparator<Song> comparator = switch (key) {
            case RATING -> Comparator.comparingInt(Song::getRating);
            case ARTIST -> Comparator.comparing(song ->
                    song.getArtist() == null ? "" : song.getArtist().toLowerCase());
            case LENGTH -> Comparator.comparingDouble(song ->
                    song.getDuration() == null || song.getDuration().isUnknown() ? 0 : song.getDuration().toSeconds());
        };
        return direction == SortDirection.DESC ? comparator.reversed() : comparator;
    }

    private int indexOfPath(List<Path> paths, Path target) {
        Path absolute = target.toAbsolutePath();
        for (int i = 0; i < paths.size(); i++) {
            if (paths.get(i).toAbsolutePath().equals(absolute)) {
                return i;
            }
        }
        return -1;
    }

    private void refreshGrid() {
        if (sortedSongs.isEmpty()) {
            cardsRow.getChildren().setAll(buildEmptyState());
            leftArrowButton.setDisable(true);
            rightArrowButton.setDisable(true);
            return;
        }

        focusIndex = clampFocusIndex(focusIndex);

        List<Node> cards = new ArrayList<>();
        if (focusIndex > 0) {
            cards.add(buildCard(sortedSongs.get(focusIndex - 1), sideCardSize, false));
        }
        cards.add(buildCard(sortedSongs.get(focusIndex), centerCardSize, true));
        if (focusIndex < sortedSongs.size() - 1) {
            cards.add(buildCard(sortedSongs.get(focusIndex + 1), sideCardSize, false));
        }
        cardsRow.getChildren().setAll(cards);

        leftArrowButton.setDisable(focusIndex <= 0);
        rightArrowButton.setDisable(focusIndex >= sortedSongs.size() - 1);
    }

    private Node buildEmptyState() {
        VBox emptyState = new VBox(12);
        emptyState.getStyleClass().add("empty-state");
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(60, 20, 0, 20));

        Label icon = new Label("♪");
        icon.getStyleClass().add("empty-state-icon");

        String message;
        if (songs.isEmpty()) {
            message = Messages.get("emptyState.noFolder");
        } else if (currentPlaylist != null) {
            message = Messages.get("emptyState.emptyPlaylist");
        } else {
            message = Messages.get("emptyState.noResults");
        }
        Label text = new Label(message);
        text.getStyleClass().add("grid-placeholder");
        text.setWrapText(true);
        text.setMaxWidth(320);
        text.setTextAlignment(TextAlignment.CENTER);

        emptyState.getChildren().addAll(icon, text);

        if (songs.isEmpty()) {
            Button chooseButton = new Button("📁 " + Messages.get("button.chooseFolder"));
            chooseButton.getStyleClass().addAll("folder-button", "empty-state-cta");
            chooseButton.setOnAction(e -> chooseFolder());
            emptyState.getChildren().add(chooseButton);
            Platform.runLater(() -> playChooseFolderHighlight(chooseButton));
        }

        return emptyState;
    }

    private VBox buildCard(Song song, double size, boolean focused) {
        Node cover = buildCoverNode(song, size);

        Label titleLabel = new Label(song.getTitle());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setMaxWidth(size);

        Label artistLabel = new Label(song.getArtist());
        artistLabel.getStyleClass().add("card-artist");
        artistLabel.setMaxWidth(size);

        HBox stars = buildStarRating(song);

        Label durationLabel = new Label(song.getDurationFormatted());
        durationLabel.getStyleClass().add("card-duration");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox metaRow = new HBox(stars, spacer, durationLabel);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.setMaxWidth(size);

        VBox card = new VBox(8, cover, titleLabel, artistLabel, metaRow);
        card.getStyleClass().add("song-card");
        card.getStyleClass().add(focused ? "carousel-card-focused" : "carousel-card-side");
        card.setPrefWidth(size);
        card.setMaxWidth(size);
        card.setUserData(song);
        if (song == currentSong) {
            card.getStyleClass().add("song-card-playing");
        }
        card.setOnMouseClicked(e -> playSong(song));
        card.setOnContextMenuRequested(e ->
                buildSongContextMenu(song).show(card, e.getScreenX(), e.getScreenY()));

        card.setOnDragDetected(e -> {
            draggedSong = song;
            Dragboard db = card.startDragAndDrop(TransferMode.COPY_OR_MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(song.getPath().toString());
            db.setContent(content);
            card.getStyleClass().add("song-card-dragging");
            e.consume();
        });
        card.setOnDragOver(e -> {
            if (e.getGestureSource() != card && e.getDragboard().hasString()
                    && currentPlaylist != null && activeSortCriteria.isEmpty()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        card.setOnDragEntered(e -> {
            if (currentPlaylist != null && activeSortCriteria.isEmpty()
                    && draggedSong != null && draggedSong != song) {
                card.getStyleClass().add("song-card-drop-target");
            }
        });
        card.setOnDragExited(e -> card.getStyleClass().remove("song-card-drop-target"));
        card.setOnDragDropped(e -> {
            boolean success = false;
            if (currentPlaylist != null && activeSortCriteria.isEmpty()
                    && draggedSong != null && draggedSong != song) {
                reorderPlaylistSong(draggedSong, song);
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });
        card.setOnDragDone(e -> {
            card.getStyleClass().remove("song-card-dragging");
            draggedSong = null;
        });

        return card;
    }

    private void reorderPlaylistSong(Song dragged, Song target) {
        if (currentPlaylist == null) {
            return;
        }
        List<Path> newOrder = new ArrayList<>(currentPlaylist.getSongPaths());
        Path draggedPath = dragged.getPath().toAbsolutePath();
        newOrder.removeIf(p -> p.toAbsolutePath().equals(draggedPath));
        int targetIndex = indexOfPath(newOrder, target.getPath());
        if (targetIndex < 0) {
            targetIndex = newOrder.size();
        }
        newOrder.add(targetIndex, draggedPath);
        playlistStore.reorderPlaylist(currentPlaylist.getId(), newOrder);
        refreshSortComparator();
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

        if (song == currentSong) {
            Label badge = new Label("▶");
            badge.getStyleClass().add("now-playing-badge");
            StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);
            cover.getChildren().add(badge);
        }

        Button menuButton = new Button("⋮");
        menuButton.getStyleClass().add("card-menu-button");
        StackPane.setAlignment(menuButton, Pos.TOP_RIGHT);
        menuButton.setOnMouseClicked(e -> {
            buildSongContextMenu(song).show(menuButton, Side.BOTTOM, 0, 4);
            e.consume();
        });
        cover.getChildren().add(menuButton);

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

    private ContextMenu buildSongContextMenu(Song song) {
        ContextMenu menu = new ContextMenu();
        ThemeSupport.apply(menu, themeStore);

        Menu addToPlaylist = new Menu(Messages.get("menu.addToPlaylist"));
        List<Playlist> playlists = playlistStore.getPlaylists();
        if (playlists.isEmpty()) {
            MenuItem none = new MenuItem(Messages.get("menu.noPlaylists"));
            none.setDisable(true);
            addToPlaylist.getItems().add(none);
        } else {
            for (Playlist playlist : playlists) {
                MenuItem item = new MenuItem(playlist.getName());
                item.setOnAction(e -> {
                    playlistStore.addSongToPlaylist(playlist.getId(), song.getPath());
                    refreshSidebar();
                    if (currentPlaylist != null && currentPlaylist.getId().equals(playlist.getId())) {
                        applyFilter(searchField.getText());
                    }
                    toast.show(Messages.get("toast.addedToPlaylist", playlist.getName()));
                });
                addToPlaylist.getItems().add(item);
            }
        }
        addToPlaylist.getItems().add(new SeparatorMenuItem());
        MenuItem newPlaylistItem = new MenuItem(Messages.get("menu.newPlaylistEllipsis"));
        newPlaylistItem.setOnAction(e -> createPlaylistAndAddSong(song));
        addToPlaylist.getItems().add(newPlaylistItem);

        menu.getItems().add(addToPlaylist);

        if (currentPlaylist != null) {
            MenuItem removeItem = new MenuItem(Messages.get("menu.removeFromPlaylist"));
            removeItem.setOnAction(e -> {
                playlistStore.removeSongFromPlaylist(currentPlaylist.getId(), song.getPath());
                refreshSidebar();
                applyFilter(searchField.getText());
                toast.show(Messages.get("toast.removedFromPlaylist"));
            });
            menu.getItems().add(removeItem);
        }

        return menu;
    }

    private VBox buildPlayerBar() {
        Button prevButton = new Button("⏮");
        Button nextButton = new Button("⏭");
        prevButton.getStyleClass().add("control-button");
        nextButton.getStyleClass().add("control-button");
        shuffleButton.getStyleClass().add("control-button");
        playPauseButton.getStyleClass().add("play-button");
        prevButton.setOnAction(e -> playPrevious());
        nextButton.setOnAction(e -> playNext());
        playPauseButton.setOnAction(e -> togglePlayPause());
        shuffleButton.setOnAction(e -> toggleShuffle());

        HBox controls = new HBox(18, shuffleButton, prevButton, playPauseButton, nextButton);
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

    private void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;
        shuffleButton.getStyleClass().remove("control-button-active");
        if (shuffleEnabled) {
            shuffleButton.getStyleClass().add("control-button-active");
            regenerateShuffleOrder();
            toast.show(Messages.get("toast.shuffleOn"));
        } else {
            toast.show(Messages.get("toast.shuffleOff"));
        }
    }

    private void regenerateShuffleOrder() {
        shuffleOrder.clear();
        shuffleOrder.addAll(sortedSongs);
        Collections.shuffle(shuffleOrder);
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
        chooser.setTitle(Messages.get("chooser.title"));
        File selected = chooser.showDialog(stage);
        if (selected == null) {
            return;
        }
        currentFolder = selected;
        scanFolder(selected);
    }

    private void rescanCurrentFolder() {
        if (currentFolder != null) {
            scanFolder(currentFolder);
        }
    }

    private void scanFolder(File folder) {
        try {
            List<Path> audioFiles = scanner.scan(folder.toPath(), settingsStore.getExtensions());
            songs.setAll(audioFiles.stream().map(path -> {
                Song song = metadataService.readSong(path);
                song.setRating(ratingStore.getRating(path));
                return song;
            }).toList());
        } catch (IOException e) {
            nowPlayingTitleLabel.setText(Messages.get("error.scanFailed", e.getMessage()));
        }
    }

    private void openSettings() {
        SettingsDialog.showAndWait(stage, settingsStore.getExtensions(), themeStore, this::applyCurrentTheme,
                        languageStore, this::applyLanguageChange)
                .ifPresent(selected -> {
                    settingsStore.setExtensions(selected);
                    if (currentFolder != null) {
                        rescanCurrentFolder();
                        toast.show(Messages.get("toast.folderRescanned"));
                    } else {
                        toast.show(Messages.get("toast.settingsSaved"));
                    }
                });
    }

    private void applyLanguageChange() {
        Messages.setLanguage(languageStore.getLanguage());
        searchField.setPromptText("🔍 " + Messages.get("search.prompt"));
        if (currentSong == null) {
            nowPlayingTitleLabel.setText(Messages.get("nowPlaying.none"));
        }
        updateFilterButtonLabel();
        refreshSidebar();
        refreshGrid();
    }

    private void applyFilter(String query) {
        String lower = query == null ? "" : query.toLowerCase().trim();
        filteredSongs.setPredicate(song ->
                (currentPlaylist == null || currentPlaylist.containsSong(song.getPath()))
                        && formatFilter.contains(extensionOf(song.getPath()))
                        && (lower.isEmpty()
                        || song.getTitle().toLowerCase().contains(lower)
                        || song.getArtist().toLowerCase().contains(lower)
                        || song.getAlbum().toLowerCase().contains(lower))
        );
    }

    private String extensionOf(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private void playSong(Song song) {
        currentSong = song;
        int index = sortedSongs.indexOf(song);
        if (index >= 0) {
            focusIndex = index;
        }
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
        List<Song> order = shuffleEnabled ? shuffleOrder : sortedSongs;
        if (order.isEmpty()) {
            return;
        }
        int currentIndex = order.indexOf(currentSong);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        int nextIndex = (currentIndex + offset + order.size()) % order.size();
        playSong(order.get(nextIndex));
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
