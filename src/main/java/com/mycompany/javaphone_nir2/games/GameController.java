package com.mycompany.javaphone_nir2.games;

import com.mycompany.javaphone_nir2.models.SettingsManager;
import javafx.application.Platform;
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
    private GameThemeHelper themeHelper;

    private long myHandshakeTimestamp = 0;
    private long opponentHandshakeTimestamp = 0;
    private boolean conflictResolved = false;

    private GameController() {
        System.out.println("🎮 GameController is created");
        // Инициализация хелпера тем
        themeHelper = GameThemeHelper.getInstance();
        themeHelper.init();
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
        this.conflictResolved = false;
        sendHandshake();
    }

    // Sends handshake to opponent (with timestamp)
    private void sendHandshake() {
        if (handshakeSent) {
            System.out.println("⚠️ [Controller] Handshake already sent, skipping");
            return;
        }

        myHandshakeTimestamp = System.currentTimeMillis();
        String message = "handshake|" + currentGameType + "|" + myHandshakeTimestamp;
        GameMenuApp.getInstance().sendGameMessage(message);
        handshakeSent = true;
        System.out.println("📤 [Controller] Handshake sent for game: " + currentGameType + " (timestamp=" + myHandshakeTimestamp + ")");

        if (handshakeReceived && !conflictResolved) {
            System.out.println("→ BOTH ready! Starting game...");
            resolveConflictAndStart();
        } else {
            System.out.println("→ Waiting for opponent's handshake...");
        }
    }

private void sendChatMessage(String message) {
    GameMenuApp.getInstance().sendChatMessage(message);
}

// Отправка приглашения
private void sendInvitationToChat() {
    String gameName = getGameDisplayName(currentGameType);
    String message = "Приглашаю сыграть в " + gameName + "!";
    sendChatMessage(message);
}

// Приглашение принято
public void sendInvitationAccepted() {
    String gameName = getGameDisplayName(currentGameType);
    String message = "Приглашение принято! Игра в " + gameName + " началась!";
    sendChatMessage(message);
}

// Приглашение отклонено
public void sendInvitationRejected() {
    String message = "Приглашение отклонено.";
    sendChatMessage(message);
}

// Сообщение о победе (отправляет победитель)
public void sendVictoryMessage() {
    String message = "Я победил! Спасибо за игру!";
    sendChatMessage(message);
}

// Сообщение о поражении (отправляет проигравший)
public void sendDefeatMessage() {
    String message = "Поздравляю с победой! Спасибо за игру!";
    sendChatMessage(message);
}

