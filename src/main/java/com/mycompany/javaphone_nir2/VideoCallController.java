package com.mycompany.javaphone_nir2;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Контроллер для окна видеозвонка.
 *
 * Отвечает за:
 * 1. Управление видеопотоками (имитация для прототипа)
 * 2. Управление кнопками (микрофон, камера, фильтр)
 * 3. Отображение сообщений в ходе звонка
 * 4. Завершение звонка и возврат в главное окно
 */
public class VideoCallController {

    // ==================== КОМПОНЕНТЫ FXML ====================

    @FXML
    private StackPane remoteVideoContainer;

    @FXML
    private StackPane localVideoContainer;

    @FXML
    private TextArea callChatHistory;

    @FXML
    private TextField callMessageInput;

    @FXML
    private Button sendCallMessageButton;

    @FXML
    private Button micButton;

    @FXML
    private Button filterButton;

    @FXML
    private Button cameraButton;

    @FXML
    private Button endCallButton;

    @FXML
    private Label callStatusLabel;

    @FXML
    private VBox chatContainer;

    // ==================== ПЕРЕМЕННЫЕ ====================

    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private boolean isMicEnabled = true;
    private boolean isCameraEnabled = true;
    private boolean isFilterEnabled = false;
    private String contactName = "Собеседник";

    /**
     * Инициализация контроллера видеозвонка.
     */
    @FXML
    public void initialize() {
        setupButtonHandlers();

        appendToCallChat("System", "Соединение установлено");
        appendToCallChat("System", "Звонок активен");

        callChatHistory.setEditable(false);
    }

    /**
     * Инициализирует адаптивность размера окна.
     * Вызывается из ChatController.startVideoCall() с готовым Stage!
     *
     * @param stage сцена видеозвонка
     */
    public void initializeResponsiveLayout(Stage stage) {
        setupWindowResizeListener(stage);
    }

