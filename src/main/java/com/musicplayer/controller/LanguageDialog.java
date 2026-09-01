package com.musicplayer.controller;

import com.musicplayer.model.Language;
import com.musicplayer.service.Messages;
import com.musicplayer.service.ThemeStore;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public final class LanguageDialog {

    private LanguageDialog() {
    }

    public static Optional<Language> showAndWait(Stage owner, Language currentLanguage,
                                                   ThemeStore themeStore, boolean allowCancel) {
        Language[] selected = {currentLanguage};

        Dialog<ButtonType> dialog = new Dialog<>();
        if (owner.getScene() != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle(Messages.get("language.title"));
        dialog.getDialogPane().getStylesheets().add(
                LanguageDialog.class.getResource("/css/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-dialog");
        if (themeStore != null) {
            ThemeSupport.apply(dialog.getDialogPane(), themeStore);
        }

        Label heading = new Label(Messages.get("language.heading"));
        heading.getStyleClass().add("delete-headline");

        Label subheading = new Label(Messages.get("language.subheading"));
        subheading.getStyleClass().add("dialog-hint");
        subheading.setWrapText(true);
        subheading.setMaxWidth(320);

        Button englishButton = new Button(Language.ENGLISH.getDisplayName());
        Button germanButton = new Button(Language.GERMAN.getDisplayName());
        englishButton.getStyleClass().addAll("theme-mode-cell", "theme-mode-cell-left");
        germanButton.getStyleClass().addAll("theme-mode-cell", "theme-mode-cell-right");

        Runnable[] refresh = new Runnable[1];
        englishButton.setOnAction(e -> {
            selected[0] = Language.ENGLISH;
            refresh[0].run();
        });
        germanButton.setOnAction(e -> {
            selected[0] = Language.GERMAN;
            refresh[0].run();
        });
        refresh[0] = () -> {
            englishButton.getStyleClass().remove("theme-mode-cell-active");
            germanButton.getStyleClass().remove("theme-mode-cell-active");
            (selected[0] == Language.ENGLISH ? englishButton : germanButton)
                    .getStyleClass().add("theme-mode-cell-active");
        };
        refresh[0].run();

        HBox languageRow = new HBox(englishButton, germanButton);
        languageRow.getStyleClass().add("theme-mode-row");

        VBox content = new VBox(20, heading, subheading, languageRow);
        content.setPadding(new Insets(20, 28, 8, 28));
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        if (allowCancel) {
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        }
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText(Messages.get("language.continue"));

        return dialog.showAndWait()
                .filter(result -> result == ButtonType.OK)
                .map(result -> selected[0]);
    }
}
