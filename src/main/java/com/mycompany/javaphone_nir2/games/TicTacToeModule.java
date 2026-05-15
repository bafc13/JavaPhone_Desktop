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
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
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
    if (gameOver || !isMyTurn || waitingForOpponent) return;
    if (!board[row][col].getText().equals(" ")) return;
    
    Button cell = board[row][col];
    
    // СНАЧАЛА меняем состояние
    cell.setText("X");
    themeHelper.styleTicTacToeCell(cell, "X", false);
    cell.setDisable(true);
    
    isMyTurn = false;
    statusLabel.setText("Ожидание хода соперника...");
    
    // ОТПРАВЛЯЕМ ход
    sendMove(row + "," + col);
    
    // ПОТОМ анимация (через Platform.runLater)
    Platform.runLater(() -> animateSymbol(cell, "X"));
    
    if (checkWin("X")) {
        gameOver = true;
        statusLabel.setText("🎉 ВЫ ПОБЕДИЛИ! 🎉");
    } else if (isBoardFull()) {
        gameOver = true;
        statusLabel.setText("🤝 НИЧЬЯ! 🤝");
    }
}

    
@Override
public void onOpponentMove(String moveData) {
    String[] coords = moveData.split(",");
    int row = Integer.parseInt(coords[0]);
    int col = Integer.parseInt(coords[1]);
    
    Button cell = board[row][col];
    
    // СНАЧАЛА меняем состояние
    cell.setText("O");
    themeHelper.styleTicTacToeCell(cell, "O", false);
    cell.setDisable(true);
    
    // ПОТОМ анимация
    Platform.runLater(() -> animateSymbol(cell, "O"));
    
    if (checkWin("O")) {
        gameOver = true;
        statusLabel.setText("😢 ВЫ ПРОИГРАЛИ! 😢");
    } else if (isBoardFull()) {
        gameOver = true;
        statusLabel.setText("🤝 НИЧЬЯ! 🤝");
    } else {
        isMyTurn = true;
        statusLabel.setText("✅ Ваш ход!");
    }
}

    
private void animateSymbol(Button button, String symbol) {
    button.setText(symbol);
    
    // Начальное состояние (маленький и прозрачный)
    button.setOpacity(0);
    button.setScaleX(0.5);
    button.setScaleY(0.5);
    
    // Анимация появления с увеличением
    FadeTransition fade = new FadeTransition(Duration.millis(150), button);
    fade.setFromValue(0);
    fade.setToValue(1);
    
    ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
    scale.setFromX(0.5);
    scale.setFromY(0.5);
    scale.setToX(1);
    scale.setToY(1);
    
    ParallelTransition parallel = new ParallelTransition(fade, scale);
    parallel.play();
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
