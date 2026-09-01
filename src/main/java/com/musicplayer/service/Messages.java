package com.musicplayer.service;

import com.musicplayer.model.Language;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public final class Messages {

    private static ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", Language.ENGLISH.toLocale());

    private Messages() {
    }

    public static void setLanguage(Language language) {
        Locale locale = language.toLocale();
        Locale.setDefault(locale);
        bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    public static String get(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    public static String get(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }
}
