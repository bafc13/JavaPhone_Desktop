package com.mycompany.javaphone_nir2.models;

/**
 * data model for contact have all info to show contact in ListView.
 */
public class Contact {

    private String name;
    private String status;
    private String key;

    /**
     * HEX
     */
    private String avatarColor;

    public Contact(String name, String status, String avatarColor, String key) {
        this.name = name;
        this.status = status;
        this.avatarColor = avatarColor;
        this.key = key;
    }

    public Contact(String name, String status, String key) {
        this.name = name;
        this.status = status;
        this.key = key;
        this.avatarColor = "#dadef7";
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getAvatarColor() {
        return avatarColor;
    }

    public String getKey() {
        return key;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return name;
    }
}
