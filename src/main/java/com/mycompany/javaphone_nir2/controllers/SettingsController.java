package com.mycompany.javaphone_nir2.controllers;

import com.mycompany.javaphone_nir2.logging.SessionLogger;
import com.mycompany.javaphone_nir2.models.SettingsManager;
import java.io.File;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Controller for settings window
 *
 * Responsible for: 1. Uploading avatar 2. Setting nickname
 * 3. Setting url of signaling server 4. Setting user key
 * 5. Setting application theme 6. Turning on/off notifications
 * 7. Setting microphone, speaker, camera settings
 * 8. Validating input info
 * 9. Saving settings
 */

public class SettingsController {
    /**
     * FXML ui skeleton
     */
    @FXML private Label audioBitrateLabel;
    @FXML private TextField audioBitrateField;
//    @FXML private ImageView avatarView;
    @FXML private Label avatarView;
    @FXML private Label cameraLabel;
    @FXML private ComboBox<String> cameraComboBox;
    @FXML private Button discardButton;
    @FXML private TextField keyField;
    @FXML private Label keyLabel;
    @FXML private VBox mainContainer;
    @FXML private StackPane gradientBackgroundContainer;
    @FXML private Label microphoneLabel;
    @FXML private ComboBox<String> microphoneComboBox;
    @FXML private Label microphoneVolumeLabel;
    @FXML private Slider microphoneVolumeSlider;
    @FXML private TextField nicknameField;
    @FXML private Label nicknameLabel;
    @FXML private Label notificationsLabel;
    @FXML private HBox notificationsSliderContainer;
    @FXML private Label onNotificationsLabel;
    @FXML private Label offNotificationsLabel;
    @FXML private ToggleButton notificationsToggleButtonSelector;
    @FXML private Circle notificationsToggleButtonSelectorCircle;
    @FXML private Button regenerateKeyButton;
    @FXML private Button saveButton;
    @FXML private Label speakerLabel;
    @FXML private ComboBox<String> speakerComboBox;
    @FXML private Label speakerVolumeLabel;
    @FXML private Slider speakerVolumeSlider;
    @FXML private Label themeLabel;
    @FXML private HBox themeSliderContainer;
    @FXML private Label darkThemeLabel;
    @FXML private Label lightThemeLabel;
    @FXML private ToggleButton themeToggleButtonSelector;
    @FXML private Circle themeToggleButtonSelectorCircle;
    @FXML private Button uploadAvatarButton;
    @FXML private Label uploadedLabel;
    @FXML private Label uploadInfoLabel;
    @FXML private TextField urlField;
    @FXML private Label urlLabel;
    @FXML private Label videoBitrateLabel;
    @FXML private TextField videoBitrateField;

    /** SettingsManager stores and saves settings */
    private SettingsManager settings;

    /** Logger saves session information into log */
    private final SessionLogger logger = SessionLogger.getInstance();

    /** SettingsSnapshot necessary  to realize transactional mechanism
     * in settings window (when button cancel clicked - transaction cancelling) */
    private SettingsSnapshot initialSnapshot;

    /**
     * This method is automatically called by the FXMLLoader after the FXML file is loaded
     * and all @FXML fields have been injected
     * So this method is key method to init styles, listeners and etc before showing ui
     */
    @FXML public void initialize() {
        logger.log("Settings window initializing");

        settings = SettingsManager.getInstance();

        logger.log("Settings window: initializing settings snapshot");
        initialSnapshot = SettingsSnapshot.from(settings);

        setupSettingsUI();
        bindReactiveFields();
        setupFieldValidation();
        loadNonReactiveValues();
    }

