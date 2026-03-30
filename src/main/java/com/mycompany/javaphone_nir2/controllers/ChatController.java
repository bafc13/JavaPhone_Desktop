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
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
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
    private HBox headerContainer;

    @FXML
    private VBox contactsContainer;

    @FXML
    private HBox messageContainer;

    @FXML
    private TextArea chatHistory;

    @FXML
    private TextField messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private Button callButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button exitButton;

    @FXML
    private Label chatTitleLabel;

    private ObservableList<Contact> contacts;
    private List<ChangeListener<Number>> popupWindowListeners = new ArrayList<>();
    private ChangeListener<Boolean> popupWindowShowingListener;
    private ChangeListener<Boolean> popupWindowFocusListener;
    private Contact selectedContact;
    private Contact incomingCallContact;
    private boolean isCallPopupActive = false;

    private Popup incomingCallPopup;
    private VBox notificationBox;

    @FXML
    public void initialize() {
        setupContactList();
        incomingCallContact = contacts.get(0);  // first contact

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

        isCallPopupActive = true;

        if (callButton != null && callButton.getScene() != null) {
            Window window = callButton.getScene().getWindow();

            //one listener for all coord changes
            ChangeListener<Number> positionListener = (obs, oldVal, newVal) -> updateCallPopupPosition();

            // adding into the list
            popupWindowListeners.add(positionListener);

            window.xProperty().addListener(positionListener);
            window.yProperty().addListener(positionListener);
            window.widthProperty().addListener(positionListener);
            window.heightProperty().addListener(positionListener);

            //close window listener
            popupWindowShowingListener = (obs, wasShowing, isNowShowing) -> {
                if (!isNowShowing && incomingCallPopup.isShowing()) {
                    hideIncomingCallNotification();
                }
            };
            window.showingProperty().addListener(popupWindowShowingListener);

            //window focus listener
            popupWindowFocusListener = (obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    if (incomingCallPopup != null && incomingCallPopup.isShowing()) {
                        incomingCallPopup.hide();
                    }
                } else {
                    if (isCallPopupActive) {
                        updateCallPopupPosition();
                    }
                }
            };
            window.focusedProperty().addListener(popupWindowFocusListener);

            //first display
            updateCallPopupPosition();
        }

        appendToChat("System", "🔔 Входящий звонок от " + incomingCallContact.getName());
    }

    /**
    * notificaion pos update func
    */
    private void updateCallPopupPosition() {
//        if (incomingCallPopup == null || !incomingCallPopup.isShowing()) {
//            return;
//        }
        if (callButton == null || callButton.getScene() == null) {
            return;
        }

        // runlater solves bad repainting
        // after x resize or window maximizing/minimizing
        Platform.runLater(() -> {
            if (!isCallPopupActive || callButton == null || callButton.getScene() == null) {
                return;
            }

            Bounds bounds = callButton.localToScreen(callButton.getBoundsInLocal());

            if (bounds != null && incomingCallPopup != null) {
                incomingCallPopup.show(
                    callButton.getScene().getWindow(),
                    bounds.getCenterX() - 140,
                    bounds.getCenterY() + 26
                );
            }
        });
    }

    /**
     * func responsible for hiding incoming call notification
     */
    private void hideIncomingCallNotification() {
        isCallPopupActive = false;

        if (incomingCallPopup == null || !incomingCallPopup.isShowing()) {
            return;
        }

        if (callButton != null && callButton.getScene() != null) {
            Window window = callButton.getScene().getWindow();

            //deleting coord listener
            for (ChangeListener<Number> listener : popupWindowListeners) {
                window.xProperty().removeListener(listener);
                window.yProperty().removeListener(listener);
                window.widthProperty().removeListener(listener);
                window.heightProperty().removeListener(listener);
            }
            popupWindowListeners.clear();

            //deleting visible listener
            if (popupWindowShowingListener != null) {
                window.showingProperty().removeListener(popupWindowShowingListener);
                popupWindowShowingListener = null;
            }

            //deleting focus listener
            if (popupWindowFocusListener != null) {
                window.focusedProperty().removeListener(popupWindowFocusListener);
                popupWindowFocusListener = null;
            }
        }

        incomingCallPopup.hide();
    }

    /**
     * handle call accept
     *
     */
    private void handleCallAccepted() {
        hideIncomingCallNotification();
        appendToChat("System", "✅ Вы приняли звонок от " + incomingCallContact.getName());

        startVideoCallWithContact(incomingCallContact);
    }

    /**
     * handle call rejected
     *
     */
    private void handleCallRejected() {
        hideIncomingCallNotification();
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
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javaphone_nir2/css/video_call.css").toExternalForm());


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
     * upload contacts list from db
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

        contactsList.getStyleClass().add("contacts-list");
    }

    /**
     * setuping chat ui
     */
    private void setupChatUI() {
        chatHistory.setEditable(false);
        chatHistory.setWrapText(true);
        chatHistory.getStyleClass().add("chat-history");

        chatTitleLabel.getStyleClass().add("chat-title-label");

        headerContainer.getStyleClass().add("header-container");

        contactsContainer.getStyleClass().add("contacts-container");

        settingsButton.setOnAction(e -> openSettings());
        settingsButton.getStyleClass().add("header-button");

        sendButton.setOnAction(e -> sendMessage());
        sendButton.getStyleClass().add("send-button");

        messageContainer.getStyleClass().add("message-container");

        messageInput.setOnAction(e -> sendMessage());
        messageInput.getStyleClass().add("message-input");

        callButton.setOnAction(e -> startVideoCall());
        callButton.getStyleClass().add("header-button");

        exitButton.setOnAction(e -> closeWindow());
        exitButton.getStyleClass().add("header-button");

        initIncomingCallNotification();
    }

    /**
     * func responsible for initialization of incoming call notification popup
     */
    private void initIncomingCallNotification() {
        // container for notification
        notificationBox = new VBox(12);
        notificationBox.getStyleClass().add("notification-box");
        notificationBox.setAlignment(Pos.CENTER);
        notificationBox.setMaxWidth(200);
        notificationBox.setPrefWidth(200);

        Label incomingLabel = new Label("🔔 Звонок от");
        incomingLabel.getStyleClass().add("incoming-call-label");
        incomingLabel.setAlignment(Pos.CENTER);

        Label contactLabel = new Label(incomingCallContact.getName());
        contactLabel.getStyleClass().add("incoming-call-contact-label");
        contactLabel.setAlignment(Pos.CENTER);
        contactLabel.setWrapText(true);

        // container for buttons
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(5, 0, 0, 0));

        Button acceptButton = new Button("Принять");
        acceptButton.setPrefWidth(90);
        acceptButton.setPrefHeight(32);
        acceptButton.getStyleClass().add("accept-button");
        acceptButton.setOnAction(e -> {
            handleCallAccepted();
        });

        Button rejectButton = new Button("Отклонить");
        rejectButton.setPrefWidth(90);
        rejectButton.setPrefHeight(32);
        rejectButton.getStyleClass().add("reject-button");
        rejectButton.setOnAction(e -> {
            handleCallRejected();
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

    private void openSettings() {

    }

    /**
     * class for showing contacts
     */
//    private static class ContactListCell extends ListCell<Contact> {
//
//        @Override
//        protected void updateItem(Contact contact, boolean empty) {
//            super.updateItem(contact, empty);
//            if (empty || contact == null) {
//                setText(null);
//                setGraphic(null);
//            } else {
//                HBox hbox = new HBox(10);
//                hbox.setAlignment(Pos.CENTER_LEFT);
//
//                Circle statusCircle = new Circle(5);
//                statusCircle.setFill(javafx.scene.paint.Color.web(
//                        contact.getStatus().equals("Online") ? "#10b981" : "#6b7280"
//                ));
//
//                Label nameLabel = new Label(contact.getName());
//                Label statusLabel = new Label(contact.getStatus());
//                statusLabel.getStyleClass().add("contact-list-cell-item");
//
//                VBox vbox = new VBox(2);
//                vbox.getChildren().addAll(nameLabel, statusLabel);
//
//                hbox.getChildren().addAll(statusCircle, vbox);
//                setGraphic(hbox);
//            }
//        }
//    }
}

