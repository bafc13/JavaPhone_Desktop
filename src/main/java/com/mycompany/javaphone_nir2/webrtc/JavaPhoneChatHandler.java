/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.javaphone_nir2.webrtc;

import com.mycompany.javaphone_nir2.models.Message;
import java.io.File;

/**
 *
 * @author Andrey
 */
public interface JavaPhoneChatHandler {

    public void handleStringMessage(String sender, String content);

    public void handleMessage(Message message);

    public void handleFileMessage(File file, String sender);
}
