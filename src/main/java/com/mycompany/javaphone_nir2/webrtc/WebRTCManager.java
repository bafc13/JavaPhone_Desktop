package com.mycompany.javaphone_nir2.webrtc;

import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.*;
import dev.onvoid.webrtc.media.video.*;
import dev.onvoid.webrtc.media.audio.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebRTCManager implements PeerConnectionObserver {
    private PeerConnectionFactory factory;
    
    private RTCConfiguration config;
    
    private VideoDeviceSource videoSource;
    private AudioSource audioSource;
    
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    
    private VideoTrack remoteVideoTrack;
    private AudioTrack remoteAudioTrack;
    
    private RTCPeerConnection peerConnection;
    private RTCRtpSender videoSender;
    private RTCRtpSender audioSender;
    public RTCDataChannel dataChannel;
    
    private List<VideoDevice> cameras;
    private List<VideoCaptureCapability> capabilities;
    
    private boolean cameraEnabled = true;
    private boolean microphoneEnabled = true;
    private String remoteClientId;
    
    private Integer cameraId = 0;
    
    private final ScheduledExecutorService statsExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // STUN servers
    private static RTCIceServer ICE_SERVERS = null;
    
    public WebRTCManager() {
        initializeWebRTC();
    }
    
    private void initializeWebRTC() {
        if (ICE_SERVERS == null) {
            ICE_SERVERS = new RTCIceServer();
            ICE_SERVERS.urls.add("stun:stun.l.google.com:19302");
//            ICE_SERVERS.urls.add("stun:stun1.l.google.com:19302");
        }
        
        factory = new PeerConnectionFactory();
        config = new RTCConfiguration();
        config.iceServers.add(ICE_SERVERS);
        
        // TODO: Update UI
    }
    
    public void initializeMedia() {
        cameras = MediaDevices.getVideoCaptureDevices();
        
        VideoDevice camera = cameras.get(cameraId);

        capabilities = MediaDevices.getVideoCaptureCapabilities(camera);
        VideoCaptureCapability capability = capabilities.get(0);

        videoSource = new VideoDeviceSource();
        videoSource.setVideoCaptureDevice(camera);
        videoSource.setVideoCaptureCapability(capability);
        videoSource.start();    

        localVideoTrack = factory.createVideoTrack("video0", videoSource);
        // to display add sink
    }
    
    public void startCall(String targetClientId) {
        remoteClientId = targetClientId;
        
        peerConnection = factory.createPeerConnection(config, new PeerConnectionObserverImpl(this, remoteClientId));
        
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
        dataChannel = peerConnection.createDataChannel("data", init);
        setupDataChannel(dataChannel);
        
//        Create offer
//        RTCMediaConstraints constraints = new MediaConstraints();
//        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
//        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        peerConnection.createOffer(new RTCOfferOptions(), new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription sdp) {
                peerConnection.setLocalDescription(sdp, new SetSessionDescriptionObserver() {
                    @Override
                    public void onSuccess() { }
                    
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
        this.remoteClientId = sender;
        
        peerConnection = factory.createPeerConnection(config, new PeerConnectionObserverImpl(this, remoteClientId));
        
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
                            public void onSuccess() { }
                            
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
    
    public void handleAnswer(String sdp) {
        RTCSessionDescription remoteSdp = new RTCSessionDescription(
            RTCSdpType.ANSWER, sdp
        );
        
        peerConnection.setRemoteDescription(remoteSdp, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() { }
            
            @Override
            public void onFailure(String error) { }
        });
    }
    
    public void addIceCandidate(String candidate, String sdpMid, int sdpMLineIndex) {
        RTCIceCandidate iceCandidate = new RTCIceCandidate(sdpMid, sdpMLineIndex, candidate);
        peerConnection.addIceCandidate(iceCandidate);
    }
    
    public void setupDataChannel(RTCDataChannel dataChannel) {
        dataChannel.registerObserver(new RTCDataChannelObserver() {
            @Override
            public void onBufferedAmountChange(long amount) {}
            
            @Override
            public void onStateChange() {}
            
            @Override
            public void onMessage(RTCDataChannelBuffer buffer) {
                // TODO: do something with received data
            }
        });
    }
    
    public void sendDataMessage(String message) {
        if (dataChannel != null && dataChannel.getState() == RTCDataChannelState.OPEN) {
            try {
                dataChannel.send(new RTCDataChannelBuffer(ByteBuffer.wrap(message.getBytes()), false));
            } catch (Exception ex) {
                Logger.getLogger(WebRTCManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            
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
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        
        if (dataChannel != null) {
            dataChannel.close();
            dataChannel = null;
        }
        
        if (remoteClientId != null) {
            // TODO: send bye through signaling client
            remoteClientId = null;
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
    
    public void setCameraId(Integer newCameraId) {
        cameraId = newCameraId;
    }
}