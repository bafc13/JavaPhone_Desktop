package com.mycompany.javaphone_nir2.games;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class GameController {

    private static GameController instance;
    private FXGLGame currentGame;
    private String currentGameType;
    private boolean gameActive = false;
    private boolean handshakeSent = false;
    private boolean handshakeReceived = false;
    private boolean amIHost = false;

    private GameController() {
        System.out.println("🎮 GameController is created");
    }

    public static GameController getInstance() {
        if (instance == null) {
            instance = new GameController();
        }
        return instance;
    }

    // Called when user selects a game from the menu
    public void onGameSelected(String gameType) {
        System.out.println("🎮 [Controller] Game chosen: " + gameType);
        this.currentGameType = gameType;
        this.amIHost = true;
        this.handshakeSent = false;
        this.handshakeReceived = false;
        sendHandshake();
    }

    // Sends handshake to opponent
    private void sendHandshake() {
        if (handshakeSent) {
            System.out.println("⚠️ [Controller] Handshake already sent, skipping");
            return;
        }

        String message = "handshake|" + currentGameType;
        GameMenuApp.getInstance().sendGameMessage(message);
        handshakeSent = true;
        System.out.println("📤 [Controller] Handshake sent for game: " + currentGameType);
        System.out.println("   handshakeSent = " + handshakeSent);
        System.out.println("   handshakeReceived = " + handshakeReceived);

        if (handshakeReceived) {
            System.out.println("→ BOTH ready! Starting game...");
            startGame();
        } else {
            System.out.println("→ Waiting for opponent's handshake...");
        }
    }

    // Called when the game data channel is ready
    public void onGameChannelReady() {
        System.out.println("🎮 [Controller] Game channel is ready!");
    }

    // Main message handler - processes all incoming messages
    public void handleMessage(String message) {
        System.out.println("🎮 [Controller] Message processing: " + message);

        // Find first separator
        int firstPipe = message.indexOf('|');
        if (firstPipe == -1) {
            System.err.println("❌ Invalid message format");
            return;
        }

        String type = message.substring(0, firstPipe);
        String remaining = message.substring(firstPipe + 1);

        System.out.println("   type=" + type + ", remaining=" + remaining);

        switch (type) {
            case "handshake":
                handleHandshakeMessage(remaining);
                break;
            case "move":
                handleMoveMessage(remaining);
                break;
            case "result":
                handleResultMessage(remaining);
                break;
            default:
                System.err.println("❌ Unknown message type: " + type);
        }
    }

    private void handleHandshakeMessage(String remaining) {
        // remaining = "sea_battle"
        String gameType = remaining;
        System.out.println("   Handshake for game: " + gameType);
        onHandshakeReceived(gameType);
    }

    private void handleMoveMessage(String remaining) {
        // remaining = "sea_battle|5,5"
        int pipe = remaining.indexOf('|');
        if (pipe == -1) {
            System.err.println("❌ Invalid move format");
            return;
        }

        String gameType = remaining.substring(0, pipe);
        String content = remaining.substring(pipe + 1);

        System.out.println("   Move: gameType=" + gameType + ", content=" + content);

        if (!gameActive || currentGame == null) {
            System.err.println("❌ No active game");
            return;
        }

        if (!gameType.equals(currentGameType)) {
            System.err.println("⚠️ Message for different game: " + gameType);
            return;
        }
        // Special handling for chess - content is like "e2e4"
        if ("chess".equals(gameType)) {
            System.out.println("   Chess move detected: " + content);
        }

        currentGame.onOpponentMove(content);
    }

    private void handleResultMessage(String remaining) {
        // remaining = "sea_battle|miss|5,5"
        int firstPipe = remaining.indexOf('|');
        if (firstPipe == -1) {
            System.err.println("❌ Invalid result format");
            return;
        }

        String gameType = remaining.substring(0, firstPipe);
        String rest = remaining.substring(firstPipe + 1);

        // rest = "miss|5,5"
        int secondPipe = rest.indexOf('|');
        if (secondPipe == -1) {
            System.err.println("❌ Invalid result format (no second pipe)");
            return;
        }

        String result = rest.substring(0, secondPipe);
        String coords = rest.substring(secondPipe + 1);

        System.out.println("   Result: gameType=" + gameType + ", result=" + result + ", coords=" + coords);

        if (!gameActive || currentGame == null) {
            System.err.println("❌ No active game");
            return;
        }

        if (!gameType.equals(currentGameType)) {
            System.err.println("⚠️ Message for different game: " + gameType);
            return;
        }

        // Combine result and coords for the module
        String resultData = result + "|" + coords;
        System.out.println("   Passing to module: " + resultData);

        if (currentGame instanceof SeaBattleModule) {
            ((SeaBattleModule) currentGame).onShotResult(resultData);
        }
    }

    // Processes received handshake
    private void onHandshakeReceived(String gameType) {
        System.out.println("📨 [Controller] Handshake received for game: " + gameType);
        System.out.println("   currentGameType = " + currentGameType);
        System.out.println("   handshakeSent = " + handshakeSent);
        System.out.println("   handshakeReceived = " + handshakeReceived);

        if (currentGameType == null) {
            // Opponent is inviting us
            System.out.println("→ Showing game proposal (currentGameType is null)");
            currentGameType = gameType;
            handshakeReceived = true;
            amIHost = false;
            showGameProposal(gameType);
        } else if (currentGameType.equals(gameType)) {
            System.out.println("→ Games match");
            handshakeReceived = true;

            if (handshakeSent) {
                System.out.println("→ Both sent handshake, starting game NOW!");
                startGame();
            } else {
                System.out.println("→ Sending response handshake");
                sendHandshake();
            }
        } else {
            System.out.println("→ Error: different games selected");
            showError("Opponent chose a different game");
        }
    }

    // Shows game invitation dialog
    private void showGameProposal(String gameType) {
        String displayName = getGameDisplayName(gameType);

        Stage proposalStage = new Stage();
        proposalStage.setTitle("Приглашение");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 30; -fx-background-color: #2c3e50;");

        Text title = new Text("Приглашение на игру");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #ecf0f1;");

        Text info = new Text("Соперник хочет играть в " + displayName);
        info.setStyle("-fx-font-size: 14px; -fx-fill: #bdc3c7;");

        Button acceptButton = new Button("Принять и играть");
        acceptButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-color: #27ae60; -fx-text-fill: white;");
        acceptButton.setOnAction(e -> {
            System.out.println("→ Invitation ACCEPTED");
            System.out.println("   currentGameType = " + currentGameType);
            System.out.println("   handshakeSent = " + handshakeSent);
            System.out.println("   handshakeReceived = " + handshakeReceived);

            // Note: currentGameType is already set when proposal was shown
            handshakeReceived = true;
            amIHost = false;

            System.out.println("   After setting: handshakeReceived = " + handshakeReceived);

            // Send response handshake
            sendHandshake();

            proposalStage.close();
        });

        Button rejectButton = new Button("Отказаться");
        rejectButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-color: #e74c3c; -fx-text-fill: white;");
        rejectButton.setOnAction(e -> {
            System.out.println("→ Invitation DECLINED");
            proposalStage.close();
            resetState();
        });

        root.getChildren().addAll(title, info, acceptButton, rejectButton);

        Scene scene = new Scene(root, 350, 250);
        proposalStage.setScene(scene);
        proposalStage.show();
    }

    // Shows error dialog
    private void showError(String message) {
        Stage errorStage = new Stage();
        errorStage.setTitle("Ошибка");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 30; -fx-background-color: #2c3e50;");

        Text title = new Text("Ошибка");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #e74c3c;");

        Text info = new Text(message);
        info.setStyle("-fx-font-size: 14px; -fx-fill: #bdc3c7;");

        Button okButton = new Button("Понятно");
        okButton.setOnAction(e -> errorStage.close());

        root.getChildren().addAll(title, info, okButton);

        Scene scene = new Scene(root, 400, 250);
        errorStage.setScene(scene);
        errorStage.show();
    }

    // Starts the actual game module
    private void startGame() {
        if (gameActive) {
            System.out.println("⚠️ Game already active, skipping start");
            return;
        }

        System.out.println("🎮 Starting game: " + currentGameType);
        System.out.println("   amIHost = " + amIHost + " (first move: " + (amIHost ? "Me" : "Opponent") + ")");

        gameActive = true;

        switch (currentGameType) {
            case "tic-tac-toe":
                currentGame = new TicTacToeModule();
                break;
            case "sea_battle":
                currentGame = new SeaBattleModule();
                break;
            case "chess":
                currentGame = new ChessModule();
                break;
            default:
                return;
        }

        currentGame.setGameType(currentGameType);
        currentGame.setController(this);
        currentGame.setAmIHost(amIHost);
        currentGame.setOnCloseCallback(() -> {
            GameMenuApp.getInstance().closeGame();
        });

        currentGame.launchGame();
        // Small delay to ensure UI is ready
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            javafx.application.Platform.runLater(() -> {
                currentGame.startBattle();
            });
        });

        // Reset handshake flags AFTER game is launched
        handshakeSent = false;
        handshakeReceived = false;
    }

    // Sends a move to the opponent
    public void sendMove(String moveData) {
        if (!gameActive) {
            return;
        }

        System.out.println("📤 sendMove called with: '" + moveData + "'");

        String prefix;
        String content;

        if (moveData.contains("|")) {
            // This is a shot result (hit|5,3 or miss|5,3 or kill|5,3)
            prefix = "result";
            content = moveData;  // Keep the whole string "hit|5,3"
            System.out.println("   Detected as RESULT, content: " + content);
        } else {
            // This is a regular move (e.g., "0,0" or "5,3")
            prefix = "move";
            content = moveData;
            System.out.println("   Detected as MOVE, content: " + content);
        }

        String message = prefix + "|" + currentGameType + "|" + content;
        System.out.println("📤 Sending message: " + message);
        GameMenuApp.getInstance().sendGameMessage(message);
    }

    // Starts the battle (called when both players are ready)
    public void startBattle() {
        if (currentGame != null) {
            currentGame.startBattle();
        }
    }

    // Cleans up the current game
    public void closeGame() {
        gameActive = false;
        if (currentGame != null) {
            currentGame.cleanup();
            currentGame = null;
        }
        currentGameType = null;
        handshakeSent = false;
        handshakeReceived = false;
        amIHost = false;
    }

    private String getGameDisplayName(String gameType) {
        switch (gameType) {
            case "tic-tac-toe":
                return "Крестики-нолики";
            case "sea_battle":
                return "Морской бой";
            case "chess":
                return "Шахматы";
            default:
                return gameType;
        }
    }

    private void resetState() {
        currentGameType = null;
        handshakeSent = false;
        handshakeReceived = false;
        amIHost = false;
    }

    public boolean isGameActive() {
        return gameActive;
    }
}
