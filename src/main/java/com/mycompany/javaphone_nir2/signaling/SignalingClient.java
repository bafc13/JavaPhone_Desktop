package com.mycompany.javaphone_nir2.signaling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.javaphone_nir2.controllers.ChatController;
import com.mycompany.javaphone_nir2.cryptography.MessageCryptographer;
import com.mycompany.javaphone_nir2.models.Contact;
import com.mycompany.javaphone_nir2.models.Offer;
import com.mycompany.javaphone_nir2.models.SettingsManager;
import com.mycompany.javaphone_nir2.models.User;
import com.mycompany.javaphone_nir2.models.UserStatus;
import com.mycompany.javaphone_nir2.webrtc.JavaPhoneCallManager;
import com.mycompany.javaphone_nir2.webrtc.JavaPhoneChatHandler;
import com.mycompany.javaphone_nir2.webrtc.WebRTCManager;

import jakarta.websocket.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;


@ClientEndpoint
public class SignalingClient {
    private Session session;
    private final String serverUrl;

    private String clientId;


    private final ObjectMapper mapper = new ObjectMapper();

    private final ObjectProperty<Offer> offer = new SimpleObjectProperty<>();
    
    private final Map<String, Contact> contacts = new HashMap<>();
    
    private final Set<JavaPhoneChatHandler> chatHandlers = new HashSet<>();
    
    private final MessageCryptographer MC = MessageCryptographer.getInstance();
    private final SettingsManager settings = SettingsManager.getInstance();
    
    private JavaPhoneCallManager callManager = null;

    private PublicKey serverPublicKey;
    
    private static SignalingClient instance = null;

    public static void initialize(String serverUrl) {
        instance = new SignalingClient(serverUrl);
    }

    public static SignalingClient getInstance() {
        return instance;
    }

    public SignalingClient(String serverUrl) {
        this.serverUrl = serverUrl;
        clientId = MC.getPublicKeyString();
    }

