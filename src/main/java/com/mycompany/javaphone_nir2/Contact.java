package com.mycompany.javaphone_nir2;

/**
 * Модель данных для контакта.
 * Содержит всю информацию о контакте для отображения в ListView.
 */
public class Contact {

    // ==================== СВОЙСТВА ====================

    /** Имя контакта */
    private String name;

    /** Статус или последнее сообщение */
    private String status;

    /** Цвет аватара (HEX формат) */
    private String avatarColor;

    // ==================== КОНСТРУКТОР ====================

    /**
     * Конструктор для создания контакта.
     *
     * @param name имя контакта (например, "AVKuzma")
     * @param status последнее сообщение или статус
     * @param avatarColor цвет аватара в HEX формате (например, "#3498db")
     */
    public Contact(String name, String status, String avatarColor) {
        this.name = name;
        this.status = status;
        this.avatarColor = avatarColor;
    }

    // ==================== GETTER'S ====================

    /**
     * @return имя контакта
     */
    public String getName() {
        return name;
    }

    /**
     * @return статус или последнее сообщение
     */
    public String getStatus() {
        return status;
    }

    /**
     * @return цвет аватара
     */
    public String getAvatarColor() {
        return avatarColor;
    }

    // ==================== SETTER'S ====================

    /**
     * Устанавливает имя контакта.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Устанавливает статус или последнее сообщение.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Устанавливает цвет аватара.
     */
    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor;
    }

    // ==================== TO STRING ====================

    @Override
    public String toString() {
        return name;
    }
}
