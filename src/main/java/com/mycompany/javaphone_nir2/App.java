package com.mycompany.javaphone_nir2;

import com.mycompany.javaphone_nir2.controllers.ChatController;
import com.mycompany.javaphone_nir2.cryptography.MessageCryptographer;
import com.mycompany.javaphone_nir2.logging.SessionLogger;
import com.mycompany.javaphone_nir2.models.SettingsManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    /**
     * start() used by JavaFX while app is running.
     *
     * @param primaryStage main window
     */
    @Override
    public void start(Stage primaryStage) {
        SettingsManager settings = SettingsManager.getInstance();
        SessionLogger logger = SessionLogger.getInstance();
        MessageCryptographer MC = MessageCryptographer.getInstance();
        
        logger.log("Application started. Initializing UI...");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("fxml/chat_main.fxml")
            );

            scene = new Scene(loader.load(), 1000, 600);
            scene.getStylesheets().add(getClass().getResource("dark".equals(settings.getTheme()) ?
                    "css/chat_main_dark.css"
                    : "css/chat_main.css").toExternalForm());

            settings.themeProperty().addListener((obs, oldTheme, newTheme) -> {
                logger.log("Chat window: theme changing, new theme: " + newTheme);

                scene.getStylesheets().removeIf(s -> s.contains("css/chat_main_dark.css") || s.contains("css/chat_main.css"));
                scene.getStylesheets().add(getClass().getResource("dark".equals(newTheme) ?
                        "css/chat_main_dark.css"
                        : "css/chat_main.css").toExternalForm());
                });

            primaryStage.setTitle("WebCommunicator - Chat");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(500);

            logger.log("Showing Chat window");
            primaryStage.show();

            ChatController chatController = loader.getController();
            chatController.initializeResponsiveLayout(primaryStage);


        } catch (IOException e) {
            System.err.println("Error while getting FXML file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

