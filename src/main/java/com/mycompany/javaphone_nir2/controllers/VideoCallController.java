package com.mycompany.javaphone_nir2.controllers;

import com.mycompany.javaphone_nir2.ChatHistoryCell;
import com.mycompany.javaphone_nir2.WebRtcVideoPanel;
import com.mycompany.javaphone_nir2.models.Message;
import com.mycompany.javaphone_nir2.models.SettingsManager;
import com.mycompany.javaphone_nir2.models.VideoLayoutMode;
import com.mycompany.javaphone_nir2.webrtc.WebRTCManager;
import dev.onvoid.webrtc.media.video.VideoTrack;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * controller for video call
 *
 * Responsible for: 1. Video stream management (simulation for prototype) 2.
 * Button management (microphone, camera, filter) 3. Displaying messages during
 * a call. 4. Ending the call and returning to the main window
 */
public class VideoCallController {

    @FXML private StackPane remoteVideoContainer;
    @FXML private HBox headerContainer;
    @FXML private StackPane localVideoContainer;
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
    @FXML private HBox messageContainer;
    @FXML private HBox footerContainer;
    @FXML private VBox chatContainer;
    @FXML private StackPane videoContainer;
    @FXML private WebRtcVideoPanel remoteVideo;
    @FXML private WebRtcVideoPanel localVideo;
    @FXML private Label remoteVideoLabel;
    @FXML private Label localVideoLabel;

    private HBox splitModeContainer;

    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private boolean isMicEnabled = true;
    private boolean isCameraEnabled = true;
    private boolean isFilterEnabled = false;
    private String contactName = "Собеседник";

    private final SettingsManager settings = SettingsManager.getInstance();
    private VideoLayoutMode currentMode = VideoLayoutMode.PIP_PRIMARY_FIRST;

    private static VideoCallController instance = null;

    public static VideoCallController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        setupCallUI();

        appendToCallChat("System", "Соединение установлено");
        appendToCallChat("System", "Звонок активен");

