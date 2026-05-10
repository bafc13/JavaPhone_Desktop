package com.mycompany.javaphone_nir2.controllers;

import com.mycompany.javaphone_nir2.ChatHistoryCell;
import com.mycompany.javaphone_nir2.WebRtcVideoPanel;
import com.mycompany.javaphone_nir2.logging.SessionLogger;
import com.mycompany.javaphone_nir2.models.Message;
import com.mycompany.javaphone_nir2.models.SettingsManager;
import com.mycompany.javaphone_nir2.models.VideoLayoutMode;
import com.mycompany.javaphone_nir2.webrtc.WebRTCManager;
import dev.onvoid.webrtc.media.video.VideoTrack;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for video call
 *
 * Responsible for: 1. Video stream showing
 * 2. Call managing (microphone, camera, filter) 3. Displaying messages during
 * a call. 4. Ending the call and returning to the main window
 * 5. Starting games
 */
public class VideoCallController {

    @FXML private StackPane remoteVideoContainer;
    @FXML private HBox headerContainer;
    @FXML private StackPane localVideoContainer;
    @FXML private StackPane callChatHistoryContainer;
    @FXML private SplitPane videoSplitPane;
    @FXML private ListView<Message> callChatHistory;
    @FXML private TextField callMessageInput;
    @FXML private Button sendCallMessageButton;
    @FXML private Button gameButton;
    @FXML private HBox callControlContainer;
    @FXML private Button micButton;
    @FXML private Button filterButton;
    @FXML private Button cameraButton;
    @FXML private Button endCallButton;
    @FXML private Label callStatusLabel;
    @FXML private Label chatStatus;
    @FXML private Label callDurationLabel;
    @FXML private HBox messageContainer;
    @FXML private HBox footerContainer;
    @FXML private VBox chatContainer;
    @FXML private StackPane videoContainer;
    @FXML private WebRtcVideoPanel remoteVideo;
    @FXML private WebRtcVideoPanel localVideo;
    @FXML private Label remoteVideoLabel;
    @FXML private Label localVideoLabel;
    @FXML private Label typingIndicator;

    private HBox splitModeContainer;

    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private boolean isMicEnabled = true;
    private boolean isCameraEnabled = true;
    private boolean isFilterEnabled = false;
    private String contactName = "Собеседник";

    private Timeline callTimer;
    private long callStartTimeMillis;

    /** SettingsManager stores and saves settings */
    private final SettingsManager settings = SettingsManager.getInstance();

    /** Logger saves session information into log */
    private final SessionLogger logger = SessionLogger.getInstance();

    private VideoLayoutMode currentMode = VideoLayoutMode.PIP_PRIMARY_FIRST;

    private static VideoCallController instance = null;

    public static VideoCallController getInstance() {
        return instance;
    }

    /**
     * This method is automatically called by the FXMLLoader after the FXML file is loaded
     * and all @FXML fields have been injected
     * So this method is key method to init styles, listeners and etc before showing ui
     */
    @FXML
    public void initialize() {
        logger.log("Video call window initializing");

        setupCallUI();
        setupCallTimer();
        startCallTimer();

        setChatStatus("Соединение установлено");

        callChatHistory.setEditable(false);
        instance = this;
    }

    public void setChatStatus(String status) {
        chatStatus.setText(status);
    }

    public void removeChatStatus(){
        chatStatus.setText("");
    }

