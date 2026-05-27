package com.mycompany.javaphone_nir2;

import com.mycompany.javaphone_nir2.logging.SessionLogger;
import com.mycompany.javaphone_nir2.media.MediaUtils;
import com.mycompany.javaphone_nir2.models.Media;
import com.mycompany.javaphone_nir2.models.Message;
import java.awt.Desktop;
import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * A custom cell for displaying messages in the chat history. Implements a
 * modern design: a message "bubble" with a time stamp.
 */
public class ChatHistoryCell extends ListCell<Message> {

    private static final DateTimeFormatter TIME_FORMATTER
            = DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault());

    private final HBox root;
    private final VBox bubbleContainer;
    private final Label contentLabel = new Label();
    private final Label timeLabel = new Label();
    private final Label senderLabel = new Label();

    private final ImageView filePreview = new ImageView();
    private final VBox fileAttachmentBox = new VBox(4);
    private final Label fileNameLabel = new Label();
    private final Label fileSizeLabel = new Label();

    private Timeline appearAnimation;
    private boolean isAnimated = false;

    /**
     * Ctor that set components, styles
     */
    public ChatHistoryCell() {
        root = new HBox(8);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(1, 6, 1, 6));

        bubbleContainer = new VBox(4);
        bubbleContainer.getStyleClass().add("message-bubble");
        bubbleContainer.setPadding(new Insets(8, 12, 8, 12));

        senderLabel.getStyleClass().add("message-sender");
        senderLabel.setVisible(false);

        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("message-content");
        contentLabel.setMaxWidth(Region.USE_COMPUTED_SIZE);

        timeLabel.getStyleClass().add("message-time");
        timeLabel.setAlignment(Pos.CENTER_RIGHT);

        filePreview.setFitWidth(380);
        filePreview.setFitHeight(380);
        filePreview.setPreserveRatio(true);
        filePreview.getStyleClass().add("file-preview");
        filePreview.setVisible(false);
        filePreview.setManaged(false);
        filePreview.setCursor(Cursor.HAND);
        filePreview.setOnMouseClicked(e -> openAttachment());

        fileNameLabel.getStyleClass().add("file-name-link");
        fileNameLabel.setCursor(Cursor.HAND);
        fileNameLabel.setOnMouseClicked(e -> openAttachment());

        fileSizeLabel.getStyleClass().add("file-size");
        fileAttachmentBox.getChildren().addAll(fileNameLabel, fileSizeLabel);
        fileAttachmentBox.setVisible(false);
        fileAttachmentBox.setManaged(false);

        bubbleContainer.getChildren().addAll(senderLabel, contentLabel, filePreview, fileAttachmentBox, timeLabel);

        HBox.setHgrow(bubbleContainer, Priority.NEVER);
        root.getChildren().add(bubbleContainer);

        root.setOpacity(0);
        root.setTranslateY(15);

        appearAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(root.opacityProperty(), 0),
                        new KeyValue(root.translateYProperty(), 15, Interpolator.EASE_OUT)
                ),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(root.opacityProperty(), 1),
                        new KeyValue(root.translateYProperty(), 0)
                )
        );

        setGraphic(root);
    }

    /**
     * This method responsible for updating item
     */
    @Override
    protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            setGraphic(null);
            appearAnimation.stop();
            isAnimated = false;
            resetCellState();
            return;
        }

        contentLabel.setText(message.getContent());
        timeLabel.setText(formatTime(message.getTime()));

        String text = message.getContent();
        contentLabel.setText(text);

        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            contentLabel.setManaged(false);
            renderAttachments(message.getAttachments());
        }

        if (text.matches(".*[A-ZА-Я].*") && text.equals(text.toUpperCase())) {
            contentLabel.getStyleClass().add("uppercase-message");
        } else {
            contentLabel.getStyleClass().remove("uppercase-message");
        }

        // local - right align, remote - left align
        if (isOwnMessage(message)) {
            root.setAlignment(Pos.CENTER_RIGHT);
            bubbleContainer.getStyleClass().add("message-bubble-own");
            bubbleContainer.getStyleClass().remove("message-bubble-other");
        } else {
            root.setAlignment(Pos.CENTER_LEFT);
            bubbleContainer.getStyleClass().add("message-bubble-other");
            bubbleContainer.getStyleClass().remove("message-bubble-own");
        }
        setGraphic(root);

        int lastIndex = getListView().getItems().size() - 1;
        if (getIndex() == lastIndex && !empty) {
            root.setOpacity(0);
            root.setTranslateY(15);
            appearAnimation.playFromStart();
        } else {
            root.setOpacity(1);
            root.setTranslateY(0);
        }
    }

    private void resetCellState() {
        contentLabel.setText(null);
        timeLabel.setText(null);
        senderLabel.setText(null);

        filePreview.setVisible(false);
        filePreview.setImage(null);
        fileAttachmentBox.setVisible(false);
        fileNameLabel.setText(null);
        fileSizeLabel.setText(null);
    }

    private void renderAttachments(List<Media> attachments) {
        if (attachments.isEmpty()) {
            return;
        }

        Media att = attachments.get(0);
        String fileName = MediaUtils.extractFileName(att.getPath());
        long fileSize = MediaUtils.extractFileSize(att.getPath());
        boolean isImage = MediaUtils.isImageFile(att.getPath());

        senderLabel.setManaged(false);
        fileAttachmentBox.setManaged(true);

        if (isImage) {
            filePreview.setManaged(true);
            fileAttachmentBox.setManaged(false);
            Image img = new Image(new File(att.getPath()).toURI().toString(),
                    380, 380, true, true);
            filePreview.setImage(img);
            filePreview.setVisible(true);
        } else {
            filePreview.setManaged(false);
            fileNameLabel.setText(fileName);
            fileSizeLabel.setText(MediaUtils.formatSize(fileSize));
            fileAttachmentBox.setVisible(true);
        }
    }

    private void openAttachment() {
        Message msg = getItem();
        if (msg != null && msg.getAttachments() != null && !msg.getAttachments().isEmpty()) {
            try {
                File file = new File(msg.getAttachments().get(0).getPath());
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                }
            } catch (Exception e) {
                SessionLogger.getInstance().log("Failed to open attachment: " + e.getMessage());
            }
        }
    }

    /**
     * This method formats Unix-timestamp
     */
    private String formatTime(long timestamp) {
        return TIME_FORMATTER.format(Instant.ofEpochSecond(timestamp));
    }

    /**
     * Determines whether the message is "one's own." Can be customized: compare
     * with the current user's public key.
     */
    private boolean isOwnMessage(Message message) {
        return "Вы".equals(message.getSenderPublicKey());
    }
}
