package com.mycompany.javaphone_nir2.models;

/**
 * data model for contact have all info to show contact in ListView.
 */
public class Contact {

    private String name;
    private String status;

    /**
     * HEX
     */

    private String avatarColor;

    public Contact(String name, String status, String avatarColor) {
        this.name = name;
        this.status = status;
        this.avatarColor = avatarColor;
    }

    public Contact(String name, String status) {
        this.name = name;
        this.status = status;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor;
    }

    @Override
    public String toString() {
        return name;
    }
}
