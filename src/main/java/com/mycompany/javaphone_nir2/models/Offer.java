package com.mycompany.javaphone_nir2.models;

/**
 * Offer class includes offer sender and sdp
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
