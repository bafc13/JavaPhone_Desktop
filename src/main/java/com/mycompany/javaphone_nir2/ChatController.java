package com.mycompany.javaphone_nir2;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.util.Duration;

/**
 * Контроллер для главного окна чата.
 *
 * Отвечает за:
 * 1. Управление списком контактов
 * 2. Отображение истории сообщений
 * 3. Отправку новых сообщений
 * 4. Переход на окно видеозвонка
 * 5. Обработку кнопок навигации (Exit, Settings)
 */
public class ChatController {

    @FXML
    private ListView<Contact> contactsList;

    @FXML
    private TextArea chatHistory;

    @FXML
    private TextField messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private Button callButton;

    @FXML
    private Label chatTitleLabel;

    @FXML
    private VBox chatPanel;

    private ObservableList<Contact> contacts;
    private Contact selectedContact;

    private Popup incomingCallPopup;  // Всплывающее окно (Popup)
    private Contact incomingCallContact;     // От кого входящий звонок

    @FXML
    public void initialize() {
        setupContactList();
        setupChatUI();

        // Запускаем таймер для входящего звонка
        scheduleIncomingCallTimer();
    }

    /**
     * ⏰ Запускает таймер для входящего звонка
     * Через 10-15 секунд после загрузки появляется уведомление
     */
    private void scheduleIncomingCallTimer() {
        // Выбираем контакт для входящего звонка (первый из списка или случайный)
        if (contacts != null && !contacts.isEmpty()) {
            incomingCallContact = contacts.get(0);  // Берём первый контакт

            // Случайная задержка от 10 до 15 секунд
            int delaySeconds = 2 + (int)(Math.random() * 6);

            PauseTransition pause = new PauseTransition(Duration.seconds(delaySeconds));
            pause.setOnFinished(e -> {
                Platform.runLater(this::showIncomingCallNotification);
            });
            pause.play();
        }
    }

    /**
     * 🔔 Показывает уведомление о входящем звонке через Popup
     * Popup - это отдельное немодальное окно, которое появляется поверх основного интерфейса
     */
    private void showIncomingCallNotification() {
        if (incomingCallContact == null) return;

        // Создаём контейнер для уведомления
        VBox notificationBox = new VBox(12);
        notificationBox.setStyle(
            "-fx-background-color: #434e93; " +  // Тёмно-синий фон
            "-fx-border-color: #3b82f6; " +       // Синяя граница
            "-fx-border-width: 0; " +
            "-fx-border-radius: 10; " +
            "-fx-padding: 15; " +
            "-fx-spacing: 8;"
        );
        notificationBox.setAlignment(Pos.CENTER);
        notificationBox.setMaxWidth(200);
        notificationBox.setPrefWidth(200);

        // Заголовок "Звонок от..."
        Label incomingLabel = new Label("🔔 Звонок от");
        incomingLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold;"
        );
        incomingLabel.setAlignment(Pos.CENTER);

        // Имя контакта
        Label contactLabel = new Label(incomingCallContact.getName());
        contactLabel.setStyle(
            "-fx-text-fill: #60a5fa; " +  // Светло-синий цвет
            "-fx-font-size: 18; " +
            "-fx-font-weight: bold;"
        );
        contactLabel.setAlignment(Pos.CENTER);
        contactLabel.setWrapText(true);

