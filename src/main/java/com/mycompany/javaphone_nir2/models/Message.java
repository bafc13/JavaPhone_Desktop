package com.mycompany.javaphone_nir2.models;

import java.util.List;

public class Message {
    private int id;
    private int chatId;
    private String senderPublicKey;
    private String content;
    private long time;           // Unix timestamp
    private List<Media> attachments; // optional, for convenience

    public Message() {}

    public Message(int id, int chatId, String senderPublicKey, String content, long time) {
        this.id = id;
        this.chatId = chatId;
        this.senderPublicKey = senderPublicKey;
        this.content = content;
        this.time = time;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChatId() { return chatId; }
    public void setChatId(int chatId) { this.chatId = chatId; }

    public String getSenderPublicKey() { return senderPublicKey; }
    public void setSenderPublicKey(String senderPublicKey) { this.senderPublicKey = senderPublicKey; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }

    public List<Media> getAttachments() { return attachments; }
    public void setAttachments(List<Media> attachments) { this.attachments = attachments; }
}