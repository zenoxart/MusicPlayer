package com.musicplayer;

import com.musicplayer.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(stage);

        Scene scene = new Scene(controller.getRoot(), 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon/app-icon.png")));
        stage.setTitle("Meine Musik");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
