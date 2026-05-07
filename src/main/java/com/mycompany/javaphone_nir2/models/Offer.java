/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphone_nir2.models;

/**
 *
 * @author bafc13
 */
public class Offer {

    private String sdp;
    private String sender;

    public Offer(String sdp, String sender) {
        this.sdp = sdp;
        this.sender = sender;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void clear() {
        this.sdp = "";
        this.sender = "";
    }

}
