package com.musicplayer.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public final class Toast {

    private final Label label = new Label();
    private final PauseTransition hideDelay = new PauseTransition(Duration.seconds(2.5));
    private final FadeTransition fadeOut = new FadeTransition(Duration.millis(300), label);

    public Toast(StackPane overlay) {
        label.getStyleClass().add("toast");
        label.setVisible(false);
        label.setOpacity(0);
        StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
        StackPane.setMargin(label, new Insets(0, 0, 110, 0));
        overlay.getChildren().add(label);

        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> label.setVisible(false));
        hideDelay.setOnFinished(e -> fadeOut.playFromStart());
    }

    public void show(String message) {
        fadeOut.stop();
        hideDelay.stop();
        label.setText(message);
        label.setOpacity(1);
        label.setVisible(true);
        hideDelay.playFromStart();
    }
}