    /**
     * Настраивает слушатель событий изменения размера окна.
     */
    private void setupWindowResizeListener(Stage stage) {
        if (stage == null) {
            System.err.println("ОШИБКА: Stage равен null в setupWindowResizeListener");
            return;
        }

        // Слушаем изменение размера окна
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateVideoContainerSizes(stage);
        });

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            updateVideoContainerSizes(stage);
        });

        // Начальный расчёт размеров
        updateVideoContainerSizes(stage);
    }

    /**
     * ИСПРАВЛЕННАЯ версия - теперь окно ПРАВИЛЬНО растягивается!
     *
     * Ключевые изменения:
     * 1. Основное видео: setMaxWidth/setMaxHeight = Double.MAX_VALUE (растягивается!)
     * 2. Локальное видео: setMaxWidth/setMaxHeight = 200/150 (не растягивается, но уменьшается)
     * 3. Используем MIN, PREF, MAX для правильного макета
     */
    private void updateVideoContainerSizes(Stage stage) {
        double windowWidth = stage.getWidth();
        double windowHeight = stage.getHeight();

        double availableWidth = windowWidth - 320;
        double availableHeight = windowHeight - 140;

        if (availableWidth <= 0) availableWidth = 450;
        if (availableHeight <= 0) availableHeight = 300;

        // ==================== ОСНОВНОЕ ВИДЕО ====================
        remoteVideoContainer.setMinWidth(200);
        remoteVideoContainer.setMinHeight(200);
        remoteVideoContainer.setPrefWidth(availableWidth);
        remoteVideoContainer.setPrefHeight(availableHeight);
        remoteVideoContainer.setMaxWidth(Double.MAX_VALUE);    // 🔑 РАСТЯГИВАЕТСЯ!
        remoteVideoContainer.setMaxHeight(Double.MAX_VALUE);   // 🔑 РАСТЯГИВАЕТСЯ!

        // ==================== ЛОКАЛЬНОЕ ВИДЕО (камера 2) ====================
        localVideoContainer.setMinWidth(180);
        localVideoContainer.setMinHeight(120);

        // 🔑 ПРОПОРЦИОНАЛЬНЫЙ РАЗМЕР!
        // Камера 2 всегда = 20% ширины и 25% высоты основного видео
        double localVideoWidth = availableWidth * 0.2;    // 20% от основного
        double localVideoHeight = availableHeight * 0.25;  // 25% от основного

        localVideoContainer.setPrefWidth(localVideoWidth);
        localVideoContainer.setPrefHeight(localVideoHeight);
        localVideoContainer.setMaxWidth(localVideoWidth);   // 🔑 РАСТЯГИВАЕТСЯ!
        localVideoContainer.setMaxHeight(localVideoHeight);  // 🔑 РАСТЯГИВАЕТСЯ!
    }

    /**
     * Устанавливает имя контакта для чата.
     */
    public void setContactName(String name) {
        this.contactName = name;
        if (callStatusLabel != null) {
            callStatusLabel.setText("Звонок с " + contactName);
        }
    }

    /**
     * Устанавливает обработчики для всех кнопок управления.
     */
    private void setupButtonHandlers() {
        // КНОПКА МИКРОФОНА
        micButton.setOnAction(e -> {
            isMicEnabled = !isMicEnabled;
            if (isMicEnabled) {
                micButton.setStyle("-fx-background-color: #27ae60;");
                appendToCallChat("System", "🔊 Микрофон включен");
            } else {
                micButton.setStyle("-fx-background-color: #e74c3c;");
                appendToCallChat("System", "🔇 Микрофон отключен");
            }
        });

        // КНОПКА ФИЛЬТРА
        filterButton.setOnAction(e -> {
            isFilterEnabled = !isFilterEnabled;
            if (isFilterEnabled) {
                filterButton.setStyle("-fx-background-color: #3498db;");
                appendToCallChat("System", "✓ Фильтр применён");
            } else {
                filterButton.setStyle("-fx-background-color: #95a5a6;");
                appendToCallChat("System", "✗ Фильтр отключен");
            }
        });

        // КНОПКА КАМЕРЫ
        cameraButton.setOnAction(e -> {
            isCameraEnabled = !isCameraEnabled;
            if (isCameraEnabled) {
                cameraButton.setStyle("-fx-background-color: #27ae60;");
                appendToCallChat("System", "📷 Камера включена");
            } else {
                cameraButton.setStyle("-fx-background-color: #e74c3c;");
                appendToCallChat("System", "📷 Камера отключена");
            }
        });

        // КНОПКА ОТПРАВКИ СООБЩЕНИЯ
        sendCallMessageButton.setOnAction(e -> sendCallMessage());
        callMessageInput.setOnAction(e -> sendCallMessage());

        // КНОПКА ЗАВЕРШЕНИЯ ЗВОНКА
        endCallButton.setOnAction(e -> endCall());
    }

    /**
     * Отправляет сообщение в чат звонка.
     */
    private void sendCallMessage() {
        String message = callMessageInput.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        appendToCallChat("Вы", message);
        callMessageInput.clear();
        simulateRemoteResponse();
    }

    /**
     * Имитирует ответ от собеседника.
     */
    private void simulateRemoteResponse() {
        String[] responses = {
            "Да, я слышу тебя!",
            "Видишь меня хорошо?",
            "Отлично звучит!",
            "Как дела?",
            "Спасибо, у себя тоже всё ОК"
        };

        String response = responses[(int) (Math.random() * responses.length)];

        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            appendToCallChat(contactName, response);
        });
    }

    /**
     * Завершает звонок и закрывает окно.
     */
    private void endCall() {
        appendToCallChat("System", "📞 Звонок завершен");

        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            closeWindow();
        });
    }

    /**
     * Закрывает текущее окно видеозвонка.
     */
    private void closeWindow() {
        Stage stage = (Stage) endCallButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Добавляет новое сообщение в историю звонка.
     */
    private void appendToCallChat(String sender, String message) {
        String time = LocalTime.now().format(timeFormatter);
        String formattedMessage = String.format("[%s] %s: %s\n", time, sender, message);
        callChatHistory.appendText(formattedMessage);
        callChatHistory.setScrollTop(Double.MAX_VALUE);
    }
}