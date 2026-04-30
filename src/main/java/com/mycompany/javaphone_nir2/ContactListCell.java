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
    /** first letter in name */
    private Label avatarLabel;
    private VBox textContainer;
    private Label nameLabel;
    private Label statusLabel;

    /** avatar size in px */
    private static final double AVATAR_SIZE = 36;

    public ContactListCell() {
        initializeUI();
    }

    /** This method responsible for create ui elements, avatar and set styles */
    private void initializeUI() {
        createAvatar();

        createTextContainer();

        root = new HBox(15);
        root.getStyleClass().add("contact-list-cell-item");

        root.getChildren().addAll(avatarContainer, textContainer);
        HBox.setHgrow(textContainer, Priority.ALWAYS);
    }

    /** This method responsible for generating avatar */
    private void createAvatar() {
        avatarLabel = new Label();
        avatarLabel.setFont(Font.font("System", 20));
        avatarLabel.setTextFill(Color.BLACK);
        avatarLabel.setStyle("-fx-alignment: CENTER;");

        avatarContainer = new StackPane(avatarLabel);
        avatarContainer.setPrefSize(AVATAR_SIZE, AVATAR_SIZE);
        avatarContainer.getStyleClass().add("contacts-list-avatar-container");
    }

    /** This method responsible for create text container elements, styles */
    private void createTextContainer() {
        nameLabel = new Label();
        nameLabel.getStyleClass().add("contacts-list-name-label");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("contacts-list-status-label");
        statusLabel.setMaxHeight(Double.MAX_VALUE);

        textContainer = new VBox(5);
        textContainer.setPadding(new Insets(5, 0, 0, 0));
        textContainer.getChildren().addAll(nameLabel, statusLabel);

        VBox.setVgrow(statusLabel, Priority.ALWAYS);
    }

    /**
     * This method pulls every time, that cell is updating
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
     * This method responsible for updating contact info
     *
     * @param contact contact to show
     */
    private void updateContactDisplay(Contact contact) {
        nameLabel.setText(contact.getName());

        statusLabel.setText(contact.getStatus());

        updateAvatar(contact);
    }

    /**
     * This method responsible for updating avatar
     *
     * @param contact contact with info for contact
     */
    private void updateAvatar(Contact contact) {
        String firstName = contact.getName().substring(0, 1).toUpperCase();
        avatarLabel.setText(firstName);
    }
}
