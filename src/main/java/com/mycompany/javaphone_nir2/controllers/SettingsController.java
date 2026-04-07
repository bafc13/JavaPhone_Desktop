package com.mycompany.javaphone_nir2.controllers;

import com.mycompany.javaphone_nir2.models.SettingsManager;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

public class SettingsController {

    @FXML private Label audioBitrateLabel;
    @FXML private TextField audioBitrateField;
    @FXML private ImageView avatarView;
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
    @FXML private Label signalingServerIPLabel;
    @FXML private TextField signalingServerIPField;
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

    private SettingsManager settings;

    private SettingsSnapshot initialSnapshot;

    @FXML
    public void initialize() {
        settings = SettingsManager.getInstance();

        initialSnapshot = SettingsSnapshot.from(settings);

        setupSettingsUI();      // CSS-классы и обработчики кнопок
        bindReactiveFields();   // 🔥 Реактивная привязка к настройкам
        setupFieldValidation();
        loadNonReactiveValues(); // ComboBox, аватар (требуют ручной инициализации)
    }

    // === 1. CSS и обработчики кнопок (без изменений) ===
    private void setupSettingsUI() {
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
        signalingServerIPLabel.getStyleClass().add("text-label");

        microphoneComboBox.getStyleClass().add("settings-text-field");
        speakerComboBox.getStyleClass().add("settings-text-field");
        cameraComboBox.getStyleClass().add("settings-text-field");

        nicknameField.getStyleClass().add("settings-text-field");
        urlField.getStyleClass().add("settings-text-field");
        keyField.getStyleClass().add("settings-text-field");
        audioBitrateField.getStyleClass().add("settings-text-field");
        videoBitrateField.getStyleClass().add("settings-text-field");
        signalingServerIPField.getStyleClass().add("settings-text-field");

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

    private void saveSettings() {
        SettingsManager.getInstance().save();

        closeWindow();
    }

    public void initializeResponsiveLayout(Stage stage) {
        setupCloseInterceptor(stage);
    }

    private void setupCloseInterceptor(Stage stage) {
        stage.setOnCloseRequest(event -> {
                        onDiscardButtonClicked();
                        event.consume();
                    });
    }

    // === 2. 🔥 РЕАКТИВНАЯ ПРИВЯЗКА (заменяет setSettingsValues) ===
    private void bindReactiveFields() {
        // TextField ↔ StringProperty
        nicknameField.textProperty().bindBidirectional(settings.nicknameProperty());

        keyField.textProperty().bindBidirectional(settings.userKeyProperty());
//        audioBitrateField.textProperty().bindBidirectional((Property<String>) settings.audioBitrateProperty().asString());
        bindIntegerField(audioBitrateField, settings.audioBitrateProperty(), 32, 320, "audioBitrate");
        bindIntegerField(videoBitrateField, settings.cameraBitrateProperty(), 100, 10000, "cameraBitrate");

//        videoBitrateField.textProperty().bindBidirectional((Property<String>) settings.cameraBitrateProperty().asString());
        signalingServerIPField.textProperty().bindBidirectional(settings.signalingServerIpProperty());

        // Slider ↔ IntegerProperty
        microphoneVolumeSlider.valueProperty().bindBidirectional(settings.microphoneVolumeProperty());
        speakerVolumeSlider.valueProperty().bindBidirectional(settings.speakerVolumeProperty());

        // ToggleButton ↔ BooleanProperty
        notificationsToggleButtonSelector.selectedProperty().bindBidirectional(settings.notificationsEnabledProperty());

        bindThemeToggle(themeToggleButtonSelector, settings.themeProperty());
//        themeToggleButtonSelector.selectedProperty().bindBidirectional(settings.themeProperty().isEqualTo("light"));

        // URL field: объединяем IP:Port (двусторонняя привязка сложнее, поэтому делаем одностороннюю + обработчик)
        updateUrlField(); // Инициализация
        urlField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) { // При потере фокуса парсим и сохраняем
                parseAndSaveUrl();
            }
        });

        // Слушатель на изменения IP/Port в модели → обновление поля
        settings.userIpProperty().addListener((obs, old, newVal) -> updateUrlField());
        settings.userPortProperty().addListener((obs, old, newVal) -> updateUrlField());
    }

    /**
     * Ручная двусторонняя привязка ToggleButton к строковому свойству темы.
     * selected = true -> "light" selected = false -> "dark"
     */
    private void bindThemeToggle(ToggleButton toggle, StringProperty themeProperty) {
        // 1. Инициализация состояния
        toggle.setSelected("light".equals(themeProperty.get()));

        // 2. Модель → UI (изменение темы в файле/коде обновляет кнопку)
        themeProperty.addListener((obs, oldVal, newVal) -> {
            boolean shouldBeSelected = "light".equals(newVal);
            if (toggle.isSelected() != shouldBeSelected) {
                toggle.setSelected(shouldBeSelected);
            }
        });

        // 3. UI → Модель (клик по кнопке обновляет тему и сохраняет в JSON)
        toggle.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            String newTheme = isNowSelected ? "light" : "dark";
            if (!newTheme.equals(themeProperty.get())) {
                themeProperty.set(newTheme);
            }
        });
    }

    /**
     * Создаёт двустороннюю привязку между TextField и IntegerProperty. Включает
     * базовую валидацию целых чисел.
     *
     * @param textField поле ввода
     * @param intProperty свойство модели
     * @param min минимальное допустимое значение
     * @param max максимальное допустимое значение
     */
    /**
     * Создаёт двустороннюю привязку между TextField и IntegerProperty с
     * валидацией диапазона и визуальной обратной связью
     */
    private void bindIntegerField(TextField textField, IntegerProperty intProperty, int min, int max, String fieldName) {
        // 1. Инициализация
        textField.setText(String.valueOf(intProperty.get()));

        // 2. TextFormatter: только цифры
        textField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("\\d+")) {
                return change;
            }
            return null;
        }));

        // 3. Модель → UI (если поле не в фокусе)
        intProperty.addListener((obs, oldVal, newVal) -> {
            if (!textField.isFocused()) {
                textField.setText(String.valueOf(newVal));
            }
        });

        // 4. UI → Модель (при потере фокуса + валидация)
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try {
                    String text = textField.getText().trim();
                    if (text.isEmpty()) {
                        textField.setText(String.valueOf(intProperty.get()));
                        return;
                    }
                    int value = Integer.parseInt(text);

                    // Валидация диапазона
                    if (value < min || value > max) {
                        showFieldError(textField, "Допустимый диапазон: " + min + "–" + max);
                        textField.setText(String.valueOf(intProperty.get()));
                        return;
                    }

                    // Дополнительно: бизнес-валидация из SettingsManager
                    Optional<String> error = settings.getValidationError(fieldName, value);
                    if (error.isPresent()) {
                        showFieldError(textField, error.get());
                        textField.setText(String.valueOf(intProperty.get()));
                        return;
                    }

                    // Всё ок → сохраняем в модель
                    intProperty.set(value);

                } catch (NumberFormatException e) {
                    showFieldError(textField, "Введите целое число");
                    textField.setText(String.valueOf(intProperty.get()));
                }
            }
        });
    }

    /**
     * Настраивает базовую валидацию для текстовых полей
     */
    private void setupFieldValidation() {
        // Никнейм: фильтр ввода + валидация при потере фокуса
        nicknameField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            // Разрешаем пустое, буквы (лат/кир), цифры, пробелы, подчёркивания
            if (newText.isEmpty() || newText.matches("^[a-zA-Z0-9_\\s\\u0400-\\u04FF]{0,20}$")) {
                return change;
            }
            return null; // Отклоняем ввод
        }));

        nicknameField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String value = nicknameField.getText();
                Optional<String> error = settings.validateNickname(value);
                if (error.isPresent()) {
                    showFieldError(nicknameField, error.get());
                    // Откат к последнему валидному значению из модели
                    nicknameField.setText(settings.getNickname());
                }
            }
        });

        // IP-адрес: только цифры и точки
        signalingServerIPField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("^[0-9.]{0,15}$")) {
                return change;
            }
            return null;
        }));

        signalingServerIPField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                Optional<String> error = settings.validateIpAddress(signalingServerIPField.getText());
                if (error.isPresent()) {
                    showFieldError(signalingServerIPField, error.get());
                    signalingServerIPField.setText(settings.getSignalingServerIp());
                }
            }
        });

        // Порт: только цифры
        urlField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            // Разрешаем формат "123.456.789.012:443"
            if (newText.isEmpty() || newText.matches("^[0-9.:]{0,21}$")) {
                return change;
            }
            return null;
        }));

        urlField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String[] parts = urlField.getText().split(":");
                if (parts.length >= 2) {
                    Optional<String> ipError = settings.validateIpAddress(parts[0].trim());
                    Optional<String> portError = settings.validatePort(parts[1].trim());
                    if (ipError.isPresent()) {
                        showFieldError(urlField, "IP: " + ipError.get());
                        updateUrlField(); // откат
                    } else if (portError.isPresent()) {
                        showFieldError(urlField, "Порт: " + portError.get());
                        updateUrlField();
                    }
                }
            }
        });

        // Ключ пользователя: макс. 128 символов
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

    /**
     * Показывает ошибку валидации: красная рамка + tooltip
     */
    private void showFieldError(TextField field, String message) {
        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 12px;");
        Tooltip.install(field, tooltip);
        field.setStyle("-fx-border-color: #ff6b6b; -fx-border-width: 2px; -fx-border-radius: 4px;");

        // Убираем подсветку, когда пользователь начинает исправлять
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                field.setStyle("");
                Tooltip.uninstall(field, tooltip);
            }
        });
    }

    private void updateUrlField() {
        String ip = settings.getUserIp();
        String port = settings.getUserPort();
        // Обновляем только если поле не в фокусе (чтобы не мешать пользователю вводить)
        if (!urlField.isFocused()) {
            urlField.setText(ip + ":" + port);
        }
    }

    private void parseAndSaveUrl() {
        String[] parts = urlField.getText().split(":");
        if (parts.length >= 2) {
            settings.setUserIp(parts[0].trim());
            settings.setUserPort(parts[1].trim());
        }
    }

    // === 3. НЕРЕАКТИВНЫЕ ЗНАЧЕНИЯ (ComboBox, аватар) ===
    private void loadNonReactiveValues() {
        showAvatar();
        setMicrophoneComboBox();
        setSpeakerComboBox();
        setCameraComboBox();

        // Синхронизация выбора устройства: UI → модель (односторонняя)
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

    private void setMicrophoneComboBox() {
        List<String> devices = settings.getAvailableMicrophones();
        microphoneComboBox.getItems().addAll(devices);

        // 2. Восстанавливаем сохраненное значение
        String saved = settings.getMicrophone();
        if (saved != null && devices.contains(saved)) {
            microphoneComboBox.setValue(saved);
        } else {
            microphoneComboBox.setValue("Default");
        }

        // 3. 🔥 РЕАКТИВНАЯ ПРИВЯЗКА (Только в Контроллере!)
        // Изменение в UI -> Обновление Модели (и автосохранение внутри модели)
        microphoneComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                settings.setMicrophone(newVal);
            }
        });
    }

    private void setSpeakerComboBox() {
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

    private void setCameraComboBox() {

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

    private void showAvatar() {
        if (settings.isRegistered()) {
            String path = settings.getPathToAvatar();
            if (path != null && !path.isEmpty()) {
                Path p = Path.of(path);
                if (p.toFile().exists()) {
                    Image avatarImage = new Image(p.toUri().toString(), 162, 162, true, true);
                    if (!avatarImage.isError()) {
                        avatarView.setImage(avatarImage);
                        uploadedLabel.setText("Выбранный файл: " + p.getFileName());
                        return;
                    }
                }
            }
        }
        uploadedLabel.setText("Аватар не установлен");
    }

    // === Upload Avatar (минимальные правки) ===
    private void uploadAvatar() {
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
            // 🔥 Аватар уже сохранён в модели, просто обновляем UI
            Platform.runLater(this::showAvatar);
        } else if (selectedFile != null) {
            uploadedLabel.setText("❌ Ошибка загрузки");
        }
    }

    private void onDiscardButtonClicked() {
        initialSnapshot.revert(settings);
        closeWindow();
    }

    private void closeWindow() {
        Platform.runLater(() -> {
//            try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
            Stage stage = (Stage) discardButton.getScene().getWindow();
            stage.close();
        });
    }

    /**
     * Неизменяемый снимок настроек для реализации паттерна Rollback.
     * Используется при открытии окна настроек и при отмене изменений.
     */
    private static record SettingsSnapshot(
            String nickname, String userIp, String userPort, String userKey, String theme, String pathToAvatar,
            String microphone, String speaker, String camera, String signalingServerIp,
            boolean notificationsEnabled,
            int microphoneVolume, int speakerVolume, int audioBitrate, int cameraBitrate) {

        /**
         * Фабричный метод: создаёт снимок текущего состояния менеджера настроек
         */
        static SettingsSnapshot from(SettingsManager manager) {
            return new SettingsSnapshot(
                    manager.getNickname(),
                    manager.getUserIp(),
                    manager.getUserPort(),
                    manager.getUserKey(),
                    manager.getTheme(),
                    manager.getPathToAvatar(),
                    manager.getMicrophone(),
                    manager.getSpeaker(),
                    manager.getCamera(),
                    manager.getSignalingServerIp(),
                    manager.isNotificationsEnabled(),
                    manager.getMicrophoneVolume(),
                    manager.getSpeakerVolume(),
                    manager.getAudioBitrate(),
                    manager.getCameraBitrate()
            );
        }

        /**
         * Восстанавливает (откатывает) значения менеджера настроек до состояния
         * этого снимка
         */
        void revert(SettingsManager manager) {
            manager.setNickname(nickname);
            manager.setUserIp(userIp);
            manager.setUserPort(userPort);
            manager.setUserKey(userKey);
            manager.setTheme(theme);
            manager.setPathToAvatar(pathToAvatar);
            manager.setMicrophone(microphone);
            manager.setSpeaker(speaker);
            manager.setCamera(camera);
            manager.setSignalingServerIp(signalingServerIp);
            manager.setNotificationsEnabled(notificationsEnabled);
            manager.setMicrophoneVolume(microphoneVolume);
            manager.setSpeakerVolume(speakerVolume);
            manager.setAudioBitrate(audioBitrate);
            manager.setCameraBitrate(cameraBitrate);
        }
    }

}
