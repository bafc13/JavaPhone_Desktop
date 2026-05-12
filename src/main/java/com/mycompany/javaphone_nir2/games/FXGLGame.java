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
        this.themeHelper.init();
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

    // Абстрактные методы
    public abstract void onOpponentMove(String moveData);

    public abstract void startBattle();

    public abstract void cleanup();

    public abstract void showUI();

    // Вспомогательный метод для отправки хода
    protected void sendMove(String moveData) {
        if (controller != null) {
            controller.sendMove(moveData);
        }
    }

    // Запуск игры
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

    // Применить тему к сцене (использует CSS чата)
    protected void applyThemeToScene(javafx.scene.Scene scene) {
        if (themeHelper != null && scene != null) {
            themeHelper.applyThemeToScene(scene);
        }
    }

    // Подписаться на изменения темы
    protected void listenToThemeChanges(Runnable onThemeChanged) {
        if (themeHelper != null) {
            themeHelper.addThemeListener(onThemeChanged);
        }
    }
}
