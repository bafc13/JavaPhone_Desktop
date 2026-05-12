package com.mycompany.javaphone_nir2.webrtc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.javaphone_nir2.controllers.ChatController;
import com.mycompany.javaphone_nir2.controllers.VideoCallController;
import com.mycompany.javaphone_nir2.games.GameMenuApp;
import com.mycompany.javaphone_nir2.cryptography.MessageCryptographer;
import com.mycompany.javaphone_nir2.models.SettingsManager;
import com.mycompany.javaphone_nir2.signaling.SignalingClient;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.*;
import dev.onvoid.webrtc.media.video.*;
import dev.onvoid.webrtc.media.audio.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

public class WebRTCManager implements PeerConnectionObserver {
    private static MessageCryptographer MC = MessageCryptographer.getInstance();
    
    private PeerConnectionFactory factory;

    private RTCConfiguration config;

    private VideoDeviceSource videoSource;
    private AudioSource audioSource;

    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    private VideoTrack remoteVideoTrack;
    private AudioTrack remoteAudioTrack;

    private RTCPeerConnection peerConnection;

    private List<RTCIceCandidate> candidates;

    private RTCRtpSender videoSender;
    private RTCRtpSender audioSender;
    public RTCDataChannel chatDataChannel;

    public RTCDataChannel gameDataChannel;

    private List<VideoDevice> cameras;
    private List<VideoCaptureCapability> cameraCapabilities;

    private List<AudioDevice> microphones;
    private List<AudioDevice> speakers;
    private AudioDeviceModule audioModule;

    private boolean cameraEnabled = true;
    private boolean microphoneEnabled = true;
    private String remoteClientKey;

    private Integer cameraId = 0;
    private Integer microphoneId = 0;
    private Integer speakerId = 0;

    private Integer microphoneVolume = 0;
    private Integer speakerVolume = 0;
    
    //game channel check
    private boolean isGameChannelReady = false;
    private Runnable onGameChannelReadyCallback;

    private final ScheduledExecutorService statsExecutor = Executors.newSingleThreadScheduledExecutor();

    private final ObjectMapper mapper = new ObjectMapper();

    // STUN servers
    private static RTCIceServer ICE_SERVERS = null;

    private static WebRTCManager instance = null;
    
    private JavaPhoneCallManager callManager = null;
    
    private JavaPhoneVideoHandler videoHandler = null;
    private final Set<JavaPhoneChatHandler> chatHandlers = new HashSet<>();
    
    private PeerConnectionObserverImpl peerConnectionObserver = null;

    public static WebRTCManager getInstance() {
        if (instance == null) {
            instance = new WebRTCManager();
        }
        return instance;
    }

    public WebRTCManager() {
        initializeWebRTC();
        initializeDevices();
    }

    private void initializeWebRTC() {
        if (ICE_SERVERS == null) {
            ICE_SERVERS = new RTCIceServer();
            ICE_SERVERS.urls.add("stun:stun.l.google.com:19302");
//            ICE_SERVERS.urls.add("stun:stun1.l.google.com:19302");
        }

        config = new RTCConfiguration();
        config.iceServers.add(ICE_SERVERS);

        candidates = new ArrayList<>();
        // TODO: Update UI
    }

    public void setRemoteTrack(VideoTrack remoteVideoTrack) {
        this.remoteVideoTrack = remoteVideoTrack;
    }

    public VideoTrack getRemoteVideoTrack() {
        return this.remoteVideoTrack;
    }

    public VideoTrack getLocalVideoTrack() {
        return this.localVideoTrack;
    }

