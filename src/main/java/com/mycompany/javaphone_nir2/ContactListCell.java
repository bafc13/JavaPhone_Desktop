package com.mycompany.javaphone_nir2;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Кастомная ячейка для ListView, которая отображает контакты с аватаром,
 * именем и статусом/последним сообщением.
 *
 * Макет:
 * ┌──────────────────────────┐
 * │ [Avatar] Имя             │
 * │          Последнее сообщ. │
 * └──────────────────────────┘
 */
public class ContactListCell extends ListCell<Contact> {

    // ==================== UI КОМПОНЕНТЫ ====================

    /** Главный контейнер ячейки */
    private HBox root;

    /** Контейнер для аватара */
    private StackPane avatarContainer;

    /** Название в аватаре (первая буква имени) */
    private Label avatarLabel;

    /** Контейнер для текста (имя и статус) */
    private VBox textContainer;

    /** Текст с именем контакта */
    private Label nameLabel;

    /** Текст с последним сообщением/статусом */
    private Label statusLabel;

    // ==================== КОНСТАНТЫ ====================

    /** Размер аватара в пикселях */
    private static final double AVATAR_SIZE = 50;

    // ==================== КОНСТРУКТОР ====================

    /**
     * Конструктор - инициализирует все UI элементы.
     */
    public ContactListCell() {
        initializeUI();
    }

    // ==================== ИНИЦИАЛИЗАЦИЯ UI ====================

    /**
     * Инициализирует все визуальные элементы ячейки.
     */
    private void initializeUI() {
        // Создаём аватар (кружок с буквой)
        createAvatar();

        // Создаём текстовую информацию (имя и статус)
        createTextContainer();

        // Создаём главный контейнер HBox
        root = new HBox(15);
        root.setStyle("-fx-padding: 10; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
        root.setPrefHeight(70);
        root.setStyle(
            "-fx-padding: 10; " +
            "-fx-border-color: #ecf0f1; " +
            "-fx-border-width: 0 0 1 0; " +
            "-fx-background-color: #dadef7;"
        );

        // Добавляем аватар и текстовый контейнер в HBox
        root.getChildren().addAll(avatarContainer, textContainer);

        // Настраиваем, чтобы текстовый контейнер растягивался
        HBox.setHgrow(textContainer, Priority.ALWAYS);
    }

    /**
     * Создаёт компонент аватара (цветной кружок с первой буквой имени).
     */
    private void createAvatar() {
        // Создаём label для отображения первой буквы имени
        avatarLabel = new Label();
        avatarLabel.setFont(Font.font("System", 20));
        avatarLabel.setTextFill(Color.WHITE);
        avatarLabel.setStyle("-fx-alignment: CENTER;");

        // Создаём контейнер (круг) для аватара
        avatarContainer = new StackPane(avatarLabel);
        avatarContainer.setPrefSize(AVATAR_SIZE, AVATAR_SIZE);
        avatarContainer.setStyle(
            "-fx-border-color: #2c3e50; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 0;"
        );

        // Устанавливаем круглую форму (скругленные углы)
        avatarContainer.setStyle(
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0; " +
            "-fx-border-color: #2c3e50; " +
            "-fx-border-width: 2;"
        );
    }

    /**
     * Создаёт контейнер с информацией о контакте (имя и статус).
     */
    private void createTextContainer() {
        // Создаём Label для имени
        nameLabel = new Label();
        nameLabel.setFont(Font.font("System", 14));
        nameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");

        // Создаём Label для статуса/последнего сообщения
        statusLabel = new Label();
        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-wrap-text: true;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxHeight(Double.MAX_VALUE);

        // Создаём контейнер для объединения имени и статуса
        textContainer = new VBox(5);
        textContainer.setPadding(new Insets(5, 0, 0, 0));
        textContainer.getChildren().addAll(nameLabel, statusLabel);

        // Настраиваем растягивание
        VBox.setVgrow(statusLabel, Priority.ALWAYS);
    }

    // ==================== UPDATE ITEM ====================

    /**
     * Этот метод вызывается каждый раз, когда нужно обновить отображение ячейки.
     *
     * @param contact объект контакта
     * @param empty пустая ли ячейка
     */
    @Override
    protected void updateItem(Contact contact, boolean empty) {
        super.updateItem(contact, empty);

        // Если ячейка пустая, очищаем содержимое
        if (empty || contact == null) {
            setGraphic(null);
            return;
        }

        // Обновляем данные в компонентах
        updateContactDisplay(contact);

        // Устанавливаем корневой контейнер как содержимое ячейки
        setGraphic(root);
    }

    /**
     * Обновляет отображение информации контакта.
     *
     * @param contact контакт для отображения
     */
    private void updateContactDisplay(Contact contact) {
        // Устанавливаем имя контакта
        nameLabel.setText(contact.getName());

        // Устанавливаем статус/последнее сообщение
        statusLabel.setText(contact.getStatus());

        // Обновляем аватар
        updateAvatar(contact);
    }

    /**
     * Обновляет аватар контакта (цвет и первая буква имени).
     *
     * @param contact контакт с информацией для аватара
     */
    private void updateAvatar(Contact contact) {
        // Получаем первую букву имени контакта
        String firstName = contact.getName().substring(0, 1).toUpperCase();
        avatarLabel.setText(firstName);

        // Получаем цвет аватара и устанавливаем его
        String avatarColor = contact.getAvatarColor();
        avatarContainer.setStyle(
            "-fx-background-color: " + avatarColor + "; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0; " +
            "-fx-border-color: #2c3e50; " +
            "-fx-border-width: 2;"
        );
    }
}
