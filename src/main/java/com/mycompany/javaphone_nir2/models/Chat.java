package com.mycompany.javaphone_nir2.models;

public class Chat {
    private int id;
    private String type;        // "dm" or "server"
    private String hostPublicKey;

    public Chat() {}

    public Chat(int id, String type, String hostPublicKey) {
        this.id = id;
        this.type = type;
        this.hostPublicKey = hostPublicKey;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getHostPublicKey() { return hostPublicKey; }
    public void setHostPublicKey(String hostPublicKey) { this.hostPublicKey = hostPublicKey; }
}