    /** This method set styles and onClick handlers */
    private void setupSettingsUI() {
        logger.log("Settings window: setupping settings UI");

        mainContainer.getStyleClass().add("main-container");
        gradientBackgroundContainer.getStyleClass().add("gradient-background-container");

        uploadInfoLabel.getStyleClass().add("text-label");
        uploadedLabel.getStyleClass().add("text-label");
        nicknameLabel.getStyleClass().add("text-label");
        urlLabel.getStyleClass().add("text-label");
        keyLabel.getStyleClass().add("text-label");
        themeLabel.getStyleClass().add("text-label");
        notificationsLabel.getStyleClass().add("text-label");
        microphoneLabel.getStyleClass().add("text-label");
        microphoneVolumeLabel.getStyleClass().add("text-label");
        speakerLabel.getStyleClass().add("text-label");
        speakerVolumeLabel.getStyleClass().add("text-label");
        audioBitrateLabel.getStyleClass().add("text-label");
        cameraLabel.getStyleClass().add("text-label");
        videoBitrateLabel.getStyleClass().add("text-label");

        microphoneComboBox.getStyleClass().add("settings-text-field");
        speakerComboBox.getStyleClass().add("settings-text-field");
        cameraComboBox.getStyleClass().add("settings-text-field");

        nicknameField.getStyleClass().add("settings-text-field");
        urlField.getStyleClass().add("settings-text-field");
        keyField.getStyleClass().add("settings-text-field");
        audioBitrateField.getStyleClass().add("settings-text-field");
        videoBitrateField.getStyleClass().add("settings-text-field");

        microphoneVolumeSlider.getStyleClass().add("slider-container");
        speakerVolumeSlider.getStyleClass().add("slider-container");

        notificationsSliderContainer.getStyleClass().add("slider-container");
        onNotificationsLabel.getStyleClass().add("text-label");
        offNotificationsLabel.getStyleClass().add("text-label");
        notificationsToggleButtonSelectorCircle.getStyleClass().add("switch-thumb");
        notificationsToggleButtonSelector.getStyleClass().add("switch-toggle");

        themeSliderContainer.getStyleClass().add("slider-container");
        darkThemeLabel.getStyleClass().add("text-label");
        lightThemeLabel.getStyleClass().add("text-label");
        themeToggleButtonSelectorCircle.getStyleClass().add("switch-thumb");
        themeToggleButtonSelector.getStyleClass().add("switch-toggle");

        regenerateKeyButton.getStyleClass().add("settings-button");

        discardButton.setOnAction(e -> onDiscardButtonClicked());
        discardButton.getStyleClass().add("settings-button");

        uploadAvatarButton.getStyleClass().add("settings-button");
        uploadAvatarButton.setOnAction(e -> uploadAvatar());

        saveButton.getStyleClass().add("settings-button");
        saveButton.setOnAction(e -> saveSettings());
    }

    /** This method saves settings */
    private void saveSettings() {
        logger.log("Settings window: saving settings");

        SettingsManager.getInstance().save();

        closeWindow();
    }

    /**
     * This method helps init responsive layout
     * @param stage helps to accurately set the event handler on the initialized object
     */
    public void initializeResponsiveLayout(Stage stage) {
        logger.log("Settings window: called func initializeResponsiveLayout");

        setupCloseInterceptor(stage);
    }

    /**
     * This method lets net module stop work correct and continue closing the window
    *  @param stage helps to accurately set the event handler on the initialized object
    */
    private void setupCloseInterceptor(Stage stage) {
        logger.log("Settings window: override the window closing method");

        stage.setOnCloseRequest(event -> {
            onDiscardButtonClicked();
            event.consume();
        });
    }

    /** This method response for binding reactive fields to SettingsManager fields */
    private void bindReactiveFields() {
        logger.log("Settings window: binding reactive fields");

        nicknameField.textProperty().bindBidirectional(settings.nicknameProperty());

        keyField.textProperty().bindBidirectional(settings.userKeyProperty());
        bindIntegerField(audioBitrateField, settings.audioBitrateProperty(), 32, 320, "audioBitrate");
        bindIntegerField(videoBitrateField, settings.cameraBitrateProperty(), 100, 10000, "cameraBitrate");

        microphoneVolumeSlider.valueProperty().bindBidirectional(settings.microphoneVolumeProperty());
        speakerVolumeSlider.valueProperty().bindBidirectional(settings.speakerVolumeProperty());

        notificationsToggleButtonSelector.selectedProperty().bindBidirectional(settings.notificationsEnabledProperty());

        bindThemeToggle(themeToggleButtonSelector, settings.themeProperty());
    }

