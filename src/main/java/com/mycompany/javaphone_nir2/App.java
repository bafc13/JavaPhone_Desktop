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
     * Метод start() вызывается JavaFX при запуске приложения.
     * Здесь мы инициализируем главное окно и загружаем его содержимое.
     *
     * @param primaryStage главное окно приложения
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Загружаем FXML-макет главного окна
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("chat_main.fxml")
            );

            // Создаём сцену из загруженного FXML
            scene = new Scene(loader.load(), 1000, 600);

            // Устанавливаем титул окна
            primaryStage.setTitle("WebCommunicator - Chat");

            // Устанавливаем сцену на сцене
            primaryStage.setScene(scene);

            // Устанавливаем минимальный размер окна
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(500);

            // Показываем окно
            primaryStage.show();

        } catch (IOException e) {
            // Обработка исключения при загрузке FXML
            System.err.println("Ошибка при загрузке FXML файла: " + e.getMessage());
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