package com.mycompany.javaphone_nir2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

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
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("chat_main.fxml")
            );

            scene = new Scene(loader.load(), 1000, 600);

            primaryStage.setTitle("WebCommunicator - Chat");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(500);

            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error while getting FXML file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch(args);
    }

}