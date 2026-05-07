/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.javaphone_nir2.webrtc;

import com.mycompany.javaphone_nir2.models.Contact;
import com.mycompany.javaphone_nir2.models.Offer;

/**
 *
 * @author Andrey
 */
public interface JavaPhoneCallManager {
    public void handleContact(Contact contact);
    public void handleIncomingCall(Offer offer);
    public void handleIncomingCall(String callerKey);
}
