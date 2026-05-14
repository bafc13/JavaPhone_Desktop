package com.mycompany.javaphone_nir2.games;

import com.almasb.fxgl.dsl.FXGL; 
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import com.almasb.fxgl.animation.Animation;
import com.almasb.fxgl.animation.AnimationBuilder;
import javafx.animation.FadeTransition;
import javafx.geometry.Point2D;
import javafx.util.Duration;

public class TicTacToeModule extends FXGLGame {

    private Button[][] board = new Button[3][3];
    private Label statusLabel;
    private boolean gameOver = false;
    private VBox root;

    @Override
    public void showUI() {
        gameStage = new Stage();
        gameStage.setTitle("Крестики-нолики");

        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        themeHelper.applyToContainer(root);

        statusLabel = new Label(waitingForOpponent ? "Ожидание соперника..." : "Ваш ход!");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        statusLabel.getStyleClass().add("game-status");

        GridPane boardGrid = createBoard();

        Button backButton = new Button("Выйти в меню");
        themeHelper.styleButton(backButton);
        backButton.setOnAction(e -> {
            gameStage.close();
            if (controller != null) {
                controller.closeGame();
            }
        });

        root.getChildren().addAll(statusLabel, boardGrid, backButton);

        Scene scene = new Scene(root, 450, 550);
        themeHelper.applyThemeToScene(scene);

        gameStage.setScene(scene);
    }

    private GridPane createBoard() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(12);
        grid.setVgap(12);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Button button = new Button(" ");
                button.setPrefSize(100, 100);
                button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));

                // Начальный стиль пустой клетки
                themeHelper.styleTicTacToeCell(button, " ", false);

                final int row = i;
                final int col = j;

                // Hover эффект для пустых клеток
                button.setOnMouseEntered(e -> {
                    if (button.getText().equals(" ") && !gameOver && isMyTurn && !waitingForOpponent) {
                        button.getStyleClass().add("tic-tac-toe-cell-empty-hover");
                    }
                });

                button.setOnMouseExited(e -> {
                    if (button.getText().equals(" ")) {
                        button.getStyleClass().remove("tic-tac-toe-cell-empty-hover");
                    }
                });

                button.setOnAction(e -> makeMove(row, col));

                board[row][col] = button;
                grid.add(button, j, i);
            }
        }
        return grid;
    }

    private void makeMove(int row, int col) {
        if (gameOver || !isMyTurn || waitingForOpponent) {
            return;
        }
        if (!board[row][col].getText().equals(" ")) {
            return;
        }

        // Устанавливаем X
        animateSymbol(board[row][col], "X"); 
        board[row][col].setText("X");
        FXGL.play("com/mycompany/javaphone_nir2/games/sounds/pencil1.wav");
        themeHelper.styleTicTacToeCell(board[row][col], "X", false);
        board[row][col].setDisable(true);

        isMyTurn = false;
        statusLabel.setText("Ожидание хода соперника...");

        sendMove(row + "," + col);

        if (checkWin("X")) {
            gameOver = true;
            FXGL.play("com/mycompany/javaphone_nir2/games/sounds/win.wav");
            statusLabel.setText("🎉 ВЫ ПОБЕДИЛИ! 🎉");
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
        } else if (isBoardFull()) {
            gameOver = true;
            statusLabel.setText("🤝 НИЧЬЯ! 🤝");
            statusLabel.setStyle("-fx-text-fill: #f39c12;");
        }
    }

    @Override
    public void onOpponentMove(String moveData) {
        String[] coords = moveData.split(",");
        int row = Integer.parseInt(coords[0]);
        int col = Integer.parseInt(coords[1]);

        // Устанавливаем O
        animateSymbol(board[row][col], "O");
        board[row][col].setText("O");
        FXGL.play("com/mycompany/javaphone_nir2/games/sounds/pencil1.wav");
        themeHelper.styleTicTacToeCell(board[row][col], "O", false);
        board[row][col].setDisable(true);

        if (checkWin("O")) {
            gameOver = true;
            FXGL.play("com/mycompany/javaphone_nir2/games/sounds/lose.wav");
            statusLabel.setText("😢 ВЫ ПРОИГРАЛИ! 😢");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
        } else if (isBoardFull()) {
            gameOver = true;
            statusLabel.setText("🤝 НИЧЬЯ! 🤝");
            statusLabel.setStyle("-fx-text-fill: #f39c12;");
        } else {
            isMyTurn = true;
            statusLabel.setText("✅ Ваш ход!");
            statusLabel.setStyle(null);
            statusLabel.getStyleClass().add("game-status");
        }
    }
    
private void animateSymbol(Button button, String symbol) {
    button.setText(symbol);
    
    // Начинаем с прозрачного
    button.setOpacity(0);
    
    // Плавно появляемся
    javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
        javafx.util.Duration.millis(200), button
    );
    fade.setFromValue(0);
    fade.setToValue(1);
    fade.play();
    
    // Добавляем лёгкое увеличение для эффекта "выскакивания"
    button.setScaleX(0.8);
    button.setScaleY(0.8);
    
    javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(
        javafx.util.Duration.millis(200), button
    );
    scale.setFromX(0.8);
    scale.setFromY(0.8);
    scale.setToX(1);
    scale.setToY(1);
    scale.play();
}

    @Override
    public void startBattle() {
        waitingForOpponent = false;
        isMyTurn = amIHost;

        System.out.println("🔥 startBattle called: amIHost=" + amIHost + ", isMyTurn=" + isMyTurn);

        if (isMyTurn) {
            statusLabel.setText("✅ Ваш ход!");
        } else {
            statusLabel.setText("Ожидание хода соперника...");
        }
    }

    @Override
    public void cleanup() {
        board = null;
    }

    private boolean checkWin(String symbol) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0].getText().equals(symbol)
                    && board[i][1].getText().equals(symbol)
                    && board[i][2].getText().equals(symbol)) {
                highlightWinLine(i, 0, i, 1, i, 2);
                return true;
            }
            if (board[0][i].getText().equals(symbol)
                    && board[1][i].getText().equals(symbol)
                    && board[2][i].getText().equals(symbol)) {
                highlightWinLine(0, i, 1, i, 2, i);
                return true;
            }
        }

        if (board[0][0].getText().equals(symbol)
                && board[1][1].getText().equals(symbol)
                && board[2][2].getText().equals(symbol)) {
            highlightWinLine(0, 0, 1, 1, 2, 2);
            return true;
        }

        if (board[0][2].getText().equals(symbol)
                && board[1][1].getText().equals(symbol)
                && board[2][0].getText().equals(symbol)) {
            highlightWinLine(0, 2, 1, 1, 2, 0);
            return true;
        }

        return false;
    }

    private void highlightWinLine(int... cells) {
        for (int i = 0; i < cells.length; i += 2) {
            int row = cells[i];
            int col = cells[i + 1];
            String symbol = board[row][col].getText();
            themeHelper.styleTicTacToeCell(board[row][col], symbol, true);
        }
    }

    private boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].getText().equals(" ")) {
                    return false;
                }
            }
        }
        return true;
    }
}
