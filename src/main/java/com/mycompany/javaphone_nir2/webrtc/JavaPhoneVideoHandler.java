/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.javaphone_nir2.webrtc;

import dev.onvoid.webrtc.media.video.VideoTrack;

/**
 *
 * @author Andrey
 */
public interface JavaPhoneVideoHandler {

    public void addLocalTrack(VideoTrack localTrack);

    public void addRemoteTrack(VideoTrack remoteTrack);
}
