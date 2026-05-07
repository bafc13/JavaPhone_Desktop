package com.mycompany.javaphone_nir2.games;

import com.mycompany.javaphone_nir2.webrtc.WebRTCManager;
import com.mycompany.javaphone_nir2.models.SettingsManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.util.UUID;

public class GameMenuApp {
    
    private static GameMenuApp instance;
    private Stage gameSelectionStage;
    private String myPersistentId;
    private GameThemeHelper themeHelper;
    
    private GameMenuApp() {
        myPersistentId = UUID.randomUUID().toString();
        themeHelper = GameThemeHelper.getInstance();
        themeHelper.init(); // <- Инициализируем хелпер тем
        System.out.println("🎮 GameMenuApp created with ID: " + myPersistentId);
    }
    
    public static GameMenuApp getInstance() {
        if (instance == null) {
            instance = new GameMenuApp();
        }
        return instance;
    }
    
    public String getMyId() {
        return myPersistentId;
    }
    
    public void showGameSelection() {
        gameSelectionStage = new Stage();
        gameSelectionStage.setTitle("Выбор игры");
        
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        themeHelper.applyToContainer(root);
        
        Text title = new Text("Выберите игру");
        themeHelper.styleText(title, true);
        
        Label statusLabel = new Label("Выберите игру и нажмите 'Готов'");
        themeHelper.styleLabel(statusLabel, false);
        
        ComboBox<String> gameSelector = new ComboBox<>();
        gameSelector.getItems().addAll("Крестики-нолики", "Морской бой", "Шахматы");
        gameSelector.setPromptText("Выберите игру");
        gameSelector.setStyle("-fx-font-size: 14px;");
        gameSelector.setPrefWidth(200);
        
        // Стилизуем ComboBox под тему
        if (themeHelper.isDarkTheme()) {
            gameSelector.setStyle("-fx-font-size: 14px; -fx-background-color: #2d2d3d; -fx-text-fill: white;");
        } else {
            gameSelector.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-text-fill: black;");
        }
        
        Button readyButton = new Button("Готов");
        themeHelper.styleButton(readyButton);
        readyButton.setOnAction(e -> {
            String selected = gameSelector.getValue();
            if (selected == null) {
                statusLabel.setText("Пожалуйста, выберите игру!");
                statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px;");
                return;
            }
            
            String gameType = convertGameType(selected);
            GameController.getInstance().onGameSelected(gameType);
            gameSelectionStage.close();
        });
        
        root.getChildren().addAll(title, statusLabel, gameSelector, readyButton);
        
        Scene scene = new Scene(root, 400, 350);
        themeHelper.applyThemeToScene(scene);
        gameSelectionStage.setScene(scene);
        gameSelectionStage.show();
    }
    
    private String convertGameType(String displayName) {
        switch (displayName) {
            case "Крестики-нолики": return "tic-tac-toe";
            case "Морской бой": return "sea_battle";
            case "Шахматы": return "chess";
            default: return "tic-tac-toe";
        }
    }
    
    public void onGameMessage(String message) {
        System.out.println("📨 [Menu] Message received, passing to controller: " + message);
        if (GameController.getInstance() != null) {
            GameController.getInstance().handleMessage(message);
        }
    }
    
    public void sendGameMessage(String message) {
        System.out.println("📤 [Menu] Sending message: " + message);
        WebRTCManager.getInstance().sendGameMessage(message);
    }
    
    public void closeGame() {
        GameController.getInstance().closeGame();
    }
    
    public void onGameChannelReady() {
        System.out.println("🎮 GameMenuApp: Game channel ready!");
        GameController.getInstance().onGameChannelReady();
    }
}