package com.mycompany.javaphone_nir2.controllers;

import com.mycompany.javaphone_nir2.ChatHistoryCell;
import com.mycompany.javaphone_nir2.ContactListCell;
import com.mycompany.javaphone_nir2.models.Contact;
import com.mycompany.javaphone_nir2.models.Message;
import com.mycompany.javaphone_nir2.models.Offer;
import com.mycompany.javaphone_nir2.models.SettingsManager;
import com.mycompany.javaphone_nir2.signaling.SignalingClient;
import com.mycompany.javaphone_nir2.webrtc.WebRTCManager;
import java.awt.event.InputEvent;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Controller for main chat window
 *
 * Responsible for: 1. Managing the contact list 2. Displaying message history
 * 3. Sending new messages 4. Switching to the video call window 5. Handling nav
 * buttons (Exit, Settings)
 */
public class ChatController {
    @FXML private ListView<Contact> contactsList;
    @FXML private HBox headerContainer;
    @FXML private VBox contactsContainer;
    @FXML private HBox messageContainer;
    @FXML private ListView<Message> chatHistory;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private Button callButton;
    @FXML private Button settingsButton;
    @FXML private Button exitButton;
    @FXML private Label chatTitleLabel;
    @FXML private SplitPane mainSplitPane;

    private ObservableMap<String, Contact> contacts;
    private List<ChangeListener<Number>> popupWindowListeners = new ArrayList<>();
    private ChangeListener<Boolean> popupWindowShowingListener;
    private ChangeListener<Boolean> popupWindowFocusListener;
    private Contact selectedContact;
    private Contact incomingCallContact;
    private boolean isCallPopupActive = false;

    private Popup incomingCallPopup;
    private VBox notificationBox;

    private final SettingsManager settings = SettingsManager.getInstance();

    private final WebRTCManager webRtcManager = WebRTCManager.getInstance();

    public Offer offer;
    
    private static ChatController instance;
    
    public static ChatController getInstance() {
        return instance;
    }
    
    @FXML
    public void initialize() {
        setupContactList();
        //incomingCallContact = contacts.get(0);  // first contact

        setupChatUI();
        checkRegistration();
        initSignalingClient();

        Platform.runLater(() -> {
            appendToChat("Alice", "Привет! Это тестовое сообщение.");
            appendToChat("Bob", "Отлично, работает! 🎉");
            appendToChat("You", "Да, интерфейс стал намного лучше.");
        });
        
        instance = this;
    }

    private void initSignalingClient(){
        SettingsManager settings = SettingsManager.getInstance();
        SignalingClient.initialize(settings.getSignalingUrl());

        Platform.runLater(() -> connectToSignaling());

        SignalingClient.getInstance().offerProperty().addListener((obs, oldVal, newVal) -> {
            offer = new Offer(newVal.getSdp(), newVal.getSender());
            initIncomingCall(offer.getSender());
        });
    }

    private void setupCloseInterceptor(Stage stage) {
        stage.setOnCloseRequest(event -> {
                        closeWindow();
                        event.consume();
                    });
    }

