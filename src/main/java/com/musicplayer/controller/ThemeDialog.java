package com.musicplayer.controller;

import com.musicplayer.service.Messages;
import com.musicplayer.service.ThemeStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ThemeDialog {

    public record Selection(boolean darkMode, String accentColor) {
    }

    private static final String[] PRESET_ACCENTS = {
            "#1DB954", // Spotify green (default)
            "#2E77D0", // blue
            "#8C52FF", // purple
            "#E91E63", // pink
            "#FF7A00", // orange
            "#E04646", // red
    };

    private ThemeDialog() {
    }

    public static Optional<Selection> showAndWait(Stage owner, ThemeStore themeStore, boolean allowCancel) {
        boolean[] darkMode = {themeStore.isDarkMode()};
        String[] accentColor = {themeStore.getAccentColor()};
        Runnable[] refresh = new Runnable[1];

        Dialog<ButtonType> dialog = new Dialog<>();
        // On first launch this dialog is shown before the main Stage has a Scene
        // (Main.java sets the scene only after MainController.getRoot() returns) —
        // Dialog.initOwner() NPEs if the owner has no Scene yet, so skip it then.
        if (owner.getScene() != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle(Messages.get("theme.title"));
        dialog.getDialogPane().getStylesheets().add(
                ThemeDialog.class.getResource("/css/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-dialog");

        Label heading = new Label(Messages.get(allowCancel ? "theme.headingSettings" : "theme.headingFirstLaunch"));
        heading.getStyleClass().add("delete-headline");

        Label subheading = new Label(Messages.get(
                allowCancel ? "theme.subheadingSettings" : "theme.subheadingFirstLaunch"));
        subheading.getStyleClass().add("dialog-hint");
        subheading.setWrapText(true);
        subheading.setMaxWidth(320);

        Label modeHeading = new Label(Messages.get("theme.appearanceHeading"));
        modeHeading.getStyleClass().add("dialog-heading");

        Button darkButton = new Button(Messages.get("theme.dark"));
        Button lightButton = new Button(Messages.get("theme.light"));
        darkButton.getStyleClass().addAll("theme-mode-cell", "theme-mode-cell-left");
        lightButton.getStyleClass().addAll("theme-mode-cell", "theme-mode-cell-right");
        darkButton.setOnAction(e -> {
            darkMode[0] = true;
            refresh[0].run();
        });
        lightButton.setOnAction(e -> {
            darkMode[0] = false;
            refresh[0].run();
        });
        HBox modeRow = new HBox(darkButton, lightButton);
        modeRow.getStyleClass().add("theme-mode-row");

        Label accentHeading = new Label(Messages.get("theme.accentHeading"));
        accentHeading.getStyleClass().add("dialog-heading");

        Map<String, Region> swatches = new LinkedHashMap<>();
        HBox swatchRow = new HBox(10);
        swatchRow.setAlignment(Pos.CENTER_LEFT);
        for (String hex : PRESET_ACCENTS) {
            Region swatch = new Region();
            swatch.getStyleClass().add("theme-accent-swatch");
            swatch.setStyle("-fx-background-color: " + hex + ";");
            swatch.setOnMouseClicked(e -> {
                accentColor[0] = hex;
                refresh[0].run();
            });
            swatches.put(hex, swatch);
            swatchRow.getChildren().add(swatch);
        }

        ColorPicker customPicker = new ColorPicker(Color.web(accentColor[0]));
        customPicker.getStyleClass().add("theme-custom-picker");
        customPicker.setOnAction(e -> {
            accentColor[0] = toHex(customPicker.getValue());
            refresh[0].run();
        });

        HBox accentRow = new HBox(14, swatchRow, customPicker);
        accentRow.setAlignment(Pos.CENTER_LEFT);

        refresh[0] = () -> {
            darkButton.getStyleClass().remove("theme-mode-cell-active");
            lightButton.getStyleClass().remove("theme-mode-cell-active");
            (darkMode[0] ? darkButton : lightButton).getStyleClass().add("theme-mode-cell-active");

            swatches.forEach((hex, swatch) -> {
                swatch.getStyleClass().remove("theme-accent-swatch-active");
                if (hex.equalsIgnoreCase(accentColor[0])) {
                    swatch.getStyleClass().add("theme-accent-swatch-active");
                }
            });

            dialog.getDialogPane().getStyleClass().remove("light-theme");
            if (!darkMode[0]) {
                dialog.getDialogPane().getStyleClass().add("light-theme");
            }
            dialog.getDialogPane().setStyle("-app-accent: " + accentColor[0] + ";");
        };
        refresh[0].run();

        VBox content = new VBox(20, heading, subheading, modeHeading, modeRow, accentHeading, accentRow);
        content.setPadding(new Insets(20, 28, 8, 28));
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        if (allowCancel) {
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        }
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText(Messages.get(allowCancel ? "theme.save" : "theme.getStarted"));

        return dialog.showAndWait()
                .filter(result -> result == ButtonType.OK)
                .map(result -> new Selection(darkMode[0], accentColor[0]));
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }
}
