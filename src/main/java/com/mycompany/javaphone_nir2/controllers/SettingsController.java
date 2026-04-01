package com.mycompany.javaphone_nir2.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 * Controller for settings window
 *
 *
 */
public class SettingsController {
    @FXML
    private TextField audioBitrateField;

    @FXML
    private ImageView avatarView;

    @FXML
    private ComboBox<?> cameraComboBox;

    @FXML
    private Button discardButton;

    @FXML
    private TextField keyField;

    @FXML
    private ScrollPane mainContainer;

    @FXML
    private ComboBox<?> microphoneComboBox;

    @FXML
    private Slider microphoneVolumeSlider;

    @FXML
    private TextField nicknameField;

    @FXML
    private ToggleButton notificationsToggleButtonSelector;

    @FXML
    private Button regenerateKeyButton;

    @FXML
    private Button saveButton;

    @FXML
    private TextField signalingServerIPField;

    @FXML
    private ComboBox<?> speakerComboBox;

    @FXML
    private Slider speakerVolumeSlider;

    @FXML
    private ToggleButton themeToggleButtonSelector;

    @FXML
    private Button uploadAvatarButton;

    @FXML
    private Label uploadedLabel;

    @FXML
    private TextField urlField;

    @FXML
    private TextField videoBitrateField;

    /**
     * Initializes the controller class.
     */
    @FXML
    public void initialize() {
        discardButton.setOnAction(e -> closeWindow());
    }

    private void closeWindow() {
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
                //do close call waiter and chat threads
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Stage stage = (Stage) discardButton.getScene().getWindow();
            stage.close();
        });
    }
}
