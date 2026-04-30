package com.mycompany.javaphone_nir2.controllers;

import com.mycompany.javaphone_nir2.ChatHistoryCell;
import com.mycompany.javaphone_nir2.ContactListCell;
import com.mycompany.javaphone_nir2.models.Contact;
import com.mycompany.javaphone_nir2.models.Message;
import com.mycompany.javaphone_nir2.models.Offer;
import com.mycompany.javaphone_nir2.models.SettingsManager;
import com.mycompany.javaphone_nir2.signaling.SignalingClient;
import com.mycompany.javaphone_nir2.webrtc.WebRTCManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * buttons (Exit, Settings) 6. Opening settings window
 */
public class ChatController {
    /**
     * FXML ui skeleton
     */
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


    /** ObservableMap, that stores contacts, which connected to the signal server  */
    private ObservableMap<String, Contact> contacts;
    private Contact selectedContact;
    private Contact incomingCallContact;

    /** Listeners, which helps ui calculate incoming call notification position */
    private List<ChangeListener<Number>> popupWindowListeners = new ArrayList<>();
    private ChangeListener<Boolean> popupWindowShowingListener;
    private ChangeListener<Boolean> popupWindowFocusListener;
    private boolean isCallPopupActive = false;
    private Popup incomingCallPopup;
    private VBox notificationBox;

    /** SettingsManager stores and save necessary information for ui (splitPane ratio, theme) */
    private final SettingsManager settings = SettingsManager.getInstance();

    /** WebRTCManager let users communicate, used when call is starting */
    private final WebRTCManager webRtcManager = WebRTCManager.getInstance();

    /** Offer stores info about call offer (name, sdp) */
    public Offer offer;

    /** Singleton of this class to use public methods and don`t reproduce invalid objects,
     * which not related to the ui thread */
    private static ChatController instance;
    public static ChatController getInstance() {
        return instance;
    }

    /**
     * This method is automatically called by the FXMLLoader after the FXML file is loaded
     * and all @FXML fields have been injected
     * So this method is key method to init styles, listeners and etc before showing ui
     */
    @FXML public void initialize() {
        setupContactList();

        setupChatUI();
        checkRegistration();
        initSignalingClient();

        instance = this;
    }

