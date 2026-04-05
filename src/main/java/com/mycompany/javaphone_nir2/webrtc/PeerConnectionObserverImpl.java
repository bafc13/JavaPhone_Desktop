/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphone_nir2.webrtc;

import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCRtpReceiver;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCSignalingState;
import dev.onvoid.webrtc.media.MediaStream;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.video.VideoTrack;

/**
 *
 * @author Andrey
 */
public class PeerConnectionObserverImpl implements PeerConnectionObserver {
    WebRTCManager webRTCManager;    
    String remoteClientId;
    int candidates = 0;
    
    public PeerConnectionObserverImpl (WebRTCManager webRTCManager, String remoteClientId) {
        this.webRTCManager = webRTCManager;
        this.remoteClientId = remoteClientId;
    }
    
    @Override
    public void onIceCandidate(RTCIceCandidate iceCandidate) {
        System.out.println("Got candidate #" + String.valueOf(candidates++));
        
//        TODO: send ice candidate through signaling client
//        signalingClient.sendIceCandidate(
//                iceCandidate.sdp,
//                iceCandidate.sdpMid,
//                iceCandidate.sdpMLineIndex,
//                remoteClientId
//        );
    }

    @Override
    public void onDataChannel(RTCDataChannel dataChannel) {
        webRTCManager.setupDataChannel(dataChannel);
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        if (mediaStream.getVideoTracks().length > 0) {
            // TODO: add sink to track
        }
        // logic on call started
    }

    @Override
    public void onIceConnectionChange(RTCIceConnectionState iceConnectionState) {
        
    }

    @Override
    public void onSignalingChange(RTCSignalingState signalingState) {
        
    }

    @Override
    public void onConnectionChange(RTCPeerConnectionState newState) {
        
    }

    @Override
    public void onRenegotiationNeeded() {
        
    }
    
    @Override
    public void onAddTrack(RTCRtpReceiver receiver, MediaStream[] mediaStreams) {
        if (mediaStreams == null) {
            return;
        }
        for (MediaStream stream : mediaStreams) {
            if (stream.getVideoTracks() == null) {
                continue;
            }
            for (VideoTrack track : stream.getVideoTracks()) {
                // TODO: add sink to track
            }
        }
    }
    
    @Override
    public void onRemoveTrack(RTCRtpReceiver receiver) {
        // TODO: remove sink
    }
    
    @Override
    public void onTrack(RTCRtpTransceiver transceiver) {
        MediaStreamTrack track = transceiver.getReceiver().getTrack();
        String kind = track.getKind();

        if (kind.equals(MediaStreamTrack.VIDEO_TRACK_KIND)) {
            VideoTrack videoTrack = (VideoTrack) track;
            // TODO: add sink
        }
    } 
}
