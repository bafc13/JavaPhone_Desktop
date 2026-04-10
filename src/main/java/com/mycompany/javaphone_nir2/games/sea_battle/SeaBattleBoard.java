/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphone_nir2.games.sea_battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SeaBattleBoard {
    
    private static final int SIZE = 10;
    
    private SeaBattleCell[][] grid;
    private List<SeaBattleShip> ships;
    private int shipsAlive;
    private int nextShipId;
    
    public SeaBattleBoard() {
        this.grid = new SeaBattleCell[SIZE][SIZE];
        this.ships = new ArrayList<>();
        this.shipsAlive = 0;
        this.nextShipId = 0;
        
        // Initialize empty grid
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = new SeaBattleCell();
            }
        }
    }
    
    public int getSize() { return SIZE; }
    public SeaBattleCell getCell(int row, int col) { return grid[row][col]; }
    public List<SeaBattleShip> getShips() { return ships; }
    public int getShipsAlive() { return shipsAlive; }
    
    // Randomly place all ships
    public void randomizeShips() {
        reset();
        
        int[] shipSizes = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
        Random random = new Random();
        
        for (int size : shipSizes) {
            boolean placed = false;
            int attempts = 0;
            
            while (!placed && attempts < 1000) {
                boolean horizontal = random.nextBoolean();
                int row = random.nextInt(SIZE);
                int col = random.nextInt(SIZE);
                
                if (canPlaceShip(size, row, col, horizontal)) {
                    placeShip(size, row, col, horizontal);
                    placed = true;
                }
                attempts++;
            }
        }
        
        System.out.println("🚢 Ships placed randomly, total: " + ships.size());
    }
    
    // Check if a ship can be placed at given position
    private boolean canPlaceShip(int size, int startRow, int startCol, boolean horizontal) {
        // Check boundaries
        if (horizontal && startCol + size > SIZE) return false;
        if (!horizontal && startRow + size > SIZE) return false;
        
        // Check all cells and their neighbors (ships cannot touch)
        for (int i = -1; i <= size; i++) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int row = startRow + (horizontal ? dr : i);
                    int col = startCol + (horizontal ? i : dc);
                    
                    if (row >= 0 && row < SIZE && col >= 0 && col < SIZE) {
                        if (grid[row][col].isShip()) {
                            return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    // Place a ship on the board
    private void placeShip(int size, int startRow, int startCol, boolean horizontal) {
        SeaBattleShip ship = new SeaBattleShip(nextShipId++, size);
        
        for (int i = 0; i < size; i++) {
            int row = startRow + (horizontal ? 0 : i);
            int col = startCol + (horizontal ? i : 0);
            
            grid[row][col].markAsShip(ship.getId());
            ship.addCell(row, col);
        }
        
        ships.add(ship);
        shipsAlive++;
    }
    
    // Process a shot on this board
    // Returns: 0 - miss, 1 - hit, 2 - ship destroyed, -1 - already shot there
    public int receiveShot(int row, int col) {
    SeaBattleCell cell = grid[row][col];
    
    System.out.println("   SeaBattleBoard.receiveShot at [" + row + "," + col + "], current state=" + cell.getState());
    
    if (cell.isHit() || cell.isMiss()) {
        System.out.println("   Already shot here!");
        return -1;
    }
    
    if (cell.isShip()) {
        cell.markAsHit();
        System.out.println("   HIT! State changed to HIT");
        
        // Find the ship...
        for (SeaBattleShip ship : ships) {
            if (ship.contains(row, col)) {
                boolean shipDestroyed = ship.hit(row, col);
                if (shipDestroyed) {
                    shipsAlive--;
                    markSurroundingArea(ship);
                    System.out.println("   SHIP DESTROYED! shipsAlive now: " + shipsAlive);
                    return 2;
                }
                System.out.println("   HIT, ship still alive");
                return 1;
            }
        }
        return 1;
    } else {
        cell.markAsMiss();
        System.out.println("   MISS! State changed to MISS");
        return 0;
    }
}
    
    // Mark all cells around a destroyed ship as MISS
    private void markSurroundingArea(SeaBattleShip ship) {
        List<int[]> surrounding = ship.getSurroundingArea(SIZE);
        for (int[] pos : surrounding) {
            int row = pos[0];
            int col = pos[1];
            SeaBattleCell cell = grid[row][col];
            
            if (cell.isEmpty()) {
                cell.markAsMiss();
            }
        }
    }
    
    // Get visible board for opponent (only HIT and MISS visible)
    public int[][] getVisibleBoard() {
        int[][] visible = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                SeaBattleCell cell = grid[i][j];
                if (cell.isHit()) {
                    visible[i][j] = SeaBattleCell.HIT;
                } else if (cell.isMiss()) {
                    visible[i][j] = SeaBattleCell.MISS;
                } else {
                    visible[i][j] = SeaBattleCell.EMPTY;
                }
            }
        }
        return visible;
    }
    
    // Get full board for player (ships visible)
    public int[][] getFullBoard() {
        int[][] full = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                full[i][j] = grid[i][j].getState();
            }
        }
        return full;
    }
    
    // Check if all ships are destroyed
    public boolean allShipsDestroyed() {
        return shipsAlive == 0;
    }
    
    // Reset the board
    public void reset() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j].reset();
            }
        }
        ships.clear();
        shipsAlive = 0;
        nextShipId = 0;
    }
    
    
    // For debugging - print board
    public void printBoard() {
        System.out.println("  A B C D E F G H I J");
        for (int i = 0; i < SIZE; i++) {
            System.out.print((i + 1) + " ");
            for (int j = 0; j < SIZE; j++) {
                SeaBattleCell cell = grid[i][j];
                if (cell.isShip()) {
                    System.out.print("■ ");
                } else if (cell.isHit()) {
                    System.out.print("💥 ");
                } else if (cell.isMiss()) {
                    System.out.print("· ");
                } else {
                    System.out.print("□ ");
                }
            }
            System.out.println();
        }
    }
}