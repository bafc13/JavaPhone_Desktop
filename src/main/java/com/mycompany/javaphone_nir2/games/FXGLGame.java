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
    protected GameThemeHelper themeHelper;
    
    public FXGLGame() {
        this.themeHelper = GameThemeHelper.getInstance();
    }
    
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
    
    public abstract void onOpponentMove(String moveData);
    public abstract void startBattle();
    public abstract void cleanup();
    public abstract void showUI();
    
    protected void sendMove(String moveData) {
        if (controller != null) {
            controller.sendMove(moveData);
        }
    }
    
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
    
    protected void applyThemeToScene(javafx.scene.Scene scene) {
        if (themeHelper != null && scene != null) {
            themeHelper.applyThemeToScene(scene);
        }
    }
    
    protected String getBackgroundColor() {
        return themeHelper != null ? themeHelper.getBackgroundColor() : "#f5f5f5";
    }
    
    protected String getTextColor() {
        return themeHelper != null ? themeHelper.getTextColor() : "#000000";
    }
    
    protected String getBoardColor() {
        return themeHelper != null ? themeHelper.getBoardColor() : "#ffffff";
    }
    
    protected String getGridColor() {
        return themeHelper != null ? themeHelper.getGridColor() : "#333333";
    }
    
    protected String getXColor() {
        return themeHelper != null ? themeHelper.getXColor() : "#e74c3c";
    }
    
    protected String getOColor() {
        return themeHelper != null ? themeHelper.getOColor() : "#3498db";
    }
}