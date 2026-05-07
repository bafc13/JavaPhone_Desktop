package com.mycompany.javaphone_nir2.models;

public class Media {

    private int id;
    private String path;
    private String checksum;

    public Media() {
    }

    public Media(int id, String path, String checksum) {
        this.id = id;
        this.path = path;
        this.checksum = checksum;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
