/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphone_nir2.games.sea_battle;


import java.util.ArrayList;
import java.util.List;

public class SeaBattleShip {
    
    private int id;
    private int size;
    private List<int[]> cells; // List of [row, col] positions
    private boolean[] hits;    // Hit status for each cell
    private boolean isDestroyed;
    
    public SeaBattleShip(int id, int size) {
        this.id = id;
        this.size = size;
        this.cells = new ArrayList<>();
        this.hits = new boolean[size];
        this.isDestroyed = false;
    }
    
    public int getId() { return id; }
    public int getSize() { return size; }
    public List<int[]> getCells() { return cells; }
    public boolean isDestroyed() { return isDestroyed; }
    
    // Add a cell to the ship (during placement)
    public void addCell(int row, int col) {
        cells.add(new int[]{row, col});
    }
    
    // Check if ship contains a specific cell
    public boolean contains(int row, int col) {
        for (int[] cell : cells) {
            if (cell[0] == row && cell[1] == col) {
                return true;
            }
        }
        return false;
    }
    
    // Process a hit on this ship
    // Returns true if the ship is now fully destroyed
    public boolean hit(int row, int col) {
        for (int i = 0; i < cells.size(); i++) {
            int[] cell = cells.get(i);
            if (cell[0] == row && cell[1] == col) {
                hits[i] = true;
                break;
            }
        }
        
        // Check if all cells are hit
        boolean allHit = true;
        for (boolean hit : hits) {
            if (!hit) {
                allHit = false;
                break;
            }
        }
        
        if (allHit && !isDestroyed) {
            isDestroyed = true;
            return true; // Ship just got destroyed
        }
        
        return false; // Ship still alive
    }
    
    // Get surrounding area for marking misses after destruction
    public List<int[]> getSurroundingArea(int boardSize) {
        List<int[]> area = new ArrayList<>();
        
        // Find min and max row/col
        int minRow = 10, maxRow = -1, minCol = 10, maxCol = -1;
        for (int[] cell : cells) {
            minRow = Math.min(minRow, cell[0]);
            maxRow = Math.max(maxRow, cell[0]);
            minCol = Math.min(minCol, cell[1]);
            maxCol = Math.max(maxCol, cell[1]);
        }
        
        // Expand by 1 in all directions
        for (int r = minRow - 1; r <= maxRow + 1; r++) {
            for (int c = minCol - 1; c <= maxCol + 1; c++) {
                if (r >= 0 && r < boardSize && c >= 0 && c < boardSize) {
                    // Don't include the ship's own cells
                    if (!contains(r, c)) {
                        area.add(new int[]{r, c});
                    }
                }
            }
        }
        
        return area;
    }
}