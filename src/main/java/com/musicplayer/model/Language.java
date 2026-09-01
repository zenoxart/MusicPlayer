package com.musicplayer.model;

import java.util.Locale;

public enum Language {
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(code);
    }

    public static Language fromCode(String code) {
        for (Language language : values()) {
            if (language.code.equals(code)) {
                return language;
            }
        }
        return ENGLISH;
    }
}