    public void initializeDevices() {
        Thread deviceThread = new Thread(() -> {
            try {
                audioModule = new AudioDeviceModule();
                factory = new PeerConnectionFactory(audioModule);

                cameras = MediaDevices.getVideoCaptureDevices();
                microphones = MediaDevices.getAudioCaptureDevices();
                speakers = MediaDevices.getAudioRenderDevices();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.out.print("Ошибка инициализации устройств: " + e.getMessage());
                });
            }
        });

        deviceThread.setName("webrtc-device-init");
        deviceThread.setDaemon(true);
        deviceThread.start();
    }

    public void initializeMedia() {
        SettingsManager settings = SettingsManager.getInstance();
        String cameraName = settings.getCamera();
        String microphoneName = settings.getMicrophone();
        String speakerName = settings.getSpeaker();

        VideoDevice camera = null;
        for (VideoDevice cam : cameras) {
            if (cam.getName().equals(cameraName)) {
                camera = cam;
                break;
            }
        }

        cameraCapabilities = MediaDevices.getVideoCaptureCapabilities(camera);
        VideoCaptureCapability capability = cameraCapabilities.get(0);

        videoSource = new VideoDeviceSource();
        videoSource.setVideoCaptureDevice(camera);
        videoSource.setVideoCaptureCapability(capability);

        AudioDevice microphone = null;
        for (AudioDevice mic : microphones) {
            if (mic.getName().equals(microphoneName)) {
                microphone = mic;
                break;
            }
        }
        AudioDevice speaker = null;
        for (AudioDevice spk : speakers) {
            if (spk.getName().equals(speakerName)) {
                speaker = spk;
                break;
            }
        }

        audioModule.setRecordingDevice(microphone);
        audioModule.setPlayoutDevice(speaker);

        microphoneVolume = settings.getMicrophoneVolume();
        speakerVolume = settings.getSpeakerVolume();

        audioModule.setMicrophoneVolume(microphoneVolume);
        audioModule.setSpeakerVolume(speakerVolume);

    }

    public void initializeCapture() {
        videoSource.start();
        localVideoTrack = factory.createVideoTrack("video0", videoSource);

        if (videoHandler != null) {
            videoHandler.addLocalTrack(localVideoTrack);
        }

        audioModule.initRecording();
        audioModule.initPlayout();
        AudioOptions ao = new AudioOptions();
        AudioTrackSource ats = factory.createAudioSource(ao);
        localAudioTrack = factory.createAudioTrack("audio0", ats);
    }

    public void startCall(String targetClientKey) {
        initializeMedia();  
        initializeCapture();

        remoteClientKey = targetClientKey;

        peerConnectionObserver = new PeerConnectionObserverImpl(this, remoteClientKey);
        if (videoHandler != null) {
            peerConnectionObserver.setVideoHandler(videoHandler);
        }
        
        peerConnection = factory.createPeerConnection(config, peerConnectionObserver);

        List<String> streamIds = new ArrayList<>();
        streamIds.add("stream");
        // Add local tracks
        if (localVideoTrack != null) {
            videoSender = peerConnection.addTrack(localVideoTrack, streamIds);
        }
        if (localAudioTrack != null) {
            audioSender = peerConnection.addTrack(localAudioTrack, streamIds);
        }

        // Create data channel
        RTCDataChannelInit init = new RTCDataChannelInit();
        init.ordered = true;
        init.negotiated = false;

        RTCDataChannel dataChannel1 = peerConnection.createDataChannel("chat", init);
        setupDataChannel(dataChannel1);

        RTCDataChannel dataChannel2 = peerConnection.createDataChannel("game", init);
        setupDataChannel(dataChannel2);


//        Create offer
//        RTCMediaConstraints constraints = new MediaConstraints();
//        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
//        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        peerConnection.createOffer(new RTCOfferOptions(), new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription sdp) {
                peerConnection.setLocalDescription(sdp, new SetSessionDescriptionObserver() {
                    @Override
                    public void onSuccess() {
                        try {
                            SignalingClient.getInstance().sendOffer(sdp.sdp, remoteClientKey);
                        } catch (IOException ex) {
                            System.getLogger(WebRTCManager.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                        }
                    }

                    @Override
                    public void onFailure(String error) { }

                });
            }

            @Override
            public void onFailure(String error) { }
        });

        // Start statistics collection
        startStatisticsCollection();
    }

    public void handleOffer(String sdp, String sender) {

        initializeMedia();
        initializeCapture();

        this.remoteClientKey = sender;
        peerConnectionObserver = new PeerConnectionObserverImpl(this, remoteClientKey);
        if (videoHandler != null) {
            peerConnectionObserver.setVideoHandler(videoHandler);
        }
        
        peerConnection = factory.createPeerConnection(config, peerConnectionObserver);
        for (RTCIceCandidate candidate : candidates) {
            peerConnection.addIceCandidate(candidate);
        }
        candidates.clear();

        // Add local tracks
        if (localVideoTrack != null) {
            List<String> videoStreamIds = new ArrayList<>();
            videoStreamIds.add("videoStream1");
            peerConnection.addTrack(localVideoTrack, videoStreamIds);
        }
        if (localAudioTrack != null) {
            List<String> audioStreamIds = new ArrayList<>();
            audioStreamIds.add("audioStream1");
            peerConnection.addTrack(localAudioTrack, audioStreamIds);
        }

        // Set remote description
        RTCSessionDescription remoteSdp = new RTCSessionDescription(
            RTCSdpType.OFFER, sdp
        );

        peerConnection.setRemoteDescription(remoteSdp, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                // Create answer
                peerConnection.createAnswer(new RTCAnswerOptions(), new CreateSessionDescriptionObserver() {
                    @Override
                    public void onSuccess(RTCSessionDescription sdp) {
                        peerConnection.setLocalDescription(sdp, new SetSessionDescriptionObserver() {
                            @Override
                            public void onSuccess() {
                                try {
                                    SignalingClient.getInstance().sendAccept(sdp.sdp, remoteClientKey);
                                } catch (IOException ex) {
                                    System.getLogger(WebRTCManager.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                                }
                            }

                            @Override
                            public void onFailure(String error) { }
                        });
                    }

                    @Override
                    public void onFailure(String error) { }
                });
            }

            @Override
            public void onFailure(String error) { }
        });

        // Start statistics collection
        startStatisticsCollection();
    }

    public void handleAccept(String sdp) {
        RTCSessionDescription remoteSdp = new RTCSessionDescription(
            RTCSdpType.ANSWER, sdp
        );

        peerConnection.setRemoteDescription(remoteSdp, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                if (callManager != null) {
                    callManager.handleCallAccepted();
                }
            }

            @Override
            public void onFailure(String error) { }
        });
    }

    public void addIceCandidate(String candidate, String sdpMid, int sdpMLineIndex) {
        RTCIceCandidate iceCandidate = new RTCIceCandidate(sdpMid, sdpMLineIndex, candidate);
        if (peerConnection != null) {
            peerConnection.addIceCandidate(iceCandidate);
        } else {
            candidates.add(iceCandidate);
        }
    }

    public void setupDataChannel(RTCDataChannel dataChannel) {
        switch (dataChannel.getLabel()) {
            case "chat":
                chatDataChannel = dataChannel;
                dataChannel.registerObserver(new RTCDataChannelObserver() {
                    @Override
                    public void onBufferedAmountChange(long amount) {}

                    @Override
                    public void onStateChange() {}

                    @Override
                    public void onMessage(RTCDataChannelBuffer buffer) {
                        ByteBuffer data = buffer.data;
                        byte[] textBytes;

                        if (data.hasArray()) {
                            textBytes = data.array();
                        } else {
                            textBytes = new byte[data.remaining()];
                            data.get(textBytes);
                        }

                        String textEncrypted = new String(textBytes, StandardCharsets.UTF_8);
                        String text = MC.decryptMessage(textEncrypted);
                        
                        try {
                            System.out.println("GOT MESSAGE:");
                            System.out.println(text);
                            
                            JsonNode message = mapper.readTree(text);
                            String signature = message.get("signature").asText();
                            String sender = message.get("sender").asText();
                            String content = message.get("content").asText();
                            
                            
                            PublicKey publicKey = MessageCryptographer.stringToPublicKey(remoteClientKey);
                            boolean isVerified = MC.confirmSign(content, signature, publicKey);
                            if (!isVerified) {
                                System.out.println("Signature is false, skipping");
                                return;
                            }
                            
                            for (JavaPhoneChatHandler chatHandler : chatHandlers) {
                                if (chatHandler != null) {
                                    chatHandler.handleStringMessage(sender, content);
                                }
                            }

                        } catch (Exception ex) {
                            System.getLogger(WebRTCManager.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                        }
                    }
                });
                break;
           
            case "game":
                System.out.println("GOT GAME DATA CHANNEL");
                gameDataChannel = dataChannel;
                dataChannel.registerObserver(new RTCDataChannelObserver() {
                    @Override
                    public void onBufferedAmountChange(long amount) {}

                    @Override
                public void onStateChange() {
                    System.out.println("🎮 GAME CHANNEL IS OPEN!");
    isGameChannelReady = true;
    if (onGameChannelReadyCallback != null) {
        onGameChannelReadyCallback.run();
    }
    GameMenuApp.getInstance().onGameChannelReady();}

                    @Override
        public void onMessage(RTCDataChannelBuffer buffer) {
            ByteBuffer data = buffer.data;
            byte[] textBytes;

            if (data.hasArray()) {
                textBytes = data.array();
            } else {
                textBytes = new byte[data.remaining()];
                data.get(textBytes);
            }

            String text = new String(textBytes, StandardCharsets.UTF_8);
            try {
                System.out.println("🎮 [GAME] Получено сообщение:");
                System.out.println(text);
                JsonNode message = mapper.readTree(text);
                String sender = message.get("sender").asText();
                String content = message.get("content").asText();
               
                
                // Игнорируем свои сообщения, чтобы не обрабатывать их дважды
                javafx.application.Platform.runLater(() -> {
            String myId = getMyUserId();
            if (myId != null && !sender.equals(myId)) {
                GameMenuApp.getInstance().onGameMessage(content);
            } else if (myId == null) {
                GameMenuApp.getInstance().onGameMessage(content);
            }
        });} catch (Exception ex) {
                System.getLogger(WebRTCManager.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
                

        
    });
    break;
        }
    }
    public boolean isGameChannelReady() {
    return gameDataChannel != null && gameDataChannel.getState() == RTCDataChannelState.OPEN;
}

public void setOnGameChannelReady(Runnable callback) {
    this.onGameChannelReadyCallback = callback;
}
public String getMyUserId() {
    // Получаем clientId из SignalingClient
    try {
        return SignalingClient.getInstance().getClientId();
    } catch (Exception e) {
        // Fallback на nickname
        return SettingsManager.getInstance().getNickname();
    }
}
    public void sendChatMessage(String content) {
        if (chatDataChannel != null && chatDataChannel.getState() == RTCDataChannelState.OPEN) {
            try {
                ObjectNode message = mapper.createObjectNode();
                String sender = SettingsManager.getInstance().getNickname();
                String signature = MC.signMessage(content);
                message.put("sender", sender);
                message.put("content", content);
                message.put("signature", signature);
                     
                String textMessage = mapper.writeValueAsString(message);
                System.out.println("SENDING MESSAGE");
                System.out.println(textMessage);
                
                PublicKey publicKey = MessageCryptographer.stringToPublicKey(remoteClientKey);
                String textEncrypted = MC.encryptMessage(textMessage, publicKey);
                
                ByteBuffer textBuffer = ByteBuffer.wrap(textEncrypted.getBytes(StandardCharsets.UTF_8));
                RTCDataChannelBuffer textChannelBuffer = new RTCDataChannelBuffer(textBuffer, false);

                for (JavaPhoneChatHandler chatHandler : chatHandlers) {
                    if (chatHandler != null) {
                        chatHandler.handleStringMessage(sender, content);
                    }
                }

                chatDataChannel.send(textChannelBuffer);
            } catch (Exception ex) {
                Logger.getLogger(WebRTCManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("CHAT CHANNEL DOES NOT EXIST");
        }
    }



 
    public void sendGameMessage(String content) {
    if (gameDataChannel != null && gameDataChannel.getState() == RTCDataChannelState.OPEN) {
        try {
            ObjectNode message = mapper.createObjectNode();
            String sender = getMyUserId(); // Теперь используем clientId
            message.put("sender", sender);
            message.put("content", content);
            
            String textMessage = mapper.writeValueAsString(message);
            ByteBuffer textBuffer = ByteBuffer.wrap(textMessage.getBytes(StandardCharsets.UTF_8));
            RTCDataChannelBuffer textChannelBuffer = new RTCDataChannelBuffer(textBuffer, false);
            
            gameDataChannel.send(textChannelBuffer);
            System.out.println("📤 [GAME] Отправлено сообщение: " + content);
        } catch (Exception ex) {
            System.getLogger(WebRTCManager.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    } else {
        System.out.println("❌ GAME CHANNEL NOT READY. State: " + 
            (gameDataChannel != null ? gameDataChannel.getState() : "null"));
    }
}

    private void startStatisticsCollection() {
        RTCStatsType[] types = {RTCStatsType.PEER_CONNECTION, RTCStatsType.SENDER, RTCStatsType.RECEIVER, RTCStatsType.TRACK, RTCStatsType.MEDIA_SOURCE, RTCStatsType.TRANSPORT};
        statsExecutor.scheduleAtFixedRate(() -> {
            if (peerConnection != null) {
                peerConnection.getStats(new RTCStatsCollectorCallback() {
                    @Override
                    public void onStatsDelivered(RTCStatsReport report) {
                        StringBuilder stats = new StringBuilder();
                        for (RTCStats stat : report.getStats().values()){
                            for (RTCStatsType type : types) {
                                if (type == stat.getType()) {
                                    stats.append(stat.toString() + "\n");
                                    // TODO: print logs somewhere
                                }
                            }
                        }
                    }
                });
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    public void toggleCamera() {
        cameraEnabled = !cameraEnabled;
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(cameraEnabled);
        }
    }

    public void toggleMicrophone() {
        microphoneEnabled = !microphoneEnabled;
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(microphoneEnabled);
        }
    }

    public boolean isCameraEnabled() {
        return cameraEnabled;
    }

    public boolean isMicrophoneEnabled() {
        return microphoneEnabled;
    }

    public void hangup() {
        candidates.clear();
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }

        if (chatDataChannel != null) {
            chatDataChannel.close();
            chatDataChannel = null;
        }

        if (remoteClientKey != null) {
            // TODO: send bye through signaling client
            remoteClientKey = null;
        }

        statsExecutor.shutdown();

//        // Clear remote video
//        SwingUtilities.invokeLater(() -> {
//            if (remoteVideoPanel != null) {
//                remoteVideoPanel.release();
//            }
//        });
    }

    public void handleRemoteDisconnect() {
        hangup();
    }

    public void cleanup() {
        hangup();

//        if (localRenderer != null) {
//            localRenderer.release();
//        }

        if (videoSource != null) {
            videoSource.dispose();
        }

        if (factory != null) {
            factory.dispose();
        }
    }

    @Override
    public void onIceCandidate(RTCIceCandidate rtcic) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public List<AudioDevice> getMicrophones() {
        return microphones;
    }

    public List<VideoDevice> getCameras() {
        return cameras;
    }

    public List<AudioDevice> getSpeakers() {
        return speakers;
    }

    public void setCameraId(Integer newCameraId) {
        cameraId = newCameraId;
    }

    public Integer getCameraId() {
        return cameraId;
    }

    public void setMicrophoneId(Integer newMicrophoneId) {
        microphoneId = newMicrophoneId;
    }

    public Integer getMicrophoneId() {
        return microphoneId;
    }

    public void setSpeakerId(Integer newSpeakerId) {
        speakerId = newSpeakerId;
    }

    public Integer getSpeakerId() {
        return speakerId;
    }

    public void setMicrophoneVolume(Integer newMicrophoneVolume) {
        microphoneVolume = newMicrophoneVolume;
        if (audioModule != null) {
            audioModule.setMicrophoneVolume(microphoneVolume);
        }
    }

    public Integer getMicrophoneVolume() {
        return microphoneVolume;
    }

    public void setSpeakerVolume(Integer newSpeakerVolume) {
        speakerVolume = newSpeakerVolume;
        if (audioModule != null) {
            audioModule.setSpeakerVolume(speakerVolume);
        }
    }

    public Integer getSpeakerVolume() {
        return speakerVolume;
    }
    
    public void setCallManager(JavaPhoneCallManager callManager) {
        this.callManager = callManager;
    }
    
    public void setVideoHandler(JavaPhoneVideoHandler videoHandler) {
        this.videoHandler = videoHandler;
        
        if (videoHandler != null) {
            if (localVideoTrack != null) {
                this.videoHandler.addLocalTrack(localVideoTrack);
            }
            if (peerConnectionObserver != null) {
                peerConnectionObserver.setVideoHandler(videoHandler);
            }
        }
    }
    
    public boolean addChatHandler(JavaPhoneChatHandler chatHandler) {
        return this.chatHandlers.add(chatHandler);
    }
    
    public boolean removeChatHandler(JavaPhoneChatHandler chatHandler) {
        return this.chatHandlers.remove(chatHandler);
    }
}