    private void setupGlobalKeyboardNavigation(Stage stage) {
        if (stage.getScene() != null && stage.isShowing()) {
            Parent root = stage.getScene().getRoot();
            root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.isAltDown() && (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN)) {
                    switchChat(event.getCode() == KeyCode.UP ? -1 : 1);
                    event.consume();
                }
            });
        }
    }

    /**
    * Переключает выделение в списке чатов с циклической прокруткой
    */
    private void switchChat(int direction) {
        int currentIndex = contactsList.getSelectionModel().getSelectedIndex();
        int size = contactsList.getItems().size();
        if (size == 0) return;

        // Циклическая навигация: внизу → наверх, вверху → вниз
        int nextIndex = currentIndex + direction;
        if (nextIndex < 0) nextIndex = size - 1;
        else if (nextIndex >= size) nextIndex = 0;

        // Выделяем новый чат → сработает твой существующий слушатель выделения
        contactsList.getSelectionModel().select(nextIndex);
        contactsList.scrollTo(nextIndex);

        // Опционально: возвращаем фокус в поле ввода для мгновенного набора
        messageInput.requestFocus();
    }

    // Action methods
    private void connectToSignaling() {
        try {
            SignalingClient.getInstance().connect();
        } catch (Exception e) {
            System.err.println("ERROR WHILE CONNECTING TO THE SIGNALING CLIENT: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERORRRRR");
            alert.setHeaderText("ERROR WHILE CONNECTING TO THE SIGNALING CLIENT");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
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


            //deleting focus listener
            if (popupWindowFocusListener != null) {
                window.focusedProperty().removeListener(popupWindowFocusListener);
                popupWindowFocusListener = null;
            }
        }

        incomingCallPopup.hide();

        }
    }

    /**
     * handle call accept
     *
     */
    private void acceptCall() {
        hideIncomingCallNotification();
//        appendToChat("System", "✅ Вы приняли звонок от " + incomingCallContact.getName());
        webRtcManager.handleOffer(offer.getSdp(), offer.getSender());
        startVideoCallWithContact(incomingCallContact);
    }
    
    public void handleCallAccepted() {
        Platform.runLater(() -> {
            startVideoCallWithContact(selectedContact);
        });
    }

    /**
     * handle call rejected
     *
     */
    private void handleCallRejected() {
        hideIncomingCallNotification();

        offer.clear();
    }

    /**
     * starting video call with contact
     * @param contact contact to start call with
     */
    private void startVideoCallWithContact(Contact contact) {
        try {
            //здесь уже вызываем sendAnswer с sdp и клиент id

//            SignalingClient.getInstance().sendAccept(offer.getSdp(), offer.getSender());

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

    private void checkRegistration() {
        if(settings.isRegistered()) {
            //starting timer to demonstrate popup
//            scheduleIncomingCallTimer();
        } else {
            Platform.runLater(() -> {
                openSettings();
            });
        }
    }

    /**
     * upload contacts list from db and set it on ui
     */
    private void setupContactList() {
        //upload smthng here and then dispay it in contacts list.
        //ask from db manager or smthng else about contacts, get list to display and then display

        contacts = FXCollections.observableHashMap();

        contactsList.setItems(FXCollections.observableArrayList(contacts.values()));
        contactsList.setCellFactory(param -> new ContactListCell());
        contactsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedContact = newVal;
                updateChatPanel();
            }
        });

        contactsList.getStyleClass().add("contacts-list");
    }

    public void addContact(Contact contact) {
        System.out.println("GOT CONTACT TO UI");
        System.out.println(contact.getKey());
        
        contacts.put(contact.getKey(), contact);
        contactsList.setItems(FXCollections.observableArrayList(contacts.values()));
        contactsList.refresh();
    }
    
    /**
     * setuping chat ui
     */
    private void setupChatUI() {
        initSplitPane();
        initChat();

        chatTitleLabel.getStyleClass().add("chat-title-label");

        headerContainer.getStyleClass().add("header-container");

        contactsContainer.getStyleClass().add("contacts-container");

        settingsButton.setOnAction(e -> openSettings());
        settingsButton.getStyleClass().add("header-button");

        callButton.setOnAction(e -> startVideoCall());
        callButton.getStyleClass().add("header-button");

        exitButton.setOnAction(e -> closeWindow());
        exitButton.getStyleClass().add("header-button");
    }

    private void initSplitPane() {
        mainSplitPane.setDividerPositions(settings.getMainSplitRatio());

        // 2. Слушаем изменение разделителя
        for (SplitPane.Divider divider : mainSplitPane.getDividers()) {
            divider.positionProperty().addListener((obs, oldPos, newPos) -> {
                // Сохраняем в модель (реактивно вызовет update data)
                settings.setMainSplitRatio(newPos.doubleValue());
            });
        }
    }

    private void initChat() {
        chatHistory.setCellFactory(lv -> new ChatHistoryCell());
//        chatHistory.setFixedCellSize(80);
//        chatHistory.setFixedCellSize(100);
        chatHistory.getStyleClass().add("chat-history");
        chatHistory.setOnMousePressed(event -> {
            // Передаём фокус полю ввода сообщений
            if (messageInput != null) {
                messageInput.requestFocus();
            }
            // Позволяем событию идти дальше (чтобы можно было скроллить чат)
            // Если скролл не нужен, можно добавить event.consume();
        });

        messageInput.setOnAction(e -> sendMessage());
        messageInput.getStyleClass().add("message-input");

        sendButton.setOnAction(e -> sendMessage());
        sendButton.getStyleClass().add("send-button");

        messageContainer.getStyleClass().add("message-container");
    }
    
    public void initIncomingCall(Offer offer) {
        this.offer = offer;
        String callerKey = offer.getSender();
        
        incomingCallContact = contacts.getOrDefault(callerKey, null);
        if (incomingCallContact != null) {
            Platform.runLater( () -> {
                initIncomingCallNotification();
                showIncomingCallNotification();
            });
        }
    }
    
    public void initIncomingCall(String callerKey) {
        incomingCallContact = contacts.getOrDefault(callerKey, null);
        if (incomingCallContact != null) {
            Platform.runLater( () -> {
                initIncomingCallNotification();
                showIncomingCallNotification();
            });
        }
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
            acceptCall();
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
        }
    }

    public void loadChatHistory(List<Message> messages) {
        chatHistory.getItems().setAll(messages);
        // Прокрутка в конец после загрузки
        Platform.runLater(() -> chatHistory.scrollTo(chatHistory.getItems().size() - 1));
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

        SignalingClient sc = SignalingClient.getInstance();
        try {
            sc.sendDM(selectedContact.getKey(), message);
            appendToChat("Вы", message);
            messageInput.clear();
        } catch (IOException ex) {
            System.getLogger(ChatController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
 
        // simulateResponse();
    }
    
    public void handleMessage(String sender, String content) {
        Platform.runLater(() -> {
            appendToChat(sender, content);
        });
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

    public void initializeResponsiveLayout(Stage stage) {
        setupCloseInterceptor(stage);
        setupGlobalKeyboardNavigation(stage);
    }

    private void closeWindow() {
        settings.save();

        Platform.runLater(() -> {
            Stage stage = (Stage) exitButton.getScene().getWindow();
            stage.close();
            Platform.exit();
        });
        //if wanna wait for threads, but not stopping ui - use next lines
//        Timeline delay = new Timeline(new KeyFrame(
//            Duration.seconds(1),
//            e -> {
//                Stage stage = (Stage) discardButton.getScene().getWindow();
//                stage.close();
//            }
//        ));
//        delay.play();
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
        System.out.println(selectedContact.getKey());
        webRtcManager.startCall(selectedContact.getKey());
//        startVideoCallWithContact(selectedContact);
    }

    /**
    * Добавляет сообщение в историю чата (обратная совместимость со старым кодом)
    */
   public void appendToChat(String sender, String content) {
       Message msg = new Message();
       msg.setId(generateMessageId()); // Простая генерация ID
       msg.setChatId(1); // Или динамически
       msg.setSenderPublicKey(sender);
       msg.setContent(content);
       msg.setTime(System.currentTimeMillis() / 1000); // Unix timestamp в секундах

       appendToChat(msg);
   }

   /**
    * Добавляет готовое сообщение в историю
    */
   public void appendToChat(Message message) {
       // Добавляем в конец списка
       chatHistory.getItems().add(message);

       // 🔥 Прокрутка вниз к новому сообщению
       Platform.runLater(() -> {
           int lastIndex = chatHistory.getItems().size() - 1;
           if (lastIndex >= 0) {
               chatHistory.scrollTo(lastIndex);
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

    private void openSettings() {
         try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mycompany/javaphone_nir2/fxml/settings.fxml") //the path is searched from the classpath
            );

            Scene scene = new Scene(loader.load(), 500, 465);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javaphone_nir2/css/settings.css").toExternalForm());


            Stage settingsStage = new Stage();
            settingsStage.setTitle("WebCommunicator - Settings");
            settingsStage.setScene(scene);
            settingsStage.setMinWidth(500);
            settingsStage.setMinHeight(465);

            SettingsController controller = loader.getController();
            controller.initializeResponsiveLayout(settingsStage);

            settingsStage.show();

            settingsStage.requestFocus();

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке окна настроек: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Не удалось открыть окно настроек");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}

