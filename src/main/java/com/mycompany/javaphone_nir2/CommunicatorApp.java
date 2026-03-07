package com.mycompany.javaphone_nir2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Главный класс приложения - точка входа в JavaFX веб-коммуникатор.
 *
 * Этот класс отвечает за:
 * 1. Инициализацию главного окна приложения
 * 2. Загрузку FXML-макета для главного окна чата
 * 3. Установку стиля и размеров окна
 */
public class CommunicatorApp extends Application {

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
            Scene scene = new Scene(loader.load(), 1000, 600);

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

    /**
     * Точка входа в приложение. Просто вызывает launch() с параметрами.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        launch(args);
    }
}
