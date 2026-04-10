/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphone_nir2.games.sea_battle;


public class SeaBattleCell {
    
    // Cell states
    public static final int EMPTY = 0;
    public static final int SHIP = 1;
    public static final int HIT = 2;
    public static final int MISS = 3;
    
    private int state;
    private int shipId; // Which ship this cell belongs to (-1 if none)
    
    public SeaBattleCell() {
        this.state = EMPTY;
        this.shipId = -1;
    }
    
    public int getState() { return state; }
    public void setState(int state) { this.state = state; }
    
    public int getShipId() { return shipId; }
    public void setShipId(int shipId) { this.shipId = shipId; }
    
    public boolean isEmpty() { return state == EMPTY; }
    public boolean isShip() { return state == SHIP; }
    public boolean isHit() { return state == HIT; }
    public boolean isMiss() { return state == MISS; }
    
    public void markAsShip(int shipId) {
        this.state = SHIP;
        this.shipId = shipId;
    }
    
    public void markAsHit() {
        this.state = HIT;
    }
    
    public void markAsMiss() {
        this.state = MISS;
    }
    
    public void reset() {
        this.state = EMPTY;
        this.shipId = -1;
    }
}