package com.mycompany.javaphone_nir2.games;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class TicTacToeModule extends FXGLGame {

    private Button[][] board = new Button[3][3];
    private Label statusLabel;
    private boolean gameOver = false;

    @Override
    public void showUI() {
        gameStage = new Stage();
        gameStage.setTitle("Крестики-нолики");

        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20; -fx-background-color: #34495e;");

        statusLabel = new Label(waitingForOpponent ? "Ожидание соперника..." : "Ваш ход!");
        statusLabel.setFont(Font.font(16));
        statusLabel.setTextFill(Color.WHITE);

        GridPane boardGrid = createBoard();

        Button backButton = new Button("Выйти в меню");
        backButton.setOnAction(e -> {
            gameStage.close();
            if (controller != null) {
                controller.closeGame();
            }
        });

        root.getChildren().addAll(statusLabel, boardGrid, backButton);

        Scene scene = new Scene(root, 400, 450);
        gameStage.setScene(scene);
    }

    private GridPane createBoard() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(5);
        grid.setVgap(5);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Button button = new Button(" ");
                button.setPrefSize(80, 80);
                button.setFont(Font.font(24));
                button.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #7f8c8d;");

                final int row = i;
                final int col = j;
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

        board[row][col].setText("X");
        board[row][col].setDisable(true);
        isMyTurn = false;
        statusLabel.setText("Ожидание хода соперника...");

        sendMove(row + "," + col);

        if (checkWin("X")) {
            gameOver = true;
            statusLabel.setText("🎉 ВЫ ПОБЕДИЛИ! 🎉");
            statusLabel.setTextFill(Color.GREEN);
        } else if (isBoardFull()) {
            gameOver = true;
            statusLabel.setText("🤝 НИЧЬЯ! 🤝");
            statusLabel.setTextFill(Color.ORANGE);
        }
    }

    @Override
    public void onOpponentMove(String moveData) {
        String[] coords = moveData.split(",");
        int row = Integer.parseInt(coords[0]);
        int col = Integer.parseInt(coords[1]);

        board[row][col].setText("O");
        board[row][col].setDisable(true);

        if (checkWin("O")) {
            gameOver = true;
            statusLabel.setText("😢 ВЫ ПРОИГРАЛИ! 😢");
            statusLabel.setTextFill(Color.RED);
        } else if (isBoardFull()) {
            gameOver = true;
            statusLabel.setText("🤝 НИЧЬЯ! 🤝");
            statusLabel.setTextFill(Color.ORANGE);
        } else {
            isMyTurn = true;
            statusLabel.setText("✅ Ваш ход!");
            statusLabel.setTextFill(Color.GREEN);
        }
    }

    @Override
    public void startBattle() {
        waitingForOpponent = false;
        isMyTurn = amIHost;  // ← Эта строка должна быть

        System.out.println("🔥 startBattle called: amIHost=" + amIHost + ", isMyTurn=" + isMyTurn);

        if (isMyTurn) {
            statusLabel.setText("✅ Ваш ход!");
            statusLabel.setTextFill(Color.GREEN);
        } else {
            statusLabel.setText("Ожидание хода соперника...");
            statusLabel.setTextFill(Color.ORANGE);
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
                return true;
            }
            if (board[0][i].getText().equals(symbol)
                    && board[1][i].getText().equals(symbol)
                    && board[2][i].getText().equals(symbol)) {
                return true;
            }
        }
        if (board[0][0].getText().equals(symbol)
                && board[1][1].getText().equals(symbol)
                && board[2][2].getText().equals(symbol)) {
            return true;
        }
        if (board[0][2].getText().equals(symbol)
                && board[1][1].getText().equals(symbol)
                && board[2][0].getText().equals(symbol)) {
            return true;
        }
        return false;
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
