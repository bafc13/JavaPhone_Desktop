package com.mycompany.javaphone_nir2;

import com.mycompany.javaphone_nir2.controllers.ChatController;
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

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("fxml/chat_main.fxml")
            );

            scene = new Scene(loader.load(), 1000, 600);

            //connecting scene css style
            //if theme is dark - chose one css file, if light - choose other.
            if("dark".equals(settings.getTheme())) {

            } else {

            }
            scene.getStylesheets().add(getClass().getResource("css/chat_main.css").toExternalForm());

            primaryStage.setTitle("WebCommunicator - Chat");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(500);

            ChatController chatController = loader.getController();
            chatController.initializeResponsiveLayout(primaryStage);

            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error while getting FXML file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

