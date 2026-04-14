package com.mycompany.javaphone_nir2.games;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class TicTacToeModule extends FXGLGame {
    
    private Button[][] board = new Button[3][3];
    private Label statusLabel;
    private boolean gameOver = false;
    private ThemeApplier themeApplier = new ThemeApplier();
    @Override
    public void showUI() {
        gameStage = new Stage();
        gameStage.setTitle("Крестики-нолики");
        
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 30; -fx-background-color: #34495e;");
        
        statusLabel = new Label(waitingForOpponent ? "Ожидание соперника..." : "Ваш ход!");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.WHITE);
        
        GridPane boardGrid = createBoard();
        
        Button backButton = new Button("Выйти в меню");
        backButton.setStyle(
            "-fx-font-size: 14px; -fx-padding: 8 20; " +
            "-fx-background-color: #1a225d; -fx-text-fill: white; " +
            "-fx-background-radius: 20; -fx-cursor: hand;"
        );
        backButton.setOnAction(e -> {
            gameStage.close();
            if (controller != null) {
                controller.closeGame();
            }
        });
        
        root.getChildren().addAll(statusLabel, boardGrid, backButton);
        
        Scene scene = new Scene(root, 450, 550);
        
        // ПРИМЕНЯЕМ ТЕМУ
        themeApplier.applyThemeToScene(scene);
        
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
                
                // Современный дизайн клетки
                button.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.9); " +
                    "-fx-border-color: #bdc3c7; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 12; " +
                    "-fx-background-radius: 12; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0.5, 0, 2);"
                );
                
                // Эффект при наведении
                button.setOnMouseEntered(e -> {
                    if (button.getText().equals(" ") && !gameOver && isMyTurn && !waitingForOpponent) {
                        button.setStyle(
                            "-fx-background-color: rgba(255, 255, 255, 1); " +
                            "-fx-border-color: #3498db; " +
                            "-fx-border-width: 3; " +
                            "-fx-border-radius: 12; " +
                            "-fx-background-radius: 12; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(52,152,219,0.3), 8, 0.5, 0, 2);"
                        );
                    }
                });
                
                button.setOnMouseExited(e -> {
                    if (button.getText().equals(" ")) {
                        button.setStyle(
                            "-fx-background-color: rgba(255, 255, 255, 0.9); " +
                            "-fx-border-color: #bdc3c7; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 12; " +
                            "-fx-background-radius: 12; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0.5, 0, 2);"
                        );
                    }
                });
                
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
        if (gameOver || !isMyTurn || waitingForOpponent) return;
        if (!board[row][col].getText().equals(" ")) return;
        
        // Устанавливаем X с красивым стилем
        board[row][col].setText("X");
        board[row][col].setStyle(
            "-fx-background-color: #3498db; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 48px; " +
            "-fx-font-weight: bold; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: default;"
        );
        board[row][col].setDisable(true);
        
        isMyTurn = false;
        statusLabel.setText("Ожидание хода соперника...");
        statusLabel.setStyle(String.format(
            "-fx-text-fill: #f39c12; -fx-font-size: 18px; -fx-font-weight: bold;",
            ThemeHelper.getTextColorHex()
        ));
        
        sendMove(row + "," + col);
        
        if (checkWin("X")) {
            gameOver = true;
            statusLabel.setText("🎉 ВЫ ПОБЕДИЛИ! 🎉");
            statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else if (isBoardFull()) {
            gameOver = true;
            statusLabel.setText("🤝 НИЧЬЯ! 🤝");
            statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 20px; -fx-font-weight: bold;");
        }
    }
    
    @Override
    public void onOpponentMove(String moveData) {
        String[] coords = moveData.split(",");
        int row = Integer.parseInt(coords[0]);
        int col = Integer.parseInt(coords[1]);
        
        // Устанавливаем O с красивым стилем
        board[row][col].setText("O");
        board[row][col].setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 48px; " +
            "-fx-font-weight: bold; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: default;"
        );
        board[row][col].setDisable(true);
        
        if (checkWin("O")) {
            gameOver = true;
            statusLabel.setText("😢 ВЫ ПРОИГРАЛИ! 😢");
            statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else if (isBoardFull()) {
            gameOver = true;
            statusLabel.setText("🤝 НИЧЬЯ! 🤝");
            statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else {
            isMyTurn = true;
            statusLabel.setText("✅ Ваш ход!");
            statusLabel.setStyle(String.format(
                "-fx-text-fill: #27ae60; -fx-font-size: 18px; -fx-font-weight: bold;"
            ));
        }
    }
    
    @Override
    public void startBattle() {
        waitingForOpponent = false;
        isMyTurn = amIHost;
        
        System.out.println("🔥 startBattle called: amIHost=" + amIHost + ", isMyTurn=" + isMyTurn);
        
        if (isMyTurn) {
            statusLabel.setText("✅ Ваш ход!");
            statusLabel.setStyle(String.format(
                "-fx-text-fill: #27ae60; -fx-font-size: 18px; -fx-font-weight: bold;"
            ));
        } else {
            statusLabel.setText("Ожидание хода соперника...");
            statusLabel.setStyle(String.format(
                "-fx-text-fill: #f39c12; -fx-font-size: 18px; -fx-font-weight: bold;"
            ));
        }
    }
    
    @Override
    public void cleanup() {
        board = null;
    }
    
    private boolean checkWin(String symbol) {
        // Проверка по строкам и столбцам
        for (int i = 0; i < 3; i++) {
            if (board[i][0].getText().equals(symbol) &&
                board[i][1].getText().equals(symbol) &&
                board[i][2].getText().equals(symbol)) {
                highlightWinLine(i, 0, i, 1, i, 2);
                return true;
            }
            if (board[0][i].getText().equals(symbol) &&
                board[1][i].getText().equals(symbol) &&
                board[2][i].getText().equals(symbol)) {
                highlightWinLine(0, i, 1, i, 2, i);
                return true;
            }
        }
        
        // Проверка диагоналей
        if (board[0][0].getText().equals(symbol) &&
            board[1][1].getText().equals(symbol) &&
            board[2][2].getText().equals(symbol)) {
            highlightWinLine(0, 0, 1, 1, 2, 2);
            return true;
        }
        
        if (board[0][2].getText().equals(symbol) &&
            board[1][1].getText().equals(symbol) &&
            board[2][0].getText().equals(symbol)) {
            highlightWinLine(0, 2, 1, 1, 2, 0);
            return true;
        }
        
        return false;
    }
    
    private void highlightWinLine(int... cells) {
        for (int i = 0; i < cells.length; i += 2) {
            int row = cells[i];
            int col = cells[i+1];
            board[row][col].setStyle(
                board[row][col].getStyle() + 
                "-fx-effect: dropshadow(gaussian, #f1c40f, 20, 0.8, 0, 0); " +
                "-fx-border-color: #f1c40f; " +
                "-fx-border-width: 3;"
            );
        }
    }
    
    private boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].getText().equals(" ")) return false;
            }
        }
        return true;
    }
}