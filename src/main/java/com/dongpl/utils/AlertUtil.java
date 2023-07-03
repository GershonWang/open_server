package com.dongpl.utils;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AlertUtil {

    public static void aboutAlert(){
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        VBox vBox = new VBox();
        vBox.setStyle("-fx-alignment: center;");
        ObservableList<Node> children = vBox.getChildren();
        Label proLabel = new Label();
        proLabel.setStyle("-fx-text-fill: #fbedde;-fx-opacity: 0.7;");
        proLabel.setText("当前程序版本：1.1.1\n" +
                "当前JDK版本:" + System.getProperty("java.version") + "\n" +
                "当前JavaFX版本:" + System.getProperty("javafx.version") + "\n" +
                "当前系统版本:" + System.getProperty("os.name") + "\n" +
                "当前系统内核版本:" + System.getProperty("os.version") + "\n" +
                "软件开发者：WangShengshan");
        children.add(proLabel);
        VBox.setVgrow(proLabel, Priority.ALWAYS);
        Scene scene = new Scene(vBox, 300, 150);
        scene.getStylesheets().add("css/main.css");
        // 禁用最大化按钮
        stage.setResizable(false);
        stage.setScene(scene);
        stage.setTitle("系统信息");
        stage.getIcons().add(new Image("images/logo.png"));
        stage.showAndWait();
    }

    public static void errAlert(String content) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        VBox vBox = new VBox();
        vBox.setStyle("-fx-alignment: center;");
        ObservableList<Node> children = vBox.getChildren();
        Label proLabel = new Label(content);
        proLabel.setStyle("-fx-text-fill: #fbedde;-fx-opacity: 0.7;");
        children.add(proLabel);
        VBox.setVgrow(proLabel, Priority.ALWAYS);
        Scene scene = new Scene(vBox, 300, 100);
        scene.getStylesheets().add("css/main.css");
        // 禁用最大化按钮
        stage.setResizable(false);
        stage.setScene(scene);
        stage.setTitle("错误");
        stage.getIcons().add(new Image("images/logo.png"));
        stage.showAndWait();
    }

    public static void infoAlert(String content) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        VBox vBox = new VBox();
        vBox.setStyle("-fx-alignment: center;");
        ObservableList<Node> children = vBox.getChildren();
        Label proLabel = new Label(content);
        proLabel.setStyle("-fx-text-fill: #fbedde;-fx-opacity: 0.7;");
        children.add(proLabel);
        VBox.setVgrow(proLabel, Priority.ALWAYS);
        Scene scene = new Scene(vBox, 300, 100);
        scene.getStylesheets().add("css/main.css");
        // 禁用最大化按钮
        stage.setResizable(false);
        stage.setScene(scene);
        stage.setTitle("提示");
        stage.getIcons().add(new Image("images/logo.png"));
        stage.showAndWait();
    }
}