    public final void connect() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxSessionIdleTimeout(2000000);
        container.connectToServer(this, new URI(serverUrl));
        System.out.println("connected!");
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("OPENED SESSION");
    }

    @OnMessage
    public void onMessage(String messageEncrypted) throws IOException{
        System.out.println(messageEncrypted);
        String message;
        if (serverPublicKey != null) {
            message = MC.decryptMessage(messageEncrypted);
        } else {
            message = messageEncrypted;
        }
        
        JsonNode json = mapper.readTree(message);
        String type = json.get("type").asText();

        switch (type) {
            case "welcome":
                handleWelcome(json);
                break;
            case "peer":
                handlePeer(json);
                break;
            case "offer":
                handleOffer(json);
                break;
            case "accept":
                handleAccept(json);
                break;
            case "reject":
                handleReject(json);
                break;
            case "candidate":
                handleCandidate(json);
                break;
            case "clientDisconnected":
                handleClientDisconnected(json);
                break;
            case "message":
                handleDM(json);
                break;
            case "error":
                handleError(json);
                break;
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("CLOSED CONNECTION TO SIGNALING SERVER");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("ERROR IN SIGNALING");
        System.out.println(throwable.toString());
        throwable.printStackTrace();
    }

    public void sendPeer(User user, UserStatus status) throws IOException {
        ObjectNode dataNode = mapper.valueToTree(user);
        ObjectNode userNode = mapper.createObjectNode();
        userNode.set("data", dataNode);
        userNode.put("status", status.toString());

        ObjectNode message = mapper.createObjectNode();
        message.put("type", "peer");
        message.set("user", userNode);

        message.put("target", "all");
        message.put("sender", MC.getPublicKeyString());

        sendJson(message);
        // send to all
    }

    private void handlePeer(JsonNode json) {
        try {
            JsonNode userNode = json.get("user");
            User user = mapper.treeToValue(userNode.get("data"), User.class);
            UserStatus status = mapper.treeToValue(userNode.get("status"), UserStatus.class);

            String toPrint = userNode.asText();

            Contact contact = new Contact(user.getName(), status.toString(), user.getPublicKey());
            contacts.put(contact.getKey(), contact);
            if (callManager != null) {
                callManager.handleContact(contact);
            }

            System.out.println("GOT CONTACT");
            System.out.println(toPrint);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(SignalingClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(SignalingClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleWelcome(JsonNode json) {
        System.out.println("GOT WELCOME!");
        
        String serverPublicKeyString = json.get("serverKey").asText();
        serverPublicKey = MessageCryptographer.stringToPublicKey(serverPublicKeyString);
        
        User me = new User();
        me.setName(settings.getNickname());
        me.setEmail(settings.getEmail());
        me.setIp("localhost");
        me.setPublicKey(clientId);
        me.setAvatarId(Integer.MAX_VALUE);

        try {
            sendPeer(me, UserStatus.ONLINE);
        } catch (IOException ex) {
            System.getLogger(SignalingClient.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        // logic on connected
    }

    private void handleOffer(JsonNode json) {
        String sdp = json.get("sdp").asText();
        String sender = json.get("sender").asText();

        if (callManager != null) {
            callManager.handleIncomingCall(new Offer(sdp, sender));
        }
    }

    private void handleAccept(JsonNode json) {
        String sdp = json.get("sdp").asText();
        WebRTCManager rtcm = WebRTCManager.getInstance();
        rtcm.handleAccept(sdp);
    }

    private void handleReject(JsonNode json) {
        String sdp = json.get("sdp").asText();
        // TODO: handle reject
    }

    private void handleCandidate(JsonNode json) {
        String candidate = json.get("candidate").asText();
        String sdpMid = json.get("sdpMid").asText();
        int sdpMLineIndex = json.get("sdpMLineIndex").asInt();

        WebRTCManager rtcm = WebRTCManager.getInstance();
        rtcm.addIceCandidate(candidate, sdpMid, sdpMLineIndex);
    }

    private void handleClientDisconnected(JsonNode json) {
        String disconnectedClientId = json.get("clientId").asText();
        // TODO: handle disconnect
    }

    private void handleDM(JsonNode json) {
        System.out.println("GOT MESSAGE");
        String sender = json.get("sender").asText();
        PublicKey senderPublicKey = MessageCryptographer.stringToPublicKey(sender);
        
        String signature = json.get("signature").asText();
        
        String contentEncrypted = json.get("content").asText();
        String content = MC.decryptMessage(contentEncrypted);
        System.out.println(content);
        boolean isVerified = MC.confirmSign(content, signature, senderPublicKey);
        if (!isVerified) {
            System.out.println("Signature is false, skipping");
            return;
        }
        
        
        for (JavaPhoneChatHandler chatHandler : chatHandlers) {
            if (chatHandler != null) {
                chatHandler.handleStringMessage(sender, content);
            }
        }
    }

    private void handleError(JsonNode json) {
        String error = json.get("message").asText();
    }

    public void sendOffer(String sdp, String targetClientId) throws IOException {
        sendMessage("offer", sdp, targetClientId);
    }

    public void sendAccept(String sdp, String targetClientId) throws IOException {
        sendMessage("accept", sdp, targetClientId);
    }

    public void sendReject(String sdp, String targetClientId) throws IOException {
        sendMessage("reject", sdp, targetClientId);
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
        // TODO: handle call end
    }

    public void sendDM(String targetClientId, String content) throws IOException {
        PublicKey targetKey = MessageCryptographer.stringToPublicKey(targetClientId);
        
        String contentEncrypted = MC.encryptMessage(content, targetKey);
        String signature = MC.signMessage(content);
        
        ObjectNode message = mapper.createObjectNode();
        message.put("type", "message");
        message.put("sender", clientId);
        message.put("target", targetClientId);
        message.put("content", contentEncrypted);
        message.put("signature", signature);

        sendJson(message);
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
            String encryptedJson = MC.encryptMessage(json, serverPublicKey);
            session.getBasicRemote().sendText(encryptedJson);
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
    
    public void setCallManager(JavaPhoneCallManager callManager) {
        this.callManager = callManager;
    }
    
    public boolean addChatHandler(JavaPhoneChatHandler chatHandler) {
        return this.chatHandlers.add(chatHandler);
    }
    
    public boolean removeChatHandler(JavaPhoneChatHandler chatHandler) {
        return this.chatHandlers.remove(chatHandler);
    }
    
    public Contact getContact(String key) {
        return contacts.get(key);
    }
}
