package com.musicplayer.controller;

import com.musicplayer.service.ThemeStore;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;

public final class ThemeSupport {

    private static final String LIGHT_THEME_CLASS = "light-theme";

    private ThemeSupport() {
    }

    public static void apply(Parent root, ThemeStore themeStore) {
        applyClass(root.getStyleClass(), themeStore);
        root.setStyle(accentStyle(themeStore));
    }

    public static void apply(DialogPane pane, ThemeStore themeStore) {
        applyClass(pane.getStyleClass(), themeStore);
        pane.setStyle(accentStyle(themeStore));
    }

    public static void apply(ContextMenu menu, ThemeStore themeStore) {
        applyClass(menu.getStyleClass(), themeStore);
        menu.setStyle(accentStyle(themeStore));
    }

    private static void applyClass(javafx.collections.ObservableList<String> styleClasses, ThemeStore themeStore) {
        styleClasses.remove(LIGHT_THEME_CLASS);
        if (!themeStore.isDarkMode()) {
            styleClasses.add(LIGHT_THEME_CLASS);
        }
    }

    private static String accentStyle(ThemeStore themeStore) {
        return "-app-accent: " + themeStore.getAccentColor() + ";";
    }
}
