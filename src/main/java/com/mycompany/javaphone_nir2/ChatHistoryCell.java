package com.mycompany.javaphone_nir2;

import com.mycompany.javaphone_nir2.models.Message;
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
import javafx.scene.layout.Region;
/**
 * Кастомная ячейка для отображения сообщения в истории чата.
 * Реализует современный дизайн: "пузырь" сообщения с временем.
 */
public class ChatHistoryCell extends ListCell<Message> {

    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    // Корневой контейнер ячейки
    private final HBox root;

    // Контейнер для "пузыря" сообщения
    private final VBox bubbleContainer;

    // Элементы контента
    private final Label contentLabel = new Label();
    private final Label timeLabel = new Label();
    private final Label senderLabel = new Label();

    public ChatHistoryCell() {
        // === Настройка корневой компоновки ===
        root = new HBox(8);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(1, 6, 1, 6));

        // === Настройка "пузыря" сообщения ===
        bubbleContainer = new VBox(4);
        bubbleContainer.getStyleClass().add("message-bubble");
        bubbleContainer.setPadding(new Insets(8, 12, 8, 12));
//        bubbleContainer.setMaxWidth(Region.USE_PREF_SIZE); // Будет задано через CSS

        // === Сборка контента ===
        senderLabel.getStyleClass().add("message-sender");
        senderLabel.setVisible(false); // Скрыт по умолчанию, показывается при необходимости
//        senderLabel.setMaxWidth(Region.USE_COMPUTED_SIZE);

        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("message-content");
//        contentLabel.setPrefWidth(Region.USE_PREF_SIZE);
        contentLabel.setMaxWidth(Region.USE_COMPUTED_SIZE);

        timeLabel.getStyleClass().add("message-time");
        timeLabel.setAlignment(Pos.CENTER_RIGHT);

        bubbleContainer.getChildren().addAll(senderLabel, contentLabel, timeLabel);

        // === Добавление "пузыря" в корень ===
        // HBox.setHgrow позволяет пузырю сжиматься/растягиваться
        HBox.setHgrow(bubbleContainer, Priority.NEVER);
        root.getChildren().add(bubbleContainer);

        // === Привязка контейнера к ячейке ===
        setGraphic(root);
    }

    @Override
    protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            setGraphic(null);
            return;
        }

        // Заполнение контента
        contentLabel.setText(message.getContent());
        timeLabel.setText(formatTime(message.getTime()));

        // Показывать отправителя только если это не своё сообщение (опционально)
        // if (!isOwnMessage(message)) { ... }
        senderLabel.setVisible(false);

        // === Позиционирование: свои сообщения — справа, чужие — слева ===
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
    }

    /**
     * Форматирует Unix-timestamp в читаемое время
     */
    private String formatTime(long timestamp) {
        return TIME_FORMATTER.format(Instant.ofEpochSecond(timestamp));
    }

    /**
     * Определяет, является ли сообщение "своим".
     * Можно кастомизировать: сравнивать с публичным ключом текущего пользователя.
     */
    private boolean isOwnMessage(Message message) {
        if(message.getSenderPublicKey() == "Вы") {
            return true;
        }
        // TODO: заменить на реальную проверку
        // return message.getSenderPublicKey().equals(SettingsManager.getInstance().getUserKey());
        return false; // Пока все сообщения считаем "чужими" для демонстрации
    }
}
