package com.mycompany.javaphone_nir2.games;

import javafx.stage.Stage;

public abstract class FXGLGame {
    
    protected String gameType;
    protected GameController controller;
    protected boolean isMyTurn = true;
    protected boolean gameActive = true;
    protected boolean waitingForOpponent = true;
    protected boolean amIHost = false;
    private Runnable onCloseCallback;
    protected Stage gameStage;
    
    public void setGameType(String gameType) {
        this.gameType = gameType;
    }
    
    public void setController(GameController controller) {
        this.controller = controller;
    }
    
    public void setAmIHost(boolean amIHost) {
        this.amIHost = amIHost;
    }
    
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }
    
    /**
     * Called when opponent's move is received
     */
    public abstract void onOpponentMove(String moveData);
    
    /**
     * Called when battle starts (both players are ready)
     */
    public abstract void startBattle();
    
    /**
     * Cleans up resources
     */
    public abstract void cleanup();
    
    /**
     * Displays the game UI
     */
    public abstract void showUI();
    
    /**
     * Sends a move to the opponent via GameController
     */
    protected void sendMove(String moveData) {
        if (controller != null) {
            controller.sendMove(moveData);
        }
    }
    
    /**
     * Launches the game window
     */
    public void launchGame() {
        showUI();
        if (gameStage != null) {
            gameStage.setOnCloseRequest(e -> {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
            });
            gameStage.show();
        }
    }
}