    /** This method responsible for manual bidirectional bindings for theme */
    private void bindThemeToggle(ToggleButton toggle, StringProperty themeProperty) {
        logger.log("Settings window: binding theme toggle");

        toggle.setSelected("light".equals(themeProperty.get()));

        themeProperty.addListener((obs, oldVal, newVal) -> {
            boolean shouldBeSelected = "light".equals(newVal);
            if (toggle.isSelected() != shouldBeSelected) {
                toggle.setSelected(shouldBeSelected);
            }
        });

        toggle.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            String newTheme = isNowSelected ? "light" : "dark";
            if (!newTheme.equals(themeProperty.get())) {
                themeProperty.set(newTheme);
            }
        });
    }

    /**
     * This method responsible for bidirectional bindings for integer fields
     * and validating this fields
     * @param textField integer field
     * @param intProperty model property
     * @param min min value
     * @param max max value
     */
    private void bindIntegerField(TextField textField, IntegerProperty intProperty, int min, int max, String fieldName) {
        logger.log("Settings window: binding integer field");

        textField.setText(String.valueOf(intProperty.get()));

        textField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("\\d+")) {
                return change;
            }
            return null;
        }));

        intProperty.addListener((obs, oldVal, newVal) -> {
            if (!textField.isFocused()) {
                textField.setText(String.valueOf(newVal));
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try {
                    String text = textField.getText().trim();
                    if (text.isEmpty()) {
                        textField.setText(String.valueOf(intProperty.get()));
                        return;
                    }
                    int value = Integer.parseInt(text);

                    if (value < min || value > max) {
                        showFieldError(textField, "Допустимый диапазон: " + min + "–" + max);
                        textField.setText(String.valueOf(intProperty.get()));
                        return;
                    }

                    Optional<String> error = settings.getValidationError(fieldName, value);
                    if (error.isPresent()) {
                        showFieldError(textField, error.get());
                        textField.setText(String.valueOf(intProperty.get()));
                        return;
                    }

                    intProperty.set(value);
                } catch (NumberFormatException e) {
                    showFieldError(textField, "Введите целое число");
                    textField.setText(String.valueOf(intProperty.get()));
                }
            }
        });
    }

    /** This method responsible for validation text fields */
    private void setupFieldValidation() {
        logger.log("Settings window: setupping field validation");

        nicknameField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            // allow empty characters, letters (latin/cyr), numbers, spaces, underscores
            if (newText.isEmpty() || newText.matches("^[a-zA-Z0-9_\\s\\u0400-\\u04FF]{0,20}$")) {
                return change;
            }
            return null;
        }));

        nicknameField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String value = nicknameField.getText();
                Optional<String> error = settings.validateNickname(value);
                if (error.isPresent()) {
                    showFieldError(nicknameField, error.get());
                    // rollback to the last valid value from the model
                    nicknameField.setText(settings.getNickname());
                }
            }
        });

        // simple TextFormatter: Allows EVERYTHING Except Line Breaks and Tabs
        urlField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            // block only control characters that break URLs.
            if (newText != null && newText.matches(".*[\\n\\r\\t].*")) {
                return null;
            }
            return change;
        }));

        urlField.setText(settings.getSignalingUrl());

        urlField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) { // only when user ended input
                String url = urlField.getText().trim();
                Optional<String> error = settings.validateSignalingUrl(url);

                if (error.isPresent()) {
                    showFieldError(urlField, error.get());
                    urlField.setText(settings.getSignalingUrl());
                } else {
                    settings.setSignalingUrl(url);
                    urlField.setStyle("");
                    Tooltip.uninstall(urlField, null);
                }
            }
        });

        // user key - max length 128
        keyField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().length() <= 128) {
                return change;
            }
            return null;
        }));

        keyField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                Optional<String> error = settings.validateUserKey(keyField.getText());
                if (error.isPresent()) {
                    showFieldError(keyField, error.get());
                    keyField.setText(settings.getUserKey());
                }
            }
        });
    }

    /** This method responsible for showing validation error: tooltip and field color */
    private void showFieldError(TextField field, String message) {
        logger.log("Settings window: showing field error via tooltip");

        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 12px;");
        Tooltip.install(field, tooltip);
        field.setStyle("-fx-border-color: #ff6b6b; -fx-border-width: 2px; -fx-border-radius: 4px;");

        // remove the backlight when the user starts to correct
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                field.setStyle("");
                Tooltip.uninstall(field, tooltip);
            }
        });
    }

    /** This method responsible for load and set listeners for non reactive fields */
    private void loadNonReactiveValues() {
        logger.log("Settings window: loading non reactive values to the UI");

        showAvatar();
        setMicrophoneComboBox();
        setSpeakerComboBox();
        setCameraComboBox();

        microphoneComboBox.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                settings.setMicrophone(newVal);
            }
        });
        speakerComboBox.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                settings.setSpeaker(newVal);
            }
        });
        cameraComboBox.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                settings.setCamera(newVal);
            }
        });
    }

    /** This method responsible for set available microphones on ui */
    private void setMicrophoneComboBox() {
        logger.log("Settings window: setting microphone combo box");

        List<String> devices = settings.getAvailableMicrophones();
        microphoneComboBox.getItems().addAll(devices);

        String saved = settings.getMicrophone();
        if (saved != null && devices.contains(saved)) {
            microphoneComboBox.setValue(saved);
        } else {
            microphoneComboBox.setValue("Default");
        }

        microphoneComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                settings.setMicrophone(newVal);
            }
        });
    }

    /** This method responsible for set available speakers on ui */
    private void setSpeakerComboBox() {
        logger.log("Settings window: setting speaker combo box");

        List<String> devices = settings.getAvailableSpeakers();
        speakerComboBox.getItems().addAll(devices);

        String saved = settings.getSpeaker();
        if (saved != null && devices.contains(saved)) {
            speakerComboBox.setValue(saved);
        } else {
            speakerComboBox.setValue("Default");
        }

        speakerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                settings.setSpeaker(newVal);
            }
        });
    }

    /** This method responsible for set available cameras on ui */
    private void setCameraComboBox() {
        logger.log("Settings window: setting camera combo box");

        List<String> devices = settings.getAvailableCameras();
        cameraComboBox.getItems().addAll(devices);

        String saved = settings.getCamera();
        if (saved != null && devices.contains(saved)) {
            cameraComboBox.setValue(saved);
        } else {
            cameraComboBox.setValue("Default");
        }

        cameraComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                settings.setCamera(newVal);
            }
        });
    }

    /** This method responsible for show avatar */
    private void showAvatar() {
        logger.log("Settings window: showing avatar");
//        if (settings.isRegistered()) {
//            String path = settings.getPathToAvatar();
//            if (path != null && !path.isEmpty()) {
//                Path p = Path.of(path);
//                if (p.toFile().exists()) {
//                    Image avatarImage = new Image(p.toUri().toString(), 162, 162, true, true);
//                    if (!avatarImage.isError()) {
//                        avatarView.setImage(avatarImage);
//                        uploadedLabel.setText("Выбранный файл: " + p.getFileName());
//                        return;
//                    }
//                }
//            }
//        }

        //could done label instead of imageview for better handling
        avatarView.setFont(Font.font("System", 89));
        avatarView.setTextFill(Color.BLACK);
        avatarView.setStyle("-fx-alignment: CENTER; "
                + "-fx-background-color: #dadef7;"
                + "-fx-border-color: #2c3e50;"
                + "-fx-border-radius: 0;"
                + "-fx-border-width: 5");

        String firstName = settings.getNickname().substring(0, 1).toUpperCase();
        avatarView.setText(firstName);

        uploadedLabel.setText("Аватар не установлен");
    }

    /** This method responsible for upload avatar */
    private void uploadAvatar() {
        logger.log("Settings window: uploading avatar");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение для аватара");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        Window ownerWindow = uploadAvatarButton.getScene() != null
                ? uploadAvatarButton.getScene().getWindow()
                : null;

        File selectedFile = fileChooser.showOpenDialog(ownerWindow);
        if (selectedFile != null && settings.uploadAvatar(selectedFile)) {
            Platform.runLater(this::showAvatar);
        } else if (selectedFile != null) {
            uploadedLabel.setText("❌ Ошибка загрузки");
        }
    }

    /** This method responsible for handling discard button click */
    private void onDiscardButtonClicked() {
        logger.log("Settings window: handling discard button clicked");

        logger.log("Settings window: revert settings to the previous");
        initialSnapshot.revert(settings);
        closeWindow();
    }

    /** This method responsible for close window properly */
    private void closeWindow() {
        logger.log("Settings window: user requested closing window");

        Platform.runLater(() -> {
            Stage stage = (Stage) discardButton.getScene().getWindow();
            stage.close();
        });
    }

    /**
     * an immutable snapshot of the settings for implementing the Rollback pattern.
     * used when opening the settings window and when undoing changes.
     */
    private static record SettingsSnapshot(
            String nickname, String signalingUrl, String userKey, String theme, String pathToAvatar,
            String microphone, String speaker, String camera,
            boolean notificationsEnabled,
            int microphoneVolume, int speakerVolume, int audioBitrate, int cameraBitrate) {


        /**
        * factory method: creates a snapshot of the current state of the settings manager
        */
        static SettingsSnapshot from(SettingsManager manager) {
            return new SettingsSnapshot(
                    manager.getNickname(),
                    manager.getSignalingUrl(),
                    manager.getUserKey(),
                    manager.getTheme(),
                    manager.getPathToAvatar(),
                    manager.getMicrophone(),
                    manager.getSpeaker(),
                    manager.getCamera(),
                    manager.isNotificationsEnabled(),
                    manager.getMicrophoneVolume(),
                    manager.getSpeakerVolume(),
                    manager.getAudioBitrate(),
                    manager.getCameraBitrate()
            );
        }

        /**
        * Restores (rolls back) the settings manager to the state
        * of this snapshot
        */
        void revert(SettingsManager manager) {
            manager.setNickname(nickname);
            manager.setSignalingUrl(signalingUrl);
            manager.setUserKey(userKey);
            manager.setTheme(theme);
            manager.setPathToAvatar(pathToAvatar);
            manager.setMicrophone(microphone);
            manager.setSpeaker(speaker);
            manager.setCamera(camera);
            manager.setNotificationsEnabled(notificationsEnabled);
            manager.setMicrophoneVolume(microphoneVolume);
            manager.setSpeakerVolume(speakerVolume);
            manager.setAudioBitrate(audioBitrate);
            manager.setCameraBitrate(cameraBitrate);
        }
    }
}