        // Контейнер для кнопок
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(5, 0, 0, 0));

        // Кнопка "Принять"
        Button acceptButton = new Button("Принять");
        acceptButton.setPrefWidth(90);
        acceptButton.setPrefHeight(32);
        acceptButton.setStyle(
            "-fx-background-color: #10b981; " +  // Зелёный
            "-fx-text-fill: white; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 5; " +
            "-fx-border-radius: 6;"
        );
        acceptButton.setOnAction(e -> {
            handleCallAccepted();
            incomingCallPopup.hide();
        });

        // Кнопка "Отклонить"
        Button rejectButton = new Button("Отклонить");
        rejectButton.setPrefWidth(90);
        rejectButton.setPrefHeight(32);
        rejectButton.setStyle(
            "-fx-background-color: #ef4444; " +  // Красный
            "-fx-text-fill: white; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 5; " +
            "-fx-border-radius: 6;"
        );
        rejectButton.setOnAction(e -> {
            handleCallRejected();
            incomingCallPopup.hide();
        });

        buttonBox.getChildren().addAll(acceptButton, rejectButton);

        // Собираем всё в контейнер
        notificationBox.getChildren().addAll(
            incomingLabel,
            contactLabel,
            buttonBox
        );

        // ========== СОЗДАЁМ POPUP ==========
        incomingCallPopup = new Popup();
        incomingCallPopup.getContent().add(notificationBox);
        incomingCallPopup.setAutoHide(false);  // Не закрывается при клике вне окна
        incomingCallPopup.setHideOnEscape(false);  // Не закрывается на ESC

        // Показываем Popup в верхнем правом углу экрана
        if (callButton != null) {
            // Получаем позицию кнопки "Позвонить"
            Bounds buttonBounds = callButton.localToScreen(callButton.getBoundsInLocal());

            // Показываем Popup ниже и справа от кнопки
            incomingCallPopup.show(
                callButton.getScene().getWindow(),
                buttonBounds.getCenterX() - 135,  // По центру кнопки (минус половина ширины попапа)
                buttonBounds.getCenterY() + 30    // Ниже кнопки на 40px
            );
        }

        appendToChat("System", "🔔 Входящий звонок от " + incomingCallContact.getName());
    }

    /**
     * ✅ Обработчик: Пользователь принял звонок
     * Переводит на видеозвонок с контактом
     */
    private void handleCallAccepted() {
        appendToChat("System", "✅ Вы приняли звонок от " + incomingCallContact.getName());

        // Переводим на видеозвонок
        startVideoCallWithContact(incomingCallContact);
    }

    /**
     * ❌ Обработчик: Пользователь отклонил звонок
     * Просто закрывает уведомление
     */
    private void handleCallRejected() {
        appendToChat("System", "❌ Вы отклонили звонок от " + incomingCallContact.getName());
    }

    /**
     * 📞 Запускает видеозвонок с указанным контактом
     */
    private void startVideoCallWithContact(Contact contact) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("video_call.fxml")
            );

            Scene scene = new Scene(loader.load(), 1200, 700);
            Stage videoStage = new Stage();
            videoStage.setTitle("WebCommunicator - Video Call with " + contact.getName());
            videoStage.setScene(scene);
            videoStage.setMinWidth(1000);
            videoStage.setMinHeight(600);

            VideoCallController controller = loader.getController();
            controller.setContactName(contact.getName());
            controller.initializeResponsiveLayout(videoStage);

            videoStage.show();

            appendToChat("System", "📞 Видеозвонок с " + contact.getName() + " начат");

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке окна видеозвонка: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Не удалось открыть окно видеозвонка");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Инициализирует список контактов
     */
    private void setupContactList() {
        contacts = javafx.collections.FXCollections.observableArrayList(
            new Contact("AVKuzma", "Online"),
            new Contact("BaFC13", "Online"),
            new Contact("Кто-то", "Offline")
        );

        contactsList.setItems(contacts);
        contactsList.setCellFactory(param -> new ContactListCell());
        contactsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedContact = newVal;
                updateChatPanel();
            }
        });
    }

    /**
     * Инициализирует UI чата
     */
    private void setupChatUI() {
        chatHistory.setEditable(false);
        chatHistory.setWrapText(true);

        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());

        callButton.setOnAction(e -> startVideoCall());
    }

    /**
     * Обновляет панель чата при выборе контакта
     */
    private void updateChatPanel() {
        if (selectedContact != null) {
            chatTitleLabel.setText("Чат с " + selectedContact.getName());
            chatHistory.clear();
            appendToChat("System", "Чат с " + selectedContact.getName() + " начат");
        }
    }

    /**
     * Отправляет сообщение
     */
    private void sendMessage() {
        if (selectedContact == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Предупреждение");
            alert.setHeaderText("Контакт не выбран");
            alert.setContentText("Выберите контакт для отправки сообщения");
            alert.showAndWait();
            return;
        }

        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        appendToChat("Вы", message);
        messageInput.clear();

        // Имитация ответа
        simulateResponse();
    }

    /**
     * Имитирует ответ от собеседника
     */
    private void simulateResponse() {
        String[] responses = {
            "Привет! 😊",
            "Как дела?",
            "Слышу тебя хорошо",
            "Что нового?",
            "Спасибо за сообщение!"
        };

        String response = responses[(int) (Math.random() * responses.length)];

        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            appendToChat(selectedContact.getName(), response);
        });
    }

    /**
     * Запускает видеозвонок с выбранным контактом
     */
    private void startVideoCall() {
        if (selectedContact == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Предупреждение");
            alert.setHeaderText("Контакт не выбран");
            alert.setContentText("Пожалуйста, выберите контакт перед началом вызова");
            alert.showAndWait();
            return;
        }

        startVideoCallWithContact(selectedContact);
    }

    /**
     * Добавляет сообщение в историю чата
     */
    private void appendToChat(String sender, String message) {
        String formattedMessage = String.format("%s: %s\n", sender, message);
        chatHistory.appendText(formattedMessage);
    }

    /**
     * Вспомогательный класс для отображения контактов
     */
    private static class ContactListCell extends ListCell<Contact> {
        @Override
        protected void updateItem(Contact contact, boolean empty) {
            super.updateItem(contact, empty);
            if (empty || contact == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);

                Circle statusCircle = new Circle(5);
                statusCircle.setFill(javafx.scene.paint.Color.web(
                    contact.getStatus().equals("Online") ? "#10b981" : "#6b7280"
                ));

                Label nameLabel = new Label(contact.getName());
                Label statusLabel = new Label(contact.getStatus());
                statusLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");

                VBox vbox = new VBox(2);
                vbox.getChildren().addAll(nameLabel, statusLabel);

                hbox.getChildren().addAll(statusCircle, vbox);
                setGraphic(hbox);
            }
        }
    }

    /**
     * Класс для хранения данных контакта
     */
    public static class Contact {
        private String name;
        private String status;

        public Contact(String name, String status) {
            this.name = name;
            this.status = status;
        }

        public String getName() { return name; }
        public String getStatus() { return status; }
    }
}