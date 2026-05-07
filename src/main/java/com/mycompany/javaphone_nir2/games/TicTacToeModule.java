package com.mycompany.javaphone_nir2.games;

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

public class TicTacToeModule extends FXGLGame {
    
    private Button[][] board = new Button[3][3];
    private Label statusLabel;
    private boolean gameOver = false;
    private VBox root;
    private GridPane boardGrid;
    
    @Override
    public void showUI() {
        gameStage = new Stage();
        gameStage.setTitle("Крестики-нолики");
        
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        themeHelper.applyToContainer(root);
        
        statusLabel = new Label(waitingForOpponent ? "Ожидание соперника..." : "Ваш ход!");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        themeHelper.styleLabel(statusLabel, false);
        
        boardGrid = createBoard();
        
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
        
        // Подписываемся на изменения темы
        com.mycompany.javaphone_nir2.models.SettingsManager.getInstance().themeProperty().addListener((obs, oldTheme, newTheme) -> {
            Platform.runLater(() -> updateTheme());
        });
        
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
                
                // Применяем стиль клетки в зависимости от темы
                updateCellStyle(button, " ", false);
                
                // Эффект при наведении
                final int row = i;
                final int col = j;
                button.setOnMouseEntered(e -> {
                    if (button.getText().equals(" ") && !gameOver && isMyTurn && !waitingForOpponent) {
                        String hoverColor = themeHelper.isDarkTheme() ? "#4a4a5a" : "#3498db";
                        button.setStyle(
                            String.format("-fx-background-color: %s; " +
                            "-fx-text-fill: %s; " +
                            "-fx-font-size: 48px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-border-radius: 12; " +
                            "-fx-background-radius: 12; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0.5, 0, 2);",
                            hoverColor, themeHelper.getTextColor())
                        );
                    }
                });
                
                button.setOnMouseExited(e -> {
                    if (button.getText().equals(" ")) {
                        updateCellStyle(button, " ", false);
                    }
                });
                
                button.setOnAction(e -> makeMove(row, col));
                
                board[row][col] = button;
                grid.add(button, j, i);
            }
        }
        return grid;
    }
    
    private void updateCellStyle(Button button, String symbol, boolean isWinning) {
        String bgColor;
        String textColor;
        String borderColor = themeHelper.getGridColor();
        int borderWidth = isWinning ? 3 : 2;
        
        if (isWinning) {
            bgColor = themeHelper.isDarkTheme() ? "#f1c40f" : "#f39c12";
            textColor = "#ffffff";
            borderColor = "#f1c40f";
        } else if (symbol.equals("X")) {
            bgColor = themeHelper.isDarkTheme() ? "#4ecdc4" : "#3498db";
            textColor = "#ffffff";
        } else if (symbol.equals("O")) {
            bgColor = themeHelper.isDarkTheme() ? "#ffe66d" : "#e74c3c";
            textColor = "#ffffff";
        } else {
            // Пустая клетка
            bgColor = themeHelper.getBoardColor();
            textColor = themeHelper.getTextColor();
        }
        
        String effect = isWinning ? 
            "-fx-effect: dropshadow(gaussian, #f1c40f, 20, 0.8, 0, 0);" : 
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0.5, 0, 2);";
        
        button.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-font-size: 48px; " +
            "-fx-font-weight: bold; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: %d; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: %s; " +
            "%s",
            bgColor, textColor, borderColor, borderWidth,
            symbol.equals(" ") ? "hand" : "default",
            effect
        ));
    }
    
    private void updateTheme() {
        // Обновляем фон контейнера
        themeHelper.applyToContainer(root);
        
        // Обновляем стиль статуса
        if (!gameOver) {
            if (waitingForOpponent) {
                statusLabel.setText("Ожидание соперника...");
            } else if (isMyTurn) {
                statusLabel.setText("✅ Ваш ход!");
            } else {
                statusLabel.setText("Ожидание хода соперника...");
            }
            themeHelper.styleLabel(statusLabel, false);
        }
        
        // Обновляем стиль всех клеток
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Button btn = board[i][j];
                if (btn != null) {
                    String symbol = btn.getText();
                    if (!symbol.equals(" ")) {
                        // Перерисовываем X и O с новыми цветами
                        if (symbol.equals("X")) {
                            updateCellStyle(btn, "X", false);
                        } else if (symbol.equals("O")) {
                            updateCellStyle(btn, "O", false);
                        }
                    } else {
                        updateCellStyle(btn, " ", false);
                    }
                }
            }
        }
    }
    
    private void makeMove(int row, int col) {
        if (gameOver || !isMyTurn || waitingForOpponent) return;
        if (!board[row][col].getText().equals(" ")) return;
        
        // Устанавливаем X
        board[row][col].setText("X");
        updateCellStyle(board[row][col], "X", false);
        board[row][col].setDisable(true);
        
        isMyTurn = false;
        statusLabel.setText("Ожидание хода соперника...");
        themeHelper.styleLabel(statusLabel, false);
        
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
        
        // Устанавливаем O
        board[row][col].setText("O");
        updateCellStyle(board[row][col], "O", false);
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
            themeHelper.styleLabel(statusLabel, false);
        }
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
        themeHelper.styleLabel(statusLabel, false);
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
            updateCellStyle(board[row][col], board[row][col].getText(), true);
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