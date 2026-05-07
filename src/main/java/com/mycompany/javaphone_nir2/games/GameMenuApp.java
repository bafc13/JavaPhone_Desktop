package com.mycompany.javaphone_nir2.games;

import com.mycompany.javaphone_nir2.webrtc.WebRTCManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class GameMenuApp {

    private static GameMenuApp instance;
    private Stage gameSelectionStage;

    private GameMenuApp() {
    }

    public static GameMenuApp getInstance() {
        if (instance == null) {
            instance = new GameMenuApp();
        }
        return instance;
    }

    // Shows the game selection window
    public void showGameSelection() {
        gameSelectionStage = new Stage();
        gameSelectionStage.setTitle("Выбор игры");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 30; -fx-background-color: #2c3e50;");

        Text title = new Text("Выберите игру");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-fill: #ecf0f1;");

        Label statusLabel = new Label("Выберите игру и нажмите 'Готов'");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-fill: #bdc3c7;");

        ComboBox<String> gameSelector = new ComboBox<>();
        gameSelector.getItems().addAll("Крестики-нолики", "Морской бой", "Шахматы");
        gameSelector.setPromptText("Выберите игру");
        gameSelector.setStyle("-fx-font-size: 14px;");
        gameSelector.setPrefWidth(200);

        Button readyButton = new Button("Готов");
        readyButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-color: #27ae60; -fx-text-fill: white;");
        readyButton.setOnAction(e -> {
            String selected = gameSelector.getValue();
            if (selected == null) {
                statusLabel.setText("Пожалуйста, выберите игру!");
                statusLabel.setStyle("-fx-fill: #e74c3c;");
                return;
            }

            String gameType = convertGameType(selected);

            // Notify GameController about game selection
            GameController.getInstance().onGameSelected(gameType);

            gameSelectionStage.close();
        });

        root.getChildren().addAll(title, statusLabel, gameSelector, readyButton);

        Scene scene = new Scene(root, 400, 350);
        gameSelectionStage.setScene(scene);
        gameSelectionStage.show();
    }

    private String convertGameType(String displayName) {
        switch (displayName) {
            case "Крестики-нолики":
                return "tic-tac-toe";
            case "Морской бой":
                return "sea_battle";
            case "Шахматы":
                return "chess";
            default:
                return "tic-tac-toe";
        }
    }

    // Called when a message is received from WebRTC - forwards to GameController
    public void onGameMessage(String message) {
        System.out.println("📨 [Menu] Message got, pass to contorller: " + message);
        if (GameController.getInstance() != null) {
            GameController.getInstance().handleMessage(message);
        }
    }

    // Sends a message through WebRTC
    public void sendGameMessage(String message) {
        System.out.println("📤 [Menu] Отправка сообщения: " + message);
        WebRTCManager.getInstance().sendGameMessage(message);
    }

    // Called when the game window is closed
    public void closeGame() {
        GameController.getInstance().closeGame();
    }

    // Called when the game data channel is ready
    public void onGameChannelReady() {
        System.out.println("🎮 GameMenuApp: Игровой канал готов!");
        GameController.getInstance().onGameChannelReady();
    }
}