        callChatHistory.setEditable(false);
        instance = this;
    }

    public void addLocalTrack(VideoTrack localTrack) {
        localTrack.addSink(localVideo);
    }

    public void addRemoteTrack(VideoTrack remoteTrack) {
        remoteTrack.addSink(remoteVideo);
    }

    /**
     * Initializes window resizing. Called from ChatController.startVideoCall()
     * with a ready Stage!
     *
     * @param stage video call scene
     */
    public void initializeResponsiveLayout(Stage stage) {
        setupCloseInterceptor(stage);
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
     * sets styles and listeners for all buttons
     */
    private void setupCallUI() {
        initSplitPane();

        remoteVideoContainer.getStyleClass().add("video-pane");
        localVideoContainer.getStyleClass().add("video-pane");
        remoteVideoLabel.getStyleClass().add("video-label");
        localVideoLabel.getStyleClass().add("video-label");

        applyLayoutMode(currentMode);
        setupClickHandlers();

        // mic button
        micButton.setOnAction(e -> {
            isMicEnabled = WebRTCManager.getInstance().isMicrophoneEnabled();;
            WebRTCManager.getInstance().toggleMicrophone();
            if (isMicEnabled) {
                micButton.setStyle("-fx-background-color: #27ae60;");
            } else {
                micButton.setStyle("-fx-background-color: #e74c3c;");
            }
        });
        micButton.getStyleClass().add("call-control-button");

        // filter button
        filterButton.setOnAction(e -> {
            isFilterEnabled = !isFilterEnabled;
            if (isFilterEnabled) {
                filterButton.setStyle("-fx-background-color: #27ae60;");
                setCameraFilters(true);
            } else {
                filterButton.setStyle("-fx-background-color: #e74c3c;");
                setCameraFilters(false);
            }
        });
        filterButton.getStyleClass().add("call-control-button");

        // camera button
        cameraButton.setOnAction(e -> {
            isCameraEnabled = WebRTCManager.getInstance().isCameraEnabled();
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

        // exit button
        endCallButton.setOnAction(e -> endCall());
        endCallButton.getStyleClass().add("header-button");

        callControlContainer.getStyleClass().add("call-control-container");

        footerContainer.getStyleClass().add("footer-container");

        // 🔥 Важно: layout после инициализации
        videoContainer.applyCss();
        videoContainer.layout();

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

    private void setCameraFilters(boolean setFilter){
        if(setFilter) {
            remoteVideo.setWarmthLevel(60);
            localVideo.setWarmthLevel(60);
        } else {
            remoteVideo.setWarmthLevel(0);
            localVideo.setWarmthLevel(0);
        }

    }

    private void initSplitPane() {
        videoSplitPane.setDividerPositions(settings.getVideoSplitRatio());

        // 2. Слушаем изменение разделителя
        for (SplitPane.Divider divider : videoSplitPane.getDividers()) {
            divider.positionProperty().addListener((obs, oldPos, newPos) -> {
                // Сохраняем в модель (реактивно вызовет update data)
                settings.setVideoSplitRatio(newPos.doubleValue());
            });
        }
    }

    /**
     * Применяет выбранный режим отображения
     */
    private void applyLayoutMode(VideoLayoutMode mode) {
        // 🔥 1. ПОЛНАЯ ОЧИСТКА: удаляем ВСЕХ прямых детей из корневого контейнера
        // Это гарантирует, что камеры не "зависнут" с двумя родителями
        videoContainer.getChildren().clear();

        // 2. Сброс стилей и позиционирования (визуальная часть)
        remoteVideoContainer.getStyleClass().removeAll("pip-main", "pip-small", "split");
        localVideoContainer.getStyleClass().removeAll("pip-main", "pip-small", "split");
        StackPane.setAlignment(remoteVideoContainer, null);
        StackPane.setAlignment(localVideoContainer, null);

        // 🔥 3. Сбрасываем старые привязки размеров (важно!)
        unbindAllSizeProperties(remoteVideoContainer);
        unbindAllSizeProperties(localVideoContainer);

        switch (mode) {
            case PIP_PRIMARY_FIRST:
                // === PiP: Камера 1 главная, Камера 2 в углу ===
                remoteVideoContainer.getStyleClass().add("pip-main");
                localVideoContainer.getStyleClass().add("pip-small");

                // Добавляем КАМЕРЫ напрямую в StackPane
                videoContainer.getChildren().addAll(remoteVideoContainer, localVideoContainer);
                StackPane.setAlignment(localVideoContainer, Pos.BOTTOM_LEFT);
                localVideoContainer.toFront(); // Маленькая поверх большой
                break;

            case PIP_PRIMARY_SECOND:
                // === PiP: Камера 2 главная, Камера 1 в углу ===
                localVideoContainer.getStyleClass().add("pip-main");
                remoteVideoContainer.getStyleClass().add("pip-small");

                videoContainer.getChildren().addAll(localVideoContainer, remoteVideoContainer);
                StackPane.setAlignment(remoteVideoContainer, Pos.BOTTOM_LEFT);
                remoteVideoContainer.toFront();
                break;

            case SPLIT:
                // === SPLIT: Обе камеры рядом в HBox ===
                remoteVideoContainer.getStyleClass().add("split");
                localVideoContainer.getStyleClass().add("split");

                // Создаём/настраиваем HBox
                if (splitModeContainer == null) {
                    splitModeContainer = new HBox(10); // 10px зазор
                    splitModeContainer.setAlignment(Pos.CENTER);
                    splitModeContainer.setFillHeight(true);
                } else {
                    splitModeContainer.getChildren().clear(); // На всякий случай
                }

                // Настраиваем приоритеты роста для HBox
                HBox.setHgrow(remoteVideoContainer, Priority.ALWAYS);
                HBox.setHgrow(localVideoContainer, Priority.ALWAYS);

                // 🔥 КЛЮЧЕВОЙ МОМЕНТ:
                // 1. Кладём камеры в HBox
                splitModeContainer.getChildren().addAll(remoteVideoContainer, localVideoContainer);
                // 2. Кладём ТОЛЬКО HBox в корневой StackPane
                videoContainer.getChildren().add(splitModeContainer);
                break;
        }

        // 🔥 4. Применяем новые привязки размеров
        setupVideoSizeBindings(mode);

        // 6. Принудительный пересчёт макета
        videoContainer.requestLayout();
    }

    /**
    * Настраивает привязки размеров в зависимости от режима
    */
    private void setupVideoSizeBindings(VideoLayoutMode mode) {
       // Получаем корневой контейнер
       StackPane rootContainer = videoContainer;

       // === Базовые привязки для доступного пространства ===
       DoubleBinding availableWidth = Bindings.createDoubleBinding(() ->
           Math.max(400, rootContainer.getWidth() - 40), // -40px отступы
           rootContainer.widthProperty()
       );
       DoubleBinding availableHeight = Bindings.createDoubleBinding(() ->
           Math.max(300, rootContainer.getHeight() - 40),
           rootContainer.heightProperty()
       );

       // === Привязки для PiP режима ===
       DoubleBinding pipMainWidth = availableWidth;
       DoubleBinding pipMainHeight = availableHeight;

       DoubleBinding pipSmallWidth = Bindings.createDoubleBinding(() ->
           pipMainWidth.get() * 0.25, // 25% от главной
           pipMainWidth
       );
       DoubleBinding pipSmallHeight = Bindings.createDoubleBinding(() ->
           pipSmallWidth.get() * 0.75, // 4:3 пропорция
           pipSmallWidth
       );

       // === Привязки для Split режима (каждая ~48% ширины) ===
       DoubleBinding splitWidth = Bindings.createDoubleBinding(() ->
           (availableWidth.get() - 10) * 0.48, // -10px зазор, 48% каждая
           availableWidth
       );
       DoubleBinding splitHeight = availableHeight;

       // === Применяем привязки ===
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
               // В Split режиме HBox сам управляет шириной через Hgrow,
               // но задаём min/max для корректного сжатия
               bindPane(remoteVideoContainer, splitWidth, splitHeight, true);
               bindPane(localVideoContainer, splitWidth, splitHeight, true);
               break;
       }
   }

   /**
    * Убирает все привязки размеров с панели
    */
   private void unbindAllSizeProperties(StackPane pane) {
       pane.prefWidthProperty().unbind();
       pane.prefHeightProperty().unbind();
       pane.minWidthProperty().unbind();
       pane.minHeightProperty().unbind();
       pane.maxWidthProperty().unbind();
       pane.maxHeightProperty().unbind();
       // Сбрасываем к дефолтным значениям
       pane.setPrefWidth(Region.USE_COMPUTED_SIZE);
       pane.setPrefHeight(Region.USE_COMPUTED_SIZE);
   }

   /**
    * Привязывает размеры панели с опцией "гибкости" (для Split режима)
    */
   private void bindPane(StackPane pane, DoubleBinding width, DoubleBinding height, boolean flexible) {
       pane.prefWidthProperty().bind(width);
       pane.prefHeightProperty().bind(height);

       if (flexible) {
           // В Split режиме позволяем сжиматься/растягиваться
           pane.minWidthProperty().bind(width.multiply(0.5));
           pane.minHeightProperty().bind(height.multiply(0.8));
           pane.setMaxWidth(Double.MAX_VALUE);
           pane.setMaxHeight(Double.MAX_VALUE);
       } else {
           // В PiP маленькая камера — фиксированный размер
           pane.minWidthProperty().bind(width);
           pane.minHeightProperty().bind(height);
           pane.maxWidthProperty().bind(width);
           pane.maxHeightProperty().bind(height);
       }
   }

    /**
     * Настраивает переключение режима по клику
     */
    private void setupClickHandlers() {
        // Клики на камеры переключают режим
        remoteVideoContainer.setOnMouseClicked(this::handleCameraClick);
        localVideoContainer.setOnMouseClicked(this::handleCameraClick);
    }

    /**
     * Обработчик клика: переключает режим
     */
    private void handleCameraClick(MouseEvent event) {
        StackPane clickedPane = (StackPane) event.getSource();

        switch (currentMode) {
            case PIP_PRIMARY_FIRST:
                // Если кликнули на маленькую (камера 2) → она становится главной
                if (clickedPane == localVideoContainer) {
                    currentMode = VideoLayoutMode.PIP_PRIMARY_SECOND;
                } else {
                    // Клик на главную → переход в split
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
                // В режиме split: клик на камеру делает её главной (возврат к PiP)
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


    public void loadCallChatHistory(List<Message> messages) {
        callChatHistory.getItems().setAll(messages);
        // Прокрутка в конец после загрузки
        Platform.runLater(() -> callChatHistory.scrollTo(callChatHistory.getItems().size() - 1));
    }

    private void initChat() {
        callChatHistory.setCellFactory(lv -> new ChatHistoryCell());
        callChatHistory.getStyleClass().add("chat-history");
        callChatHistory.setOnMousePressed(event -> {
            // Передаём фокус полю ввода сообщений
            if (callMessageInput != null) {
                callMessageInput.requestFocus();
            }
            // Позволяем событию идти дальше (чтобы можно было скроллить чат)
            // Если скролл не нужен, можно добавить event.consume();
        });

        // send message button
        sendCallMessageButton.setOnAction(e -> sendCallMessage());
        sendCallMessageButton.getStyleClass().add("send-button");

        callMessageInput.setOnAction(e -> sendCallMessage());
        callMessageInput.getStyleClass().add("message-input");

        messageContainer.getStyleClass().add("mesage-container");

        chatContainer.getStyleClass().add("message-container");
    }


    /**
     * send message to call chat
     */
    private void sendCallMessage() {
        String message = callMessageInput.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        WebRTCManager rtcm = WebRTCManager.getInstance();

        rtcm.sendChatMessage(message);
        callMessageInput.clear();
        // simulateRemoteResponse();
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

        Platform.runLater(() -> {
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
            WebRTCManager.getInstance().cleanup();
        });
    }

    private void openGameChooser() {

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

    private void setupCloseInterceptor(Stage stage) {
        stage.setOnCloseRequest(event -> {
                        WebRTCManager.getInstance().cleanup();
                        closeWindow();
                        WebRTCManager.getInstance().cleanup();
                        event.consume();
                    });
    }

    private void closeWindow() {
        settings.save();

        Platform.runLater(() -> {
            Stage stage = (Stage) endCallButton.getScene().getWindow();
            stage.close();
        });
    }

    /**
    * Добавляет сообщение в историю чата (обратная совместимость со старым кодом)
    */
    public void appendToCallChat(String sender, String content) {
        Message msg = new Message();
        msg.setId(generateMessageId()); // Простая генерация ID
        msg.setChatId(1); // Или динамически
        if (sender.equals(settings.getUserKey())) {
            msg.setSenderPublicKey("Вы");
        } else {
            msg.setSenderPublicKey(sender);
        }
        msg.setContent(content);
        msg.setTime(System.currentTimeMillis() / 1000); // Unix timestamp в секундах

        Platform.runLater(() -> {
            appendToCallChat(msg);
        });
    }

    /**
     * Добавляет готовое сообщение в историю
     */
    public void appendToCallChat(Message message) {
        // Добавляем в конец списка
        callChatHistory.getItems().add(message);

        // 🔥 Прокрутка вниз к новому сообщению
        Platform.runLater(() -> {
            int lastIndex = callChatHistory.getItems().size() - 1;
            if (lastIndex >= 0) {
                callChatHistory.scrollTo(lastIndex);
            }
        });
    }

    /**
     * Простая генерация уникального ID (заглушка)
     * В реальном приложении — использовать базу данных или UUID
     */
    private int generateMessageId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }
}

