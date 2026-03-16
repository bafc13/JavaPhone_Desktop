package com.mycompany.javaphone_nir2;

import com.mycompany.javaphone_nir2.models.Contact;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Custom cell for ListView, which show contacts with avatar, name и status/last
 * message.
 *
 */
public class ContactListCell extends ListCell<Contact> {

    private HBox root;
    private StackPane avatarContainer;
    /**
     * first letter in name
     */
    private Label avatarLabel;
    private VBox textContainer;
    private Label nameLabel;
    private Label statusLabel;

    /**
     * avatar size in px
     */
    private static final double AVATAR_SIZE = 50;

    public ContactListCell() {
        initializeUI();
    }

    private void initializeUI() {
        createAvatar();

        createTextContainer();

        root = new HBox(15);
        root.setStyle("-fx-padding: 10; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
        root.setPrefHeight(70);
        root.setStyle(
                "-fx-padding: 10; "
                + "-fx-border-color: #ecf0f1; "
                + "-fx-border-width: 0 0 1 0; "
                + "-fx-background-color: #dadef7;"
        );

        root.getChildren().addAll(avatarContainer, textContainer);
        HBox.setHgrow(textContainer, Priority.ALWAYS);
    }

    private void createAvatar() {
        avatarLabel = new Label();
        avatarLabel.setFont(Font.font("System", 20));
        avatarLabel.setTextFill(Color.WHITE);
        avatarLabel.setStyle("-fx-alignment: CENTER;");

        avatarContainer = new StackPane(avatarLabel);
        avatarContainer.setPrefSize(AVATAR_SIZE, AVATAR_SIZE);
        avatarContainer.setStyle(
                "-fx-border-color: #2c3e50; "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 0;"
        );

        avatarContainer.setStyle(
                "-fx-background-radius: 0; "
                + "-fx-border-radius: 0; "
                + "-fx-border-color: #2c3e50; "
                + "-fx-border-width: 2;"
        );
    }

    private void createTextContainer() {
        nameLabel = new Label();
        nameLabel.setFont(Font.font("System", 14));
        nameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");

        statusLabel = new Label();
        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-wrap-text: true;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxHeight(Double.MAX_VALUE);

        textContainer = new VBox(5);
        textContainer.setPadding(new Insets(5, 0, 0, 0));
        textContainer.getChildren().addAll(nameLabel, statusLabel);

        VBox.setVgrow(statusLabel, Priority.ALWAYS);
    }

    /**
     * this method pulls every time, that cell is updating
     *
     * @param contact contact object
     * @param empty status of cell
     */
    @Override
    protected void updateItem(Contact contact, boolean empty) {
        super.updateItem(contact, empty);

        if (empty || contact == null) {
            setGraphic(null);
            return;
        }

        updateContactDisplay(contact);

        setGraphic(root);
    }

    /**
     * updating contact info
     *
     * @param contact contact to show
     */
    private void updateContactDisplay(Contact contact) {
        nameLabel.setText(contact.getName());

        statusLabel.setText(contact.getStatus());

        updateAvatar(contact);
    }

    /**
     * updating avatar
     *
     * @param contact contact with info for contact
     */
    private void updateAvatar(Contact contact) {
        String firstName = contact.getName().substring(0, 1).toUpperCase();
        avatarLabel.setText(firstName);

        String avatarColor = contact.getAvatarColor();
        avatarContainer.setStyle(
                "-fx-background-color: " + avatarColor + "; "
                + "-fx-background-radius: 0; "
                + "-fx-border-radius: 0; "
                + "-fx-border-color: #2c3e50; "
                + "-fx-border-width: 2;"
        );
    }
}
