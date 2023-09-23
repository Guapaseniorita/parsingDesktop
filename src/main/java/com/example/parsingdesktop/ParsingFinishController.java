package com.example.parsingdesktop;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class ParsingFinishController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button closeButton;

    @FXML
    void close(ActionEvent event) {
        Platform.exit();
    }
    @FXML
    private Label statusLabel;
    public void initialize() {
        Timeline timeline = new Timeline();

        // Создание первого ключевого кадра
        KeyFrame keyFrame1 = new KeyFrame(Duration.seconds(0), event -> {
            statusLabel.setText("Идет парсинг...");
            closeButton.setVisible(false);
        });
//         Создание второго ключевого кадра с задержкой
        KeyFrame keyFrame2 = new KeyFrame(Duration.seconds(4), event -> {
            statusLabel.setText("Парсинг окончен");
            closeButton.setVisible(true);
        });

//         Добавление ключевых кадров в таймлайн
        timeline.getKeyFrames().addAll(keyFrame1, keyFrame2);

        // Запуск таймлайна
        timeline.play();
    }

}