// Сообщение о ничьей
public void sendDrawMessage() {
    String message = "Ничья! Спасибо за интересную игру!";
    sendChatMessage(message);
}
    public void onGameChannelReady() {
        System.out.println("🎮 [Controller] Game channel is ready!");
    }

    // Main message handler
    public void handleMessage(String message) {
        System.out.println("🎮 [Controller] Message processing: " + message);

        int firstPipe = message.indexOf('|');
        if (firstPipe == -1) {
            System.err.println("❌ Invalid message format");
            return;
        }

        String type = message.substring(0, firstPipe);
        String remaining = message.substring(firstPipe + 1);

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
        int pipe = remaining.indexOf('|');
        if (pipe == -1) {
            System.err.println("❌ Invalid handshake format");
            return;
        }
        String gameType = remaining.substring(0, pipe);
        long timestamp = Long.parseLong(remaining.substring(pipe + 1));
        System.out.println("   Handshake: gameType=" + gameType + ", timestamp=" + timestamp);
        onHandshakeReceived(gameType, timestamp);
    }

    private void handleMoveMessage(String remaining) {
        int pipe = remaining.indexOf('|');
        if (pipe == -1) {
            System.err.println("❌ Invalid move format");
            return;
        }

        String gameType = remaining.substring(0, pipe);
        String content = remaining.substring(pipe + 1);

        if (!gameActive || currentGame == null) {
            System.err.println("❌ No active game");
            return;
        }
        if (!gameType.equals(currentGameType)) {
            System.err.println("⚠️ Message for different game: " + gameType);
            return;
        }
        currentGame.onOpponentMove(content);
    }

    private void handleResultMessage(String remaining) {
        int firstPipe = remaining.indexOf('|');
        if (firstPipe == -1) {
            System.err.println("❌ Invalid result format");
            return;
        }
        String gameType = remaining.substring(0, firstPipe);
        String rest = remaining.substring(firstPipe + 1);

        int secondPipe = rest.indexOf('|');
        if (secondPipe == -1) {
            System.err.println("❌ Invalid result format (no second pipe)");
            return;
        }

        String result = rest.substring(0, secondPipe);
        String coords = rest.substring(secondPipe + 1);

        if (!gameActive || currentGame == null) {
            System.err.println("❌ No active game");
            return;
        }
        if (!gameType.equals(currentGameType)) {
            System.err.println("⚠️ Message for different game: " + gameType);
            return;
        }

        String resultData = result + "|" + coords;
        if (currentGame instanceof SeaBattleModule) {
            ((SeaBattleModule) currentGame).onShotResult(resultData);
        }
    }

    private void onHandshakeReceived(String gameType, long timestamp) {
        System.out.println("📨 [Controller] Handshake received: game=" + gameType + ", timestamp=" + timestamp);
        System.out.println("   currentGameType=" + currentGameType + ", handshakeSent=" + handshakeSent);

        opponentHandshakeTimestamp = timestamp;

        // Случай 1: нас приглашают (мы ещё не выбрали игру)
        if (currentGameType == null) {
            System.out.println("→ Showing game proposal");
            currentGameType = gameType;
            handshakeReceived = true;
            amIHost = false;
            showGameProposal(gameType);
            return;
        }

        // Случай 2: игра уже выбрана, проверяем совпадение типов
        if (!currentGameType.equals(gameType)) {
            System.out.println("❌ Game type mismatch");
            showError("Opponent chose a different game");
            resetState();
            return;
        }

        // Случай 3: типы совпадают
        handshakeReceived = true;

        // Если мы ещё не отправили свой handshake — отправляем ответ
        if (!handshakeSent) {
            System.out.println("→ Sending response handshake");
            sendHandshake();
        } // КОНФЛИКТ: оба отправили handshake одновременно
        else if (handshakeSent && !conflictResolved) {
            System.out.println("⚠️ Conflict detected – both sent handshake!");
            resolveConflictAndStart();
        }
    }

    // Разрешаем конфликт на основе времени первого handshake
    private void resolveConflictAndStart() {
        if (conflictResolved) {
            System.out.println("Conflict already resolved, skipping");
            return;
        }

        conflictResolved = true;

        // Сравниваем временные метки: у кого timestamp меньше (раньше отправил), тот и хост
        if (myHandshakeTimestamp < opponentHandshakeTimestamp) {
            System.out.println("→ I sent handshake FIRST (my timestamp=" + myHandshakeTimestamp
                    + " < opponent=" + opponentHandshakeTimestamp + ") → I am HOST");
            amIHost = true;
        } else if (opponentHandshakeTimestamp < myHandshakeTimestamp) {
            System.out.println("→ Opponent sent handshake FIRST (their timestamp=" + opponentHandshakeTimestamp
                    + " < my=" + myHandshakeTimestamp + ") → I am GUEST");
            amIHost = false;
        } else {
            // Теоретически маловероятно, но на случай одинаковых timestamp
            System.out.println("⚠️ Equal timestamps! Using default: I am HOST");
            amIHost = true;
        }

        System.out.println("→ Starting game with amIHost=" + amIHost);
        startGame();
    }

    private void showGameProposal(String gameType) {
        String displayName = getGameDisplayName(gameType);

        Stage proposalStage = new Stage();
        proposalStage.setTitle("Приглашение");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        themeHelper.applyToContainer(root);

        Text title = new Text("Приглашение на игру");
        themeHelper.styleText(title, true);

        Text info = new Text("Соперник хочет играть в " + displayName);
        themeHelper.styleText(info, false);

        Button acceptButton = new Button("Принять и играть");
        themeHelper.styleButton(acceptButton);
        acceptButton.setOnAction(e -> {
            sendHandshake();
            proposalStage.close();
        });

        Button rejectButton = new Button("Отказаться");
        rejectButton.setStyle(
                "-fx-font-size: 14px; -fx-padding: 10 20; "
                + "-fx-background-color: #ef4444; -fx-text-fill: white; "
                + "-fx-background-radius: 5; -fx-cursor: hand;"
        );
        rejectButton.setOnAction(e -> {
            proposalStage.close();
            resetState();
        });

        root.getChildren().addAll(title, info, acceptButton, rejectButton);
        Scene scene = new Scene(root, 350, 250);
        themeHelper.applyThemeToScene(scene);
        proposalStage.setScene(scene);
        proposalStage.show();
    }

    private void showError(String message) {
        Stage errorStage = new Stage();
        errorStage.setTitle("Ошибка");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        themeHelper.applyToContainer(root);

        Text title = new Text("Ошибка");
        title.setStyle(String.format(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #ef4444;"
        ));

        Text info = new Text(message);
        themeHelper.styleText(info, false);

        Button okButton = new Button("Понятно");
        themeHelper.styleButton(okButton);
        okButton.setOnAction(e -> errorStage.close());

        root.getChildren().addAll(title, info, okButton);
        Scene scene = new Scene(root, 400, 250);
        themeHelper.applyThemeToScene(scene);
        errorStage.setScene(scene);
        errorStage.show();
    }

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

        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                currentGame.startBattle();
            });
        });

        // Сбрасываем флаги для следующей игры
        handshakeSent = false;
        handshakeReceived = false;
        conflictResolved = false;
    }

    public void sendMove(String moveData) {
        if (!gameActive) {
            return;
        }

        String prefix;
        String content;

        if (moveData.contains("|")) {
            prefix = "result";
            content = moveData;
        } else {
            prefix = "move";
            content = moveData;
        }

        String message = prefix + "|" + currentGameType + "|" + content;
        GameMenuApp.getInstance().sendGameMessage(message);
    }

    public void startBattle() {
        if (currentGame != null) {
            currentGame.startBattle();
        }
    }

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
        conflictResolved = false;
        myHandshakeTimestamp = 0;
        opponentHandshakeTimestamp = 0;
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
        conflictResolved = false;
        myHandshakeTimestamp = 0;
        opponentHandshakeTimestamp = 0;
    }

    public boolean isGameActive() {
        return gameActive;
    }
}
