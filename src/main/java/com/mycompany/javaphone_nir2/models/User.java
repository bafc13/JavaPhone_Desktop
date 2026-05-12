package com.mycompany.javaphone_nir2.models;

public class User {

    private String publicKey;
    private String name;
    private String email;
    private String ip;
    private Integer avatarId;

    // Constructors
    public User() {
    }

    public User(String publicKey, String name, String email, String ip, Integer avatarId) {
        this.publicKey = publicKey;
        this.name = name;
        this.email = email;
        this.ip = ip;
        this.avatarId = avatarId;
    }

    // Getters and setters
    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(Integer avatarId) {
        this.avatarId = avatarId;
    }
}