    /**
     * This method inits observableHashMap for contacts, sets cells for contacts ListView and styles
     */
    private void setupContactList() {
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

    /** This method set styles for nav bar and contacts and set onClick handlers for nav buttons */
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

        exitButton.getStyleClass().add("header-button");
        exitButton.setOnAction(e -> {
            if(webRtcManager != null) {
                webRtcManager.cleanup();
                }
                if (SignalingClient.getInstance() != null){
                    try {
                        SignalingClient.getInstance().disconnect();
                    } catch (IOException ex) {
                        Logger.getLogger(ChatController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            closeWindow();
        });
    }

    /** This method open settings window when user is not registered */
    private void checkRegistration() {
        if(!settings.isRegistered()) {
            Platform.runLater(() -> {
                openSettings();
            });
        }
    }

    /** This method init signaling client and add handler for notify about incoming call */
    private void initSignalingClient(){
        SettingsManager settings = SettingsManager.getInstance();
        SignalingClient.initialize(settings.getSignalingUrl());

        Platform.runLater(() -> connectToSignaling());

        SignalingClient.getInstance().offerProperty().addListener((obs, oldVal, newVal) -> {
            offer = new Offer(newVal.getSdp(), newVal.getSender());
            initIncomingCall(offer.getSender());
        });
    }

    /** This method connect to signaling client */
    private void connectToSignaling() {
        try {
            SignalingClient.getInstance().connect();
        } catch (Exception e) {
            System.err.println("ERROR WHILE CONNECTING TO THE SIGNALING CLIENT: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setHeaderText("ERROR WHILE CONNECTING TO THE SIGNALING CLIENT");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /** This method get split ratio of splitPane from settingsManager and add handler for split ratio change */
    private void initSplitPane() {
        mainSplitPane.setDividerPositions(settings.getMainSplitRatio());

        mainSplitPane.getStyleClass().add("split-pane");

        for (SplitPane.Divider divider : mainSplitPane.getDividers()) {
            divider.positionProperty().addListener((obs, oldPos, newPos) -> {
                settings.setMainSplitRatio(newPos.doubleValue());
            });
        }
    }

    /** This method set cells for chat, styles of chat elements and handler for mousePress */
    private void initChat() {
        chatHistory.setCellFactory(lv -> new ChatHistoryCell());
        chatHistory.getStyleClass().add("chat-history");
        chatHistory.setOnMousePressed(event -> {
            if (messageInput != null) {
                messageInput.requestFocus();
            }
        });

        messageInput.setOnAction(e -> sendMessage());
        messageInput.getStyleClass().add("message-input");

        sendButton.setOnAction(e -> sendMessage());
        sendButton.getStyleClass().add("send-button");

        messageContainer.getStyleClass().add("message-container");
    }

    /**
     * This method helps init responsive layout
     * @param stage helps to accurately set the event handler on the initialized object
     */
    public void initializeResponsiveLayout(Stage stage) {
        setupCloseInterceptor(stage);
        setupGlobalKeyboardNavigation(stage);
    }

    /**
     * This method lets net module stop work correct and continue closing the window
    *  @param stage helps to accurately set the event handler on the initialized object
    */
    private void setupCloseInterceptor(Stage stage) {
        stage.setOnCloseRequest(event -> {
            if (webRtcManager != null) {
                webRtcManager.cleanup();
            }
            try {
                SignalingClient.getInstance().disconnect();
            } catch (IOException ex) {
                Logger.getLogger(ChatController.class.getName()).log(Level.SEVERE, null, ex);
            }
            closeWindow();
            event.consume();
        });
    }

    /**
    * This method lets user in chat window navigate contacts via pressing alt + arrow up (arrow down)
    * @param Stage stage helps to accurately set the event handler on the initialized object
    */
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
    * This method toggles selection in the contact list with cyclic scrolling
    * @param direction -1 means selecting upper contact, 1 means selecting lower contact
    */
    private void switchChat(int direction) {
        int currentIndex = contactsList.getSelectionModel().getSelectedIndex();
        int size = contactsList.getItems().size();
        if (size == 0) return;

        // cycle navigation
        int nextIndex = currentIndex + direction;
        if (nextIndex < 0) nextIndex = size - 1;
        else if (nextIndex >= size) nextIndex = 0;

        // selecting new picked chat in contactsList
        contactsList.getSelectionModel().select(nextIndex);
        contactsList.scrollTo(nextIndex);
        messageInput.requestFocus();
    }

    /** This method demonstrate notification about call  */
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

    /** This method shows notification via popup */
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

    /** This method responsible for notificaion pos update */
    private void updateCallPopupPosition() {
        if (callButton == null || callButton.getScene() == null) {
            return;
        }

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

    /** This method responsible for hiding incoming call notification */
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

        incomingCallPopup.hide();
        }
    }

    /** This method responsible for accept the call */
    private void acceptCall() {
        hideIncomingCallNotification();
        startVideoCallWithContact(incomingCallContact);
    }

    /** This method responsible for handle call accept */
    public void handleCallAccepted() {
        Platform.runLater(() -> {
            startVideoCallWithContact(selectedContact);
        });
    }

    /** This method responsible for handle call rejected */
    private void handleCallRejected() {
        hideIncomingCallNotification();

        offer.clear();
    }

    /** This method responsible for starting video call with contact
     * @param contact contact to start call with
     */
    private void startVideoCallWithContact(Contact contact) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mycompany/javaphone_nir2/fxml/video_call.fxml") //the path is searched from the classpath
            );

            Scene scene = new Scene(loader.load(), 1200, 700);
            scene.getStylesheets().add(getClass().getResource("dark".equals(settings.getTheme()) ?
                                "/com/mycompany/javaphone_nir2/css/video_call_dark.css"
                                : "/com/mycompany/javaphone_nir2/css/video_call.css").toExternalForm());

            //listener for theme change
            settings.themeProperty().addListener((obs, oldTheme, newTheme) -> {
                scene.getStylesheets().removeIf(s -> s.contains("/com/mycompany/javaphone_nir2/css/video_call_dark.css")
                        || s.contains("/com/mycompany/javaphone_nir2/css/video_call.css"));
                scene.getStylesheets().add(getClass().getResource("dark".equals(newTheme) ?
                           "/com/mycompany/javaphone_nir2/css/video_call_dark.css"
                                : "/com/mycompany/javaphone_nir2/css/video_call.css").toExternalForm());
            });

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

    /** This method responsible for add contact to the UI */
    public void addContact(Contact contact) {
        System.out.println("GOT CONTACT TO UI");
        System.out.println(contact.getKey());

        contacts.put(contact.getKey(), contact);
        contactsList.setItems(FXCollections.observableArrayList(contacts.values()));
        contactsList.refresh();
    }

    /** This method responsible for init and show call notification
     * @param offer object that include sdp and sender name
     */
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

    /** This method responsible for init and show call notification
     * @param callerKey caller id in signal server
     */
    public void initIncomingCall(String callerKey) {
        incomingCallContact = contacts.getOrDefault(callerKey, null);
        if (incomingCallContact != null) {
            Platform.runLater( () -> {
                initIncomingCallNotification();
                showIncomingCallNotification();
            });
        }
    }

    /** This method responsible for components initialization of incoming call notification popup */
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

    /** This method responsible for updating chat with selected contact */
    private void updateChatPanel() {
        if (selectedContact != null) {
            chatTitleLabel.setText("Чат с " + selectedContact.getName());
        }
    }

    /** This method responsible for load chat history with list
     * @param messages is list of messages in this chat
     */
    public void loadChatHistory(List<Message> messages) {
        chatHistory.getItems().setAll(messages);

        Platform.runLater(() -> chatHistory.scrollTo(chatHistory.getItems().size() - 1));
    }

    /** This method responsible for send message to focused contact in contact list */
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
    }

    /** This method responsible for handle message
     * @param sender
     * @param content message
     */
    public void handleMessage(String sender, String content) {
        Platform.runLater(() -> {
            appendToChat(sender, content);
        });
    }

    /** This method responsible for simulate chat response */
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

    /** This method responsible for close window properly */
    private void closeWindow() {
        settings.save();

        Platform.runLater(() -> {
            Stage stage = (Stage) exitButton.getScene().getWindow();
            stage.close();
            Platform.exit();
        });
    }

    /** This method responsible for starting videocall with focused contact in contact list */
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
    }

    /** This method responsible for add message to chat
     * @param sender
     * @param content message
     */
   public void appendToChat(String sender, String content) {
       Message msg = new Message();
       msg.setId(generateMessageId());
       msg.setChatId(1);
       msg.setSenderPublicKey(sender);
       msg.setContent(content);
       msg.setTime(System.currentTimeMillis() / 1000); // Unix timestamp in seconds

       appendToChat(msg);
   }

   /** This method responsible for adding message to history
    * @param message (id, chatId, sender public key, content, time, attachments)
    */
   public void appendToChat(Message message) {
       chatHistory.getItems().add(message);

       Platform.runLater(() -> {
           int lastIndex = chatHistory.getItems().size() - 1;
           if (lastIndex >= 0) {
               chatHistory.scrollTo(lastIndex);
           }
       });
   }

   /** This method responsible for  simple generation message id */
   private int generateMessageId() {
       return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
   }

   /** This method responsible for open settings window */
    private void openSettings() {
         try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mycompany/javaphone_nir2/fxml/settings.fxml") //the path is searched from the classpath
            );

            Scene scene = new Scene(loader.load(), 500, 465);
            scene.getStylesheets().add(getClass().getResource("dark".equals(settings.getTheme()) ?
                                "/com/mycompany/javaphone_nir2/css/settings_dark.css"
                                : "/com/mycompany/javaphone_nir2/css/settings.css").toExternalForm());

            //listener for theme property
            settings.themeProperty().addListener((obs, oldTheme, newTheme) -> {
                scene.getStylesheets().removeIf(s -> s.contains("/com/mycompany/javaphone_nir2/css/settings_dark.css")
                        || s.contains("/com/mycompany/javaphone_nir2/css/settings.css"));
                scene.getStylesheets().add(getClass().getResource("dark".equals(newTheme) ?
                           "/com/mycompany/javaphone_nir2/css/settings_dark.css"
                                : "/com/mycompany/javaphone_nir2/css/settings.css").toExternalForm());
            });

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

