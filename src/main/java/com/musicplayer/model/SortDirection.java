package com.musicplayer.model;

public enum SortDirection {
    ASC("↑"),
    DESC("↓");

    private final String symbol;

    SortDirection(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
