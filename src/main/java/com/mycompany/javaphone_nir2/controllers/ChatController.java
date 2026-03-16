package com.mycompany.javaphone_nir2.controllers;

import com.mycompany.javaphone_nir2.ContactListCell;
import com.mycompany.javaphone_nir2.models.Contact;
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
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.util.Duration;

/**
 * Controller for main chat window
 *
 * Responcible for: 1. Managing the contact list 2. Displaying message history
 * 3. Sending new messages 4. Switching to the video call window 5. Handling nav
 * buttons (Exit, Settings)
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
    private Button exitButton;

    @FXML
    private Label chatTitleLabel;

    private ObservableList<Contact> contacts;
    private Contact selectedContact;

    private Popup incomingCallPopup;
    private Contact incomingCallContact;

    @FXML
    public void initialize() {
        setupContactList();
        setupChatUI();

        //starting timer to demonstrate popup
        scheduleIncomingCallTimer();
    }

    /**
     * starting timer for uncoming call
     *
     */
    private void scheduleIncomingCallTimer() {
        if (contacts != null && !contacts.isEmpty()) {
            incomingCallContact = contacts.get(0);  // first contact

            // rand delay
            int delaySeconds = 2 + (int) (Math.random() * 6);

            PauseTransition pause = new PauseTransition(Duration.seconds(delaySeconds));
            pause.setOnFinished(e -> {
                Platform.runLater(this::showIncomingCallNotification);
            });
            pause.play();
        }
    }

    /**
     * shows notification via popup
     *
     */
    private void showIncomingCallNotification() {
        if (incomingCallContact == null) {
            return;
        }

        // container for notification
        VBox notificationBox = new VBox(12);
        notificationBox.setStyle(
                "-fx-background-color: #434e93; "
                + "-fx-border-color: #3b82f6; "
                + "-fx-border-width: 0; "
                + "-fx-border-radius: 10; "
                + "-fx-padding: 15; "
                + "-fx-spacing: 8;"
        );
        notificationBox.setAlignment(Pos.CENTER);
        notificationBox.setMaxWidth(200);
        notificationBox.setPrefWidth(200);

        Label incomingLabel = new Label("🔔 Звонок от");
        incomingLabel.setStyle(
                "-fx-text-fill: white; "
                + "-fx-font-size: 12; "
                + "-fx-font-weight: bold;"
        );
        incomingLabel.setAlignment(Pos.CENTER);

        Label contactLabel = new Label(incomingCallContact.getName());
        contactLabel.setStyle(
                "-fx-text-fill: #60a5fa; "
                + "-fx-font-size: 18; "
                + "-fx-font-weight: bold;"
        );
        contactLabel.setAlignment(Pos.CENTER);
        contactLabel.setWrapText(true);

        // container for buttons
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(5, 0, 0, 0));

        Button acceptButton = new Button("Принять");
        acceptButton.setPrefWidth(90);
        acceptButton.setPrefHeight(32);
        acceptButton.setStyle(
                "-fx-background-color: #10b981; "
                + "-fx-text-fill: white; "
                + "-fx-font-size: 11; "
                + "-fx-font-weight: bold; "
                + "-fx-padding: 5; "
                + "-fx-border-radius: 6;"
        );
        acceptButton.setOnAction(e -> {
            handleCallAccepted();
            incomingCallPopup.hide();
        });

        Button rejectButton = new Button("Отклонить");
        rejectButton.setPrefWidth(90);
        rejectButton.setPrefHeight(32);
        rejectButton.setStyle(
                "-fx-background-color: #ef4444; "
                + "-fx-text-fill: white; "
                + "-fx-font-size: 11; "
                + "-fx-font-weight: bold; "
                + "-fx-padding: 5; "
                + "-fx-border-radius: 6;"
        );
        rejectButton.setOnAction(e -> {
            handleCallRejected();
            incomingCallPopup.hide();
        });

        buttonBox.getChildren().addAll(acceptButton, rejectButton);

        notificationBox.getChildren().addAll(
                incomingLabel,
                contactLabel,
                buttonBox
        );

        incomingCallPopup = new Popup();
        incomingCallPopup.getContent().add(notificationBox);
        incomingCallPopup.setAutoHide(false);  // do not close when clicking behind the popup
        incomingCallPopup.setHideOnEscape(false);  // do not close on pressing esc

        if (callButton != null) {
            // getting position of call button
            Bounds buttonBounds = callButton.localToScreen(callButton.getBoundsInLocal());

            // showing popup right down of callbutton
            incomingCallPopup.show(
                    callButton.getScene().getWindow(),
                    buttonBounds.getCenterX() - 135,
                    buttonBounds.getCenterY() + 30
            );
        }

        appendToChat("System", "🔔 Входящий звонок от " + incomingCallContact.getName());
    }

    /**
     * handle call accept
     *
     */
    private void handleCallAccepted() {
        appendToChat("System", "✅ Вы приняли звонок от " + incomingCallContact.getName());

        startVideoCallWithContact(incomingCallContact);
    }

    /**
     * handle call rejected
     *
     */
    private void handleCallRejected() {
        appendToChat("System", "❌ Вы отклонили звонок от " + incomingCallContact.getName());
    }

    /**
     * starting video call with contact
     * @param contact contact to start call with
     */
    private void startVideoCallWithContact(Contact contact) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mycompany/javaphone_nir2/fxml/video_call.fxml") //the path is searched from the classpath
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
     * init contact list
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
     * setuping chat ui
     */
    private void setupChatUI() {
        chatHistory.setEditable(false);
        chatHistory.setWrapText(true);

        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());

        callButton.setOnAction(e -> startVideoCall());

        exitButton.setOnAction(e -> closeWindow());
    }

    /**
     * updating chat with selected contact
     */
    private void updateChatPanel() {
        if (selectedContact != null) {
            chatTitleLabel.setText("Чат с " + selectedContact.getName());
            chatHistory.clear();
            appendToChat("System", "Чат с " + selectedContact.getName() + " начат");
        }
    }

    /**
     * send message
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

        simulateResponse();
    }

    /**
     * response simulation
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

    private void closeWindow() {
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
                //do close call waiter and chat threads
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Stage stage = (Stage) exitButton.getScene().getWindow();
            stage.close();
            Platform.exit();
        });
    }

    /**
     * starting videocall
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
     * adding message to char history
     */
    private void appendToChat(String sender, String message) {
        String formattedMessage = String.format("%s: %s\n", sender, message);
        chatHistory.appendText(formattedMessage);
    }

    /**
     * class for showing contacts
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
}

