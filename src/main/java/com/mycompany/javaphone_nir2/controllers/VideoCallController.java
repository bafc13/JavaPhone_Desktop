package com.mycompany.javaphone_nir2.controllers;

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
 * controller for video call
 *
 * Responsible for: 1. Video stream management (simulation for prototype) 2.
 * Button management (microphone, camera, filter) 3. Displaying messages during
 * a call. 4. Ending the call and returning to the main window
 */
public class VideoCallController {

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

    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private boolean isMicEnabled = true;
    private boolean isCameraEnabled = true;
    private boolean isFilterEnabled = false;
    private String contactName = "Собеседник";

    @FXML
    public void initialize() {
        setupButtonHandlers();

        appendToCallChat("System", "Соединение установлено");
        appendToCallChat("System", "Звонок активен");

        callChatHistory.setEditable(false);
    }

    /**
     * Initializes window resizing. Called from ChatController.startVideoCall()
     * with a ready Stage!
     *
     * @param stage video call scene
     */
    public void initializeResponsiveLayout(Stage stage) {
        setupWindowResizeListener(stage);
    }

    /**
     * Configures a listener for window resize events.
     */
    private void setupWindowResizeListener(Stage stage) {
        if (stage == null) {
            System.err.println("ОШИБКА: Stage равен null в setupWindowResizeListener");
            return;
        }

        // listening window resize
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateVideoContainerSizes(stage);
        });

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            updateVideoContainerSizes(stage);
        });

        // init calculating sizes
        updateVideoContainerSizes(stage);
    }

    /**
     * calculating and updating video cont sizes
     */
    private void updateVideoContainerSizes(Stage stage) {
        double windowWidth = stage.getWidth();
        double windowHeight = stage.getHeight();

        double availableWidth = windowWidth - 320;
        double availableHeight = windowHeight - 140;

        if (availableWidth <= 0) {
            availableWidth = 450;
        }
        if (availableHeight <= 0) {
            availableHeight = 300;
        }

        // main video
        remoteVideoContainer.setMinWidth(200);
        remoteVideoContainer.setMinHeight(200);
        remoteVideoContainer.setPrefWidth(availableWidth);
        remoteVideoContainer.setPrefHeight(availableHeight);
        remoteVideoContainer.setMaxWidth(Double.MAX_VALUE);
        remoteVideoContainer.setMaxHeight(Double.MAX_VALUE);

        // local video (camera 2)
        localVideoContainer.setMinWidth(180);
        localVideoContainer.setMinHeight(120);

        double localVideoWidth = availableWidth * 0.2;    // 20% of main camera
        double localVideoHeight = availableHeight * 0.25;  // 25% or main camera

        localVideoContainer.setPrefWidth(localVideoWidth);
        localVideoContainer.setPrefHeight(localVideoHeight);
        localVideoContainer.setMaxWidth(localVideoWidth);
        localVideoContainer.setMaxHeight(localVideoHeight);
    }

    /**
     * sets contact name
     */
    public void setContactName(String name) {
        this.contactName = name;
        if (callStatusLabel != null) {
            callStatusLabel.setText("Звонок с " + contactName);
        }
    }

    /**
     * sets listeners for all buttons
     */
    private void setupButtonHandlers() {
        // mic button
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

        // filter button
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

        // camera button
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

        // send message button
        sendCallMessageButton.setOnAction(e -> sendCallMessage());
        callMessageInput.setOnAction(e -> sendCallMessage());

        // exit button
        endCallButton.setOnAction(e -> endCall());
    }

    /**
     * send message to call chat
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
     * ending call and closing window
     */
    private void endCall() {
        appendToCallChat("System", "📞 Звонок завершен");

        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
                //do close video and audio threads job
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            closeWindow();
        });
    }

    private void closeWindow() {
        Stage stage = (Stage) endCallButton.getScene().getWindow();
        stage.close();
    }

    /**
     * adding new message to call chat
     */
    private void appendToCallChat(String sender, String message) {
        String time = LocalTime.now().format(timeFormatter);
        String formattedMessage = String.format("[%s] %s: %s\n", time, sender, message);
        callChatHistory.appendText(formattedMessage);
        callChatHistory.setScrollTop(Double.MAX_VALUE);
    }
}

