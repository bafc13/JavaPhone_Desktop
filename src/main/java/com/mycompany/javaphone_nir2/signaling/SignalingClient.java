package com.mycompany.javaphone_nir2.signaling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;

@ClientEndpoint
public class SignalingClient {
    private Session session;
    private final String serverUrl;
    private String clientId;
    private final ObjectMapper mapper = new ObjectMapper();
    
    public SignalingClient(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    public void connect() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxSessionIdleTimeout(2000000);
        container.connectToServer(this, new URI(serverUrl));
    }
    
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
    }
    
    @OnMessage
    public void onMessage(String message) throws IOException{
        JsonNode json = mapper.readTree(message);
        String type = json.get("type").asText();

        switch (type) {
            case "welcome":
                handleWelcome(json);
                break;
            case "offer":
                handleOffer(json);
                break;
            case "answer":
                handleAnswer(json);
                break;
            case "candidate":
                handleCandidate(json);
                break;
            case "clientDisconnected":
                handleClientDisconnected(json);
                break;
            case "error":
                handleError(json);
                break;
        }
    }
    
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        // logic on call end
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        // logic on error
    }
    
    private void handleWelcome(JsonNode json) {
        // logic on connected
    }
    
    private void handleOffer(JsonNode json) {
        String sdp = json.get("sdp").asText();
        String sender = json.get("sender").asText();
//        tell webrtc manager to handle offer
        
    }
    
    private void handleAnswer(JsonNode json) {
        String sdp = json.get("sdp").asText();
//        tell webrtc manager to handle answer
    }
    
    private void handleCandidate(JsonNode json) {
        String candidate = json.get("candidate").asText();
        String sdpMid = json.get("sdpMid").asText();
        int sdpMLineIndex = json.get("sdpMLineIndex").asInt();
//        tell webrtc manager to handle candidate
    }
    
    private void handleClientDisconnected(JsonNode json) {
        String disconnectedClientId = json.get("clientId").asText();
//        tell webrtc manager to handle disconnect
    }
    
    private void handleError(JsonNode json) {
        String error = json.get("message").asText();
    }
    
    public void sendOffer(String sdp, String targetClientId) throws IOException {
        sendMessage("offer", sdp, targetClientId);
    }
    
    public void sendAnswer(String sdp, String targetClientId) throws IOException {
        sendMessage("answer", sdp, targetClientId);
    }
    
    public void sendIceCandidate(String candidate, String sdpMid, int sdpMLineIndex, String targetClientId) throws IOException {
        ObjectNode message = mapper.createObjectNode();
        message.put("type", "candidate");
        message.put("candidate", candidate);
        message.put("sdpMid", sdpMid);
        message.put("sdpMLineIndex", sdpMLineIndex);
        message.put("target", targetClientId);
        message.put("sender", clientId);
        
        sendJson(message);
    }
    
    public void sendReady(String targetClientId) throws IOException {
        sendMessage("ready", "", targetClientId);
    }
    
    public void sendBye(String targetClientId) throws IOException {
        sendMessage("bye", "", targetClientId);
//      tell webrtc manager to handle call end
    }
    
    private void sendMessage(String type, String sdp, String targetClientId) throws IOException {
        ObjectNode message = mapper.createObjectNode();
        message.put("type", type);
        if (!sdp.isEmpty()) {
            message.put("sdp", sdp);
        }
        message.put("target", targetClientId);
        message.put("sender", clientId);
        
        sendJson(message);
    }
    
    private void sendJson(ObjectNode message) throws IOException {
        if (session != null && session.isOpen()) {
            String json = mapper.writeValueAsString(message);
            session.getBasicRemote().sendText(json);
        }
    }
    
    public void disconnect() throws IOException {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
    
    public String getClientId() {
        return clientId;
    }
}
