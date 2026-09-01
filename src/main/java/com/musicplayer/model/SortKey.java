package com.musicplayer.model;

public enum SortKey {
    RATING("sort.rating"),
    ARTIST("sort.artist"),
    LENGTH("sort.length");

    private final String messageKey;

    SortKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