    private void setupCallTimer() {
        callTimer = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> {
                long elapsed = System.currentTimeMillis() - callStartTimeMillis;
                long totalSeconds = elapsed / 1000;

                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                long hours = minutes / 60;
                minutes %= 60;

                // Формат HH:MM:SS если >1 часа, иначе MM:SS
                String timeStr = hours > 0
                    ? String.format("- %02d:%02d:%02d", hours, minutes, seconds)
                    : String.format("- %02d:%02d", minutes, seconds);

                callDurationLabel.setText(timeStr);
            })
        );
        // Бесконечный цикл до явной остановки
        callTimer.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * Запуск таймера. Вызывать при успешном соединении.
     */
    public void startCallTimer() {
        callStartTimeMillis = System.currentTimeMillis();
        callTimer.playFromStart();
        callDurationLabel.setText("- 00:00");
    }

    /**
     * Остановка таймера. Вызывать при завершении звонка.
     */
    public void stopCallTimer() {
        callTimer.stop();
    }

    /** This method responsible for add local video track
     * @param localTrack
     */
    public void addLocalTrack(VideoTrack localTrack) {
        logger.log("Video call window: add local track sink");

        localTrack.addSink(localVideo);
    }

    /** This method responsible for add remote video track
     * @param remoteTrack
     */
    public void addRemoteTrack(VideoTrack remoteTrack) {
        logger.log("Video call window: add remote track sink");

        remoteTrack.addSink(remoteVideo);
    }

    /** This method responsible for Initialize window resizing. Called from ChatController.startVideoCall() with a ready Stage!
     * @param stage video call scene
     */
    public void initializeResponsiveLayout(Stage stage) {
        logger.log("Video call window: called func initializeResponsiveLayout");

        setupCloseInterceptor(stage);
    }

    /** This method responsible for set contact name
     * @param name contact name
     */
    public void setContactName(String name) {
        logger.log("Video call window: setting call status label");

        this.contactName = name;
        if (callStatusLabel != null) {
            callStatusLabel.setText("Звонок с " + contactName);
        }
    }

    /** This method responsible for set styles and listeners for all buttons */
    private void setupCallUI() {
        logger.log("Video call window: setupping call UI");

        initSplitPane();

        remoteVideoContainer.getStyleClass().add("video-pane");
        localVideoContainer.getStyleClass().add("video-pane");
        remoteVideoLabel.getStyleClass().add("video-label");
        localVideoLabel.getStyleClass().add("video-label");

        typingIndicator.getStyleClass().add("typing-indicator");
        chatStatus.getStyleClass().add("chat-status");
        callDurationLabel.getStyleClass().add("call-duration-label");

        applyLayoutMode(currentMode);
        setupClickHandlers();

        micButton.setOnAction(e -> {
            isMicEnabled = WebRTCManager.getInstance().isMicrophoneEnabled();

            logger.log("Video call window: turn " + isMicEnabled + " the mic");

            WebRTCManager.getInstance().toggleMicrophone();
            if (isMicEnabled) {
                micButton.setStyle("-fx-background-color: #27ae60;");
            } else {
                micButton.setStyle("-fx-background-color: #e74c3c;");
            }
        });
        micButton.getStyleClass().add("call-control-button");

        filterButton.setOnAction(e -> {
            isFilterEnabled = !isFilterEnabled;

            logger.log("Video call window: turn " + isFilterEnabled + " the filter");
            if (isFilterEnabled) {
                filterButton.setStyle("-fx-background-color: #27ae60;");
                setCameraFilters(true);
            } else {
                filterButton.setStyle("-fx-background-color: #e74c3c;");
                setCameraFilters(false);
            }
        });
        filterButton.getStyleClass().add("call-control-button");

        cameraButton.setOnAction(e -> {
            isCameraEnabled = WebRTCManager.getInstance().isCameraEnabled();

            logger.log("Video call window: turn " + isCameraEnabled + " the camera");

            WebRTCManager.getInstance().toggleCamera();
            if (isCameraEnabled) {
                cameraButton.setStyle("-fx-background-color: #27ae60;");
            } else {
                cameraButton.setStyle("-fx-background-color: #e74c3c;");
            }
        });
        cameraButton.getStyleClass().add("call-control-button");

        headerContainer.getStyleClass().add("header-container");

        callStatusLabel.getStyleClass().add("call-status-label");

        initChat();

        gameButton.getStyleClass().add("header-button");
        gameButton.setOnAction(e -> {
            openGameChooser();
        });

        endCallButton.setOnAction(e -> endCall());
        endCallButton.getStyleClass().add("header-button");

        callControlContainer.getStyleClass().add("call-control-container");

        footerContainer.getStyleClass().add("footer-container");

        videoContainer.applyCss();
        videoContainer.layout();

        initVideoNet();
    }

    /** This method responsible for initialize video net */
    private void initVideoNet() {
        logger.log("Video call window: initializing video net part");

        WebRTCManager rtcm = WebRTCManager.getInstance();
        VideoTrack localTrack = rtcm.getLocalVideoTrack();
        if (localTrack != null) {
            localTrack.addSink(localVideo);
        }
        VideoTrack remoteTrack = rtcm.getRemoteVideoTrack();
        if (remoteTrack != null) {
            remoteTrack.addSink(remoteVideo);
        }
    }

    /** This method responsible for set camera filters
     * @param setFilter true - filter on, false - filter off
     */
    private void setCameraFilters(boolean setFilter){
        logger.log("Video call window: setting camera filters");

        if(setFilter) {
            remoteVideo.setWarmthLevel(60);
            localVideo.setWarmthLevel(60);
        } else {
            remoteVideo.setWarmthLevel(0);
            localVideo.setWarmthLevel(0);
        }
    }

    /** This method responsible for init split pane */
    private void initSplitPane() {
        logger.log("Video call window: initializing split pane");

        videoSplitPane.setDividerPositions(settings.getVideoSplitRatio());

        // listen for divider pos change
        for (SplitPane.Divider divider : videoSplitPane.getDividers()) {
            divider.positionProperty().addListener((obs, oldPos, newPos) -> {
                settings.setVideoSplitRatio(newPos.doubleValue());
            });
        }
    }

    /** This method responsible for applyLayoutMode */
    private void applyLayoutMode(VideoLayoutMode mode) {
        logger.log("Video call window: applying layout mode: " + mode.name());

        videoContainer.getChildren().clear();

        remoteVideoContainer.getStyleClass().removeAll("pip-main", "pip-small", "split");
        localVideoContainer.getStyleClass().removeAll("pip-main", "pip-small", "split");
        StackPane.setAlignment(remoteVideoContainer, null);
        StackPane.setAlignment(localVideoContainer, null);

        unbindAllSizeProperties(remoteVideoContainer);
        unbindAllSizeProperties(localVideoContainer);

        switch (mode) {
            case PIP_PRIMARY_FIRST: //picture in picture primary full, remote small
                remoteVideoContainer.getStyleClass().add("pip-main");
                localVideoContainer.getStyleClass().add("pip-small");

                videoContainer.getChildren().addAll(remoteVideoContainer, localVideoContainer);
                StackPane.setAlignment(localVideoContainer, Pos.BOTTOM_LEFT);
                localVideoContainer.toFront();
                break;

            case PIP_PRIMARY_SECOND: //picture in picture remote full, local small
                localVideoContainer.getStyleClass().add("pip-main");
                remoteVideoContainer.getStyleClass().add("pip-small");

                videoContainer.getChildren().addAll(localVideoContainer, remoteVideoContainer);
                StackPane.setAlignment(remoteVideoContainer, Pos.BOTTOM_LEFT);
                remoteVideoContainer.toFront();
                break;

            case SPLIT: //same remote and local sizes
                remoteVideoContainer.getStyleClass().add("split");
                localVideoContainer.getStyleClass().add("split");

                if (splitModeContainer == null) {
                    splitModeContainer = new HBox(10);
                    splitModeContainer.setAlignment(Pos.CENTER);
                    splitModeContainer.setFillHeight(true);
                } else {
                    splitModeContainer.getChildren().clear();
                }

                HBox.setHgrow(remoteVideoContainer, Priority.ALWAYS);
                HBox.setHgrow(localVideoContainer, Priority.ALWAYS);

                splitModeContainer.getChildren().addAll(remoteVideoContainer, localVideoContainer);

                videoContainer.getChildren().add(splitModeContainer);
                break;
        }
        setupVideoSizeBindings(mode);

        //recalc layout size
        logger.log("Video call window: recalculating layout sizes");
        videoContainer.requestLayout();
    }

    /** This method responsible for setup split pane bindings
     * @param mode pip_primary_first, pip_primary_second, split
     */
    private void setupVideoSizeBindings(VideoLayoutMode mode) {
        logger.log("Video call window: setuping video size bindings");

       StackPane rootContainer = videoContainer;

       DoubleBinding availableWidth = Bindings.createDoubleBinding(() ->
           Math.max(400, rootContainer.getWidth() - 40), // -40px margins
           rootContainer.widthProperty()
       );
       DoubleBinding availableHeight = Bindings.createDoubleBinding(() ->
           Math.max(300, rootContainer.getHeight() - 40),
           rootContainer.heightProperty()
       );

       DoubleBinding pipMainWidth = availableWidth;
       DoubleBinding pipMainHeight = availableHeight;

       DoubleBinding pipSmallWidth = Bindings.createDoubleBinding(() ->
           pipMainWidth.get() * 0.25, // 25% of primary
           pipMainWidth
       );
       DoubleBinding pipSmallHeight = Bindings.createDoubleBinding(() ->
           pipSmallWidth.get() * 0.75, // 4:3 res
           pipSmallWidth
       );

       DoubleBinding splitWidth = Bindings.createDoubleBinding(() ->
           (availableWidth.get() - 10) * 0.48, // -10px margin, 48% each
           availableWidth
       );
       DoubleBinding splitHeight = availableHeight;

       unbindAllSizeProperties(remoteVideoContainer);
       unbindAllSizeProperties(localVideoContainer);

       switch (mode) {
           case PIP_PRIMARY_FIRST:
               bindPane(remoteVideoContainer, pipMainWidth, pipMainHeight, true);
               bindPane(localVideoContainer, pipSmallWidth, pipSmallHeight, false);
               break;
           case PIP_PRIMARY_SECOND:
               bindPane(localVideoContainer, pipMainWidth, pipMainHeight, true);
               bindPane(remoteVideoContainer, pipSmallWidth, pipSmallHeight, false);
               break;
           case SPLIT:
               bindPane(remoteVideoContainer, splitWidth, splitHeight, true);
               bindPane(localVideoContainer, splitWidth, splitHeight, true);
               break;
       }
   }

   /** This method responsible for unbind size properties from stack pane
    * @param pane pane that u like to unbind size properties
    */
   private void unbindAllSizeProperties(StackPane pane) {
       logger.log("Video call window: unbinding layout size properties");

       pane.prefWidthProperty().unbind();
       pane.prefHeightProperty().unbind();
       pane.minWidthProperty().unbind();
       pane.minHeightProperty().unbind();
       pane.maxWidthProperty().unbind();
       pane.maxHeightProperty().unbind();

       pane.setPrefWidth(Region.USE_COMPUTED_SIZE);
       pane.setPrefHeight(Region.USE_COMPUTED_SIZE);
   }

   /** This method responsible for bind stackpane size properties
    * @param pane pane that u bind
    * @param width width property
    * @param height height property
    * @param flexible flexible flag
    */
   private void bindPane(StackPane pane, DoubleBinding width, DoubleBinding height, boolean flexible) {
       logger.log("Video call window: bind pane custom layout properties");

       pane.prefWidthProperty().bind(width);
       pane.prefHeightProperty().bind(height);

       if (flexible) {
           pane.minWidthProperty().bind(width.multiply(0.5));
           pane.minHeightProperty().bind(height.multiply(0.8));
           pane.setMaxWidth(Double.MAX_VALUE);
           pane.setMaxHeight(Double.MAX_VALUE);
       } else {
           pane.minWidthProperty().bind(width);
           pane.minHeightProperty().bind(height);
           pane.maxWidthProperty().bind(width);
           pane.maxHeightProperty().bind(height);
       }
   }

    /** This method responsible for setup click handle for cameras */
    private void setupClickHandlers() {
        logger.log("Video call window: setuping click handlers for cameras");

        remoteVideoContainer.setOnMouseClicked(this::handleCameraClick);
        localVideoContainer.setOnMouseClicked(this::handleCameraClick);
    }

    /** This method responsible for switch camera modes by clic
     * @param event mouse event
     */
    private void handleCameraClick(MouseEvent event) {
        logger.log("Video call window: handle camera click");

        StackPane clickedPane = (StackPane) event.getSource();

        switch (currentMode) {
            case PIP_PRIMARY_FIRST:
                // if clicked on small - she becomes main
                if (clickedPane == localVideoContainer) {
                    currentMode = VideoLayoutMode.PIP_PRIMARY_SECOND;
                } else {
                    // click on main - split mode
                    currentMode = VideoLayoutMode.SPLIT;
                }
                break;

            case PIP_PRIMARY_SECOND:
                if (clickedPane == remoteVideoContainer) {
                    currentMode = VideoLayoutMode.PIP_PRIMARY_FIRST;
                } else {
                    currentMode = VideoLayoutMode.SPLIT;
                }
                break;

            case SPLIT:
                // in split mode click on each camera makes it main
                if (clickedPane == remoteVideoContainer) {
                    currentMode = VideoLayoutMode.PIP_PRIMARY_FIRST;
                } else {
                    currentMode = VideoLayoutMode.PIP_PRIMARY_SECOND;
                }
                break;
        }

        applyLayoutMode(currentMode);
        event.consume();
    }

    /** This method responsible for load chat history from list
     * @param messages list of messages
     */
    public void loadCallChatHistory(List<Message> messages) {
        logger.log("Video call window: loading call chat history");

        callChatHistory.getItems().setAll(messages);

        Platform.runLater(() -> callChatHistory.scrollTo(callChatHistory.getItems().size() - 1));
    }

    /** This method responsible for init chat */
    private void initChat() {
        logger.log("Video call window: initializing chat history");

        callChatHistory.setCellFactory(lv -> new ChatHistoryCell());
        callChatHistory.getStyleClass().add("chat-history");
        callChatHistory.setOnMousePressed(event -> {
            if (callMessageInput != null) {
                callMessageInput.requestFocus();
            }
        });

        // send message button
        sendCallMessageButton.setOnAction(e -> sendCallMessage());
        sendCallMessageButton.getStyleClass().add("send-button");

        callMessageInput.setOnAction(e -> sendCallMessage());
        callMessageInput.getStyleClass().add("message-input");

        messageContainer.getStyleClass().add("mesage-container");

        chatContainer.getStyleClass().add("message-container");
    }


    /** This method responsible for send message to call chat */
    private void sendCallMessage() {
        logger.log("Video call window: sending call message");

        String message = callMessageInput.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        WebRTCManager rtcm = WebRTCManager.getInstance();

        rtcm.sendChatMessage(message);
        callMessageInput.clear();
    }

    /** This method responsible for simulate chat response */
    private void simulateRemoteResponse() {
        String[] responses = {
            "Да, я слышу тебя!",
            "Видишь меня хорошо?",
            "Отлично звучит!",
            "Как дела?",
            "Спасибо, у себя тоже всё ОК"
        };

        String response = responses[(int) (Math.random() * responses.length)];

        Platform.runLater(() -> {
            appendToCallChat(contactName, response);
        });
    }

    public void setTypingIndicator(boolean isTyping) {
        if (isTyping) {
            typingIndicator.setText("печатает...");
            typingIndicator.setVisible(true);
            typingIndicator.setManaged(true);
            typingIndicator.setOpacity(1.0);
        } else {
            typingIndicator.setVisible(false);
            typingIndicator.setManaged(true);
        }
    }

    /** This method responsible for ending call and closing window */
    private void endCall() {
        logger.log("Video call window: ending call");

        setChatStatus("Звонок завершён");
        stopCallTimer();

        javafx.application.Platform.runLater(() -> {
            closeWindow();
            WebRTCManager.getInstance().hangup();
        });
    }

    /** This method responsible for open game chooser window */
    private void openGameChooser() {
        logger.log("Video call window: open game chooser");


        //that how i think we might choose themes (example with putting theme and listen to changes)

        //scene.getStylesheets().add(getClass().getResource("dark".equals(settings.getTheme()) ?
        //                    "css/chat_main_dark.css"
        //                    : "css/chat_main.css").toExternalForm());
        //
        //            settings.themeProperty().addListener((obs, oldTheme, newTheme) -> {
//       scene.getStylesheets().removeIf(s -> s.contains("css/chat_main_dark.css") || s.contains("css/chat_main.css"));
        //            scene.getStylesheets().add(getClass().getResource("dark".equals(newTheme) ?
        //                    "css/chat_main_dark.css"
        //                    : "css/chat_main.css").toExternalForm());
        //            });
    }

    /** This method responsible for redefine window closing */
    private void setupCloseInterceptor(Stage stage) {
        logger.log("Video call window: override the window closing method");

        stage.setOnCloseRequest(event -> {
            WebRTCManager.getInstance().cleanup();
            closeWindow();
            WebRTCManager.getInstance().cleanup();
            event.consume();
        });
    }

    /** This method responsible for close window */
    private void closeWindow() {
        logger.log("Video call window: user requested closing window");

        settings.save();

        Platform.runLater(() -> {
            Stage stage = (Stage) endCallButton.getScene().getWindow();
            stage.close();
        });
    }

    /** This method responsible for add message to chat
     * @param sender
     * @param content message
     */
    public void appendToCallChat(String sender, String content) {
        logger.log("Video call window: append message to call chat, sender");

        Message msg = new Message();
        msg.setId(generateMessageId());
        msg.setChatId(1);
        if (sender.equals(settings.getUserKey())) {
            msg.setSenderPublicKey("Вы");
        } else {
            msg.setSenderPublicKey(sender);
        }
        msg.setContent(content);
        msg.setTime(System.currentTimeMillis() / 1000); // Unix timestamp in sec

        Platform.runLater(() -> {
            appendToCallChat(msg);
        });
    }

    /** This method responsible for adding message to history
    * @param message (id, chatId, sender public key, content, time, attachments)
    */
    public void appendToCallChat(Message message) {
        logger.log("Video call window: append message to call chat");

        callChatHistory.getItems().add(message);

        Platform.runLater(() -> {
            int lastIndex = callChatHistory.getItems().size() - 1;
            if (lastIndex >= 0) {
                callChatHistory.scrollTo(lastIndex);
            }
        });
    }

    /** This method responsible for  simple generation message id */
    private int generateMessageId() {
        int id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        logger.log("Video call window: generationg message id: " + id);

        return id;
    }
}

