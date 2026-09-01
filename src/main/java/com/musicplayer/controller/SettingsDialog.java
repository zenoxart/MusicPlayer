package com.musicplayer.controller;

import com.musicplayer.service.LanguageStore;
import com.musicplayer.service.Messages;
import com.musicplayer.service.SettingsStore;
import com.musicplayer.service.ThemeStore;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SettingsDialog {

    private static final Map<String, String> EXTENSION_LABELS = Map.of(
            ".wav", "WAV",
            ".mp3", "MP3",
            ".m4a", "M4A",
            ".aac", "AAC"
    );

    private SettingsDialog() {
    }

    public static Optional<Set<String>> showAndWait(Stage owner, Set<String> currentExtensions,
                                                      ThemeStore themeStore, Runnable onThemeChanged,
                                                      LanguageStore languageStore, Runnable onLanguageChanged) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(Messages.get("settings.title"));
        dialog.getDialogPane().getStylesheets().add(
                SettingsDialog.class.getResource("/css/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-dialog");
        ThemeSupport.apply(dialog.getDialogPane(), themeStore);

        Label formatHeading = new Label(Messages.get("settings.formatsHeading"));
        formatHeading.getStyleClass().add("dialog-heading");

        Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();
        VBox formatSection = new VBox(14, formatHeading);
        for (String extension : SettingsStore.SUPPORTED_EXTENSIONS) {
            CheckBox checkBox = new CheckBox(EXTENSION_LABELS.getOrDefault(extension, extension.toUpperCase()));
            checkBox.setSelected(currentExtensions.contains(extension));
            checkBox.getStyleClass().add("settings-checkbox");
            checkBoxes.put(extension, checkBox);
            formatSection.getChildren().add(checkBox);
        }
        Label hint = new Label(Messages.get("settings.formatsHint"));
        hint.getStyleClass().add("dialog-hint");
        hint.setWrapText(true);
        hint.setMaxWidth(320);
        formatSection.getChildren().add(hint);

        Label themeHeading = new Label(Messages.get("settings.themeHeading"));
        themeHeading.getStyleClass().add("dialog-heading");
        Button themeButton = new Button(Messages.get("settings.themeButton"));
        themeButton.getStyleClass().add("folder-button");
        themeButton.setOnAction(e -> ThemeDialog.showAndWait(owner, themeStore, true).ifPresent(selection -> {
            themeStore.save(selection.darkMode(), selection.accentColor());
            onThemeChanged.run();
            ThemeSupport.apply(dialog.getDialogPane(), themeStore);
        }));
        VBox themeSection = new VBox(14, themeHeading, themeButton);

        Label languageHeading = new Label(Messages.get("settings.languageHeading"));
        languageHeading.getStyleClass().add("dialog-heading");
        Button languageButton = new Button(Messages.get("settings.languageButton"));
        languageButton.getStyleClass().add("folder-button");
        languageButton.setOnAction(e ->
                LanguageDialog.showAndWait(owner, languageStore.getLanguage(), themeStore, true)
                        .ifPresent(selectedLanguage -> {
                            languageStore.save(selectedLanguage);
                            onLanguageChanged.run();
                        }));
        VBox languageSection = new VBox(14, languageHeading, languageButton);

        VBox content = new VBox(22, formatSection, themeSection, languageSection);
        content.setPadding(new Insets(20, 28, 8, 28));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        Observable[] selections = checkBoxes.values().stream()
                .map(CheckBox::selectedProperty)
                .toArray(Observable[]::new);
        okButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> checkBoxes.values().stream().noneMatch(CheckBox::isSelected),
                selections));

        return dialog.showAndWait()
                .filter(result -> result == ButtonType.OK)
                .map(result -> {
                    Set<String> selected = new LinkedHashSet<>();
                    checkBoxes.forEach((extension, box) -> {
                        if (box.isSelected()) {
                            selected.add(extension);
                        }
                    });
                    return selected;
                });
    }
}
