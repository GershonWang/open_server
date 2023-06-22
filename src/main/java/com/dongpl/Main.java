package com.dongpl;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("fxml/Main.fxml")));
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add("css/main.css");
        primaryStage.setScene(scene);
        primaryStage.setTitle("数据服务");
        primaryStage.getIcons().add(new Image("images/logo.png"));
        primaryStage.show();
    }
}
