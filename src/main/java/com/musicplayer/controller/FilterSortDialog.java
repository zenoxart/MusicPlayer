package com.musicplayer.controller;

import com.musicplayer.model.SortDirection;
import com.musicplayer.model.SortKey;
import com.musicplayer.service.Messages;
import com.musicplayer.service.SettingsStore;
import com.musicplayer.service.ThemeStore;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class FilterSortDialog {

    public record Selection(Map<SortKey, SortDirection> sortCriteria, Set<String> formats) {
    }

    private static final Map<String, String> EXTENSION_LABELS = Map.of(
            ".wav", "WAV",
            ".mp3", "MP3",
            ".m4a", "M4A",
            ".aac", "AAC"
    );

    private FilterSortDialog() {
    }

    public static Optional<Selection> showAndWait(Stage owner, Map<SortKey, SortDirection> currentCriteria,
                                                    Set<String> currentFormats, ThemeStore themeStore) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(Messages.get("filterSort.title"));
        dialog.getDialogPane().getStylesheets().add(
                FilterSortDialog.class.getResource("/css/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-dialog");
        ThemeSupport.apply(dialog.getDialogPane(), themeStore);

        Label sortHeading = new Label(Messages.get("filterSort.sortHeading"));
        sortHeading.getStyleClass().add("dialog-heading");

        Map<SortKey, SortDirection> selectedCriteria = new LinkedHashMap<>(currentCriteria);
        Map<SortKey, Button> ascButtons = new LinkedHashMap<>();
        Map<SortKey, Button> descButtons = new LinkedHashMap<>();
        Runnable[] refreshSelection = new Runnable[1];

        VBox criteriaColumn = new VBox(14);
        for (SortKey key : SortKey.values()) {
            Label label = new Label(Messages.get(key.getMessageKey()));
            label.getStyleClass().add("sort-criterion-label");
            label.setPrefWidth(90);

            Button ascButton = new Button("▲");
            ascButton.getStyleClass().addAll("sort-cell", "sort-cell-left");
            Button descButton = new Button("▼");
            descButton.getStyleClass().addAll("sort-cell", "sort-cell-right");
            ascButtons.put(key, ascButton);
            descButtons.put(key, descButton);

            ascButton.setOnAction(e -> {
                toggleSelection(selectedCriteria, key, SortDirection.ASC);
                refreshSelection[0].run();
            });
            descButton.setOnAction(e -> {
                toggleSelection(selectedCriteria, key, SortDirection.DESC);
                refreshSelection[0].run();
            });

            HBox cellRow = new HBox(ascButton, descButton);
            cellRow.getStyleClass().add("sort-cell-row");

            HBox criterionRow = new HBox(16, label, cellRow);
            criterionRow.setAlignment(Pos.CENTER_LEFT);
            criteriaColumn.getChildren().add(criterionRow);
        }

        refreshSelection[0] = () -> {
            ascButtons.forEach((key, button) -> setActive(button, selectedCriteria.get(key) == SortDirection.ASC));
            descButtons.forEach((key, button) -> setActive(button, selectedCriteria.get(key) == SortDirection.DESC));
        };
        refreshSelection[0].run();

        Label formatHeading = new Label(Messages.get("filterSort.formatsHeading"));
        formatHeading.getStyleClass().add("dialog-heading");

        Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();
        HBox formatRow = new HBox(16);
        for (String extension : SettingsStore.SUPPORTED_EXTENSIONS) {
            CheckBox checkBox = new CheckBox(EXTENSION_LABELS.getOrDefault(extension, extension.toUpperCase()));
            checkBox.setSelected(currentFormats.contains(extension));
            checkBox.getStyleClass().add("settings-checkbox");
            checkBoxes.put(extension, checkBox);
            formatRow.getChildren().add(checkBox);
        }

        VBox sortSection = new VBox(8, sortHeading, criteriaColumn);
        VBox formatSection = new VBox(8, formatHeading, formatRow);
        VBox content = new VBox(22, sortSection, formatSection);
        content.setPadding(new Insets(20, 28, 8, 28));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        Observable[] formatSelections = checkBoxes.values().stream()
                .map(CheckBox::selectedProperty)
                .toArray(Observable[]::new);
        okButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> checkBoxes.values().stream().noneMatch(CheckBox::isSelected),
                formatSelections));

        return dialog.showAndWait()
                .filter(result -> result == ButtonType.OK)
                .map(result -> {
                    Set<String> selectedFormats = new LinkedHashSet<>();
                    checkBoxes.forEach((extension, box) -> {
                        if (box.isSelected()) {
                            selectedFormats.add(extension);
                        }
                    });
                    return new Selection(selectedCriteria, selectedFormats);
                });
    }

    private static void toggleSelection(Map<SortKey, SortDirection> selected, SortKey key, SortDirection direction) {
        if (selected.get(key) == direction) {
            selected.remove(key);
        } else {
            selected.put(key, direction);
        }
    }

    private static void setActive(Button button, boolean active) {
        button.getStyleClass().remove("sort-cell-active");
        if (active) {
            button.getStyleClass().add("sort-cell-active");
        }
    }
}
