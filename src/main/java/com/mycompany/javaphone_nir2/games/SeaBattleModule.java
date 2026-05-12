package com.mycompany.javaphone_nir2.games;

import com.mycompany.javaphone_nir2.games.sea_battle.SeaBattleBoard;
import com.mycompany.javaphone_nir2.games.sea_battle.SeaBattleCell;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class SeaBattleModule extends FXGLGame {
    private Label statusLabel;
    private Button[][] myBoardButtons = new Button[10][10];
    private Button[][] enemyBoardButtons = new Button[10][10];
    
    private SeaBattleBoard myBoard;
    private SeaBattleBoard enemyBoard;
    
    private int destroyedShipsCount = 0;
    private boolean gameOver = false;
    
    private ThemeApplier themeApplier = new ThemeApplier();  // ← добавляем
    
     @Override
    public void showUI() {
        gameStage = new Stage();
        gameStage.setTitle("Морской бой");
        
        myBoard = new SeaBattleBoard();
        enemyBoard = new SeaBattleBoard();
        myBoard.randomizeShips();
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #dadef7; -fx-padding: 20;");
        
        statusLabel = new Label(waitingForOpponent ? "Ожидание соперника..." : "Ваш ход!");
        statusLabel.setFont(Font.font(16));
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setAlignment(Pos.CENTER);
        root.setTop(statusLabel);
        
        HBox boards = new HBox(30);
        boards.setAlignment(Pos.CENTER);
        
        VBox myBoardBox = createBoard("⚓ МОИ КОРАБЛИ", true);
        VBox enemyBoardBox = createBoard("🎯 ОБСТРЕЛ ПРОТИВНИКА", false);
        
        boards.getChildren().addAll(myBoardBox, enemyBoardBox);
        root.setCenter(boards);
        
        Button backButton = new Button("Выйти в меню");
        backButton.setOnAction(e -> {
            gameStage.close();
            if (controller != null) {
                controller.closeGame();
            }
        });
        
        VBox bottom = new VBox(10);
        bottom.setAlignment(Pos.CENTER);
        bottom.getChildren().add(backButton);
        root.setBottom(bottom);
        
        Scene scene = new Scene(root, 1100, 750);
        
        // ПРИМЕНЯЕМ ТЕМУ
        themeApplier.applyThemeToScene(scene);
        
        gameStage.setScene(scene);
        
        updateMyBoard();
        updateEnemyBoard();
    }
    
    private VBox createBoard(String title, boolean isMyBoard) {
        Label boardLabel = new Label(title);
        boardLabel.setStyle(String.format(
            "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s;",
            ThemeHelper.getTextColorHex()
        ));
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(3);
        grid.setVgap(3);
        
        // Letters (А-К)
        for (int j = 0; j < 10; j++) {
            Label letter = new Label(String.valueOf((char) ('А' + j)));
            letter.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold;",
                ThemeHelper.getTextColorHex()
            ));
            letter.setAlignment(Pos.CENTER);
            grid.add(letter, j + 1, 0);
        }
        
        // Numbers (1-10)
        for (int i = 0; i < 10; i++) {
            Label number = new Label(String.valueOf(i + 1));
            number.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold;",
                ThemeHelper.getTextColorHex()
            ));
            number.setAlignment(Pos.CENTER);
            grid.add(number, 0, i + 1);
        }
        
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button cell = new Button(" ");
                cell.setPrefSize(38, 38);
                cell.setFont(Font.font(14));
                
                if (isMyBoard) {
                    // Своё поле - вода и корабли
                    cell.setStyle(
                        "-fx-background-color: #2980b9; " +
                        "-fx-border-color: #1a5276; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 4; " +
                        "-fx-background-radius: 4;"
                    );
                    cell.setDisable(true);
                    myBoardButtons[i][j] = cell;
                } else {
                    // Поле противника - вода с градиентом
                    cell.setStyle(
                        "-fx-background-color: #2980b9; " +
                        "-fx-border-color: #1a5276; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 4; " +
                        "-fx-background-radius: 4; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0.5, 0, 1);"
                    );
                    
                    // Эффект при наведении
                    final Button finalCell = cell;
                    cell.setOnMouseEntered(e -> {
                        if (!gameOver && isMyTurn && !waitingForOpponent && finalCell.isDisable() == false) {
                            finalCell.setStyle(
                                "-fx-background-color: #3498db; " +
                                "-fx-border-color: #1a5276; " +
                                "-fx-border-width: 2; " +
                                "-fx-border-radius: 4; " +
                                "-fx-background-radius: 4; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(52,152,219,0.5), 6, 0.5, 0, 2);"
                            );
                        }
                    });
                    
                    cell.setOnMouseExited(e -> {
                        if (!gameOver && isMyTurn && !waitingForOpponent && finalCell.isDisable() == false) {
                            finalCell.setStyle(
                                "-fx-background-color: #2980b9; " +
                                "-fx-border-color: #1a5276; " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 4; " +
                                "-fx-background-radius: 4; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0.5, 0, 1);"
                            );
                        }
                    });
                    
                    final int row = i;
                    final int col = j;
                    cell.setOnAction(e -> makeShoot(row, col));
                    enemyBoardButtons[i][j] = cell;
                }
                
                grid.add(cell, j + 1, i + 1);
            }
        }
        
        VBox container = new VBox(10, boardLabel, grid);
        container.setAlignment(Pos.CENTER);
        return container;
    }
    
    private void updateMyBoard() {
        int[][] fullBoard = myBoard.getFullBoard();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button cell = myBoardButtons[i][j];
                if (cell == null) continue;
                
                int value = fullBoard[i][j];
                if (value == SeaBattleCell.SHIP) {
                    cell.setText("[]");
                    cell.setStyle(
                        "-fx-background-color: #34495e; " +
                        "-fx-text-fill: #ecf0f1; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-border-color: #1a5276; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 4; " +
                        "-fx-background-radius: 4;"
                    );
                } else if (value == SeaBattleCell.HIT) {
                    cell.setText("X");
                    cell.setStyle(
                        "-fx-background-color: #e74c3c; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 16px; " +
                        "-fx-border-color: #c0392b; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 4; " +
                        "-fx-background-radius: 4;"
                    );
                } else if (value == SeaBattleCell.MISS) {
                    cell.setText("•");
                    cell.setStyle(
                        "-fx-background-color: #95a5a6; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-border-color: #7f8c8d; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 4; " +
                        "-fx-background-radius: 4;"
                    );
                } else {
                    cell.setText(" ");
                    cell.setStyle(
                        "-fx-background-color: #2980b9; " +
                        "-fx-border-color: #1a5276; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 4; " +
                        "-fx-background-radius: 4;"
                    );
                }
            }
        }
    }
    
    private void updateEnemyBoard() {
        System.out.println("🔄 updateEnemyBoard() called");
        int[][] visibleBoard = enemyBoard.getVisibleBoard();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button cell = enemyBoardButtons[i][j];
                if (cell == null) continue;
                
                int value = visibleBoard[i][j];
                if (value == SeaBattleCell.HIT) {
                    if (!cell.getText().equals("X")) {
                        System.out.println("   Updating [" + i + "," + j + "] to HIT");
                        cell.setText("X");
                        cell.setStyle(
                            "-fx-background-color: #e74c3c; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 16px; " +
                            "-fx-border-color: #c0392b; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 4; " +
                            "-fx-background-radius: 4;"
                        );
                        cell.setDisable(true);
                    }
                } else if (value == SeaBattleCell.MISS) {
                    if (!cell.getText().equals("•")) {
                        System.out.println("   Updating [" + i + "," + j + "] to MISS");
                        cell.setText("•");
                        cell.setStyle(
                            "-fx-background-color: #7f8c8d; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 20px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-border-color: #6c7a7d; " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 4; " +
                            "-fx-background-radius: 4;"
                        );
                        cell.setDisable(true);
                    }
                } else {
                    cell.setText(" ");
                    cell.setStyle(
                        "-fx-background-color: #2980b9; " +
                        "-fx-border-color: #1a5276; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 4; " +
                        "-fx-background-radius: 4;"
                    );
                    if (!waitingForOpponent && !gameOver && isMyTurn) {
                        cell.setDisable(false);
                    } else {
                        cell.setDisable(true);
                    }
                }
            }
        }
    }
    
    private void makeShoot(int row, int col) {
        if (gameOver || !isMyTurn || waitingForOpponent) return;
        
        if (enemyBoard.getCell(row, col).isHit() || enemyBoard.getCell(row, col).isMiss()) {
            statusLabel.setText("❌ Вы уже стреляли в эту клетку!");
            statusLabel.setTextFill(Color.RED);
            return;
        }
        
        isMyTurn = false;
        statusLabel.setText("Выстрел отправлен...");
        statusLabel.setTextFill(Color.ORANGE);
        
        sendMove(row + "," + col);
    }
    
    @Override
    public void onOpponentMove(String moveData) {
        String[] coords = moveData.split(",");
        int row = Integer.parseInt(coords[0]);
        int col = Integer.parseInt(coords[1]);
        
        int result = myBoard.receiveShot(row, col);
        
        String resultType;
        String message;
        
        switch (result) {
            case 0:
                resultType = "miss";
                message = "💨 Противник промахнулся!";
                statusLabel.setTextFill(Color.ORANGE);
                break;
            case 1:
                resultType = "hit";
                message = "💥 Противник попал!";
                statusLabel.setTextFill(Color.RED);
                break;
            case 2:
                resultType = "kill";
                message = "💀 Противник уничтожил ваш корабль! Осталось кораблей: " + myBoard.getShipsAlive();
                statusLabel.setTextFill(Color.RED);
                break;
            default:
                resultType = "error";
                message = "❌ Ошибка!";
                statusLabel.setTextFill(Color.RED);
        }
        
        statusLabel.setText(message);
        updateMyBoard();
        
        sendMove(resultType + "|" + row + "," + col);
        
        if (myBoard.allShipsDestroyed()) {
            gameOver = true;
            statusLabel.setText("😢 ВЫ ПРОИГРАЛИ! Все ваши корабли уничтожены!");
            statusLabel.setTextFill(Color.RED);
            disableBoards();
            return;
        }
        
        isMyTurn = true;
        statusLabel.setText("✅ Ваш ход!");
        statusLabel.setTextFill(Color.GREEN);
        updateEnemyBoard();
    }
    
    public void onShotResult(String resultData) {
        System.out.println("🔥 SeaBattleModule.onShotResult: " + resultData);
        
        String[] parts = resultData.split("\\|");
        if (parts.length < 2) {
            System.err.println("❌ Invalid shot result format: " + resultData);
            return;
        }
        
        String result = parts[0];
        String[] coords = parts[1].split(",");
        
        int row = Integer.parseInt(coords[0]);
        int col = Integer.parseInt(coords[1]);
        
        System.out.println("   Processing: row=" + row + ", col=" + col + ", result=" + result);
        
        if ("hit".equals(result) || "kill".equals(result)) {
            enemyBoard.getCell(row, col).markAsHit();
            updateEnemyBoard();
            
            if ("kill".equals(result)) {
                List<int[]> shipCells = findConnectedShipCells(row, col);
                System.out.println("   Found " + shipCells.size() + " ship cells");
                
                for (int[] cell : shipCells) {
                    int r = cell[0];
                    int c = cell[1];
                    
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            int nr = r + dr;
                            int nc = c + dc;
                            
                            if (nr >= 0 && nr < 10 && nc >= 0 && nc < 10) {
                                SeaBattleCell surrounding = enemyBoard.getCell(nr, nc);
                                if (surrounding.isEmpty()) {
                                    surrounding.markAsMiss();
                                    System.out.println("   Marking [" + nr + "," + nc + "] as MISS");
                                }
                            }
                        }
                    }
                }
                
                updateEnemyBoard();
                destroyedShipsCount++;
                statusLabel.setText("💀 Корабль уничтожен! Осталось кораблей: " + (10 - destroyedShipsCount));
                statusLabel.setTextFill(Color.GREEN);
                
                if (destroyedShipsCount >= 10) {
                    gameOver = true;
                    statusLabel.setText("🏆 ПОБЕДА! Все корабли противника уничтожены!");
                    statusLabel.setTextFill(Color.GREEN);
                    disableBoards();
                    return;
                }
            } else {
                statusLabel.setText("💥 Попадание!");
                statusLabel.setTextFill(Color.GREEN);
            }
            
            if (destroyedShipsCount >= 10) {
                gameOver = true;
                statusLabel.setText("🏆 ПОБЕДА! Все корабли противника уничтожены!");
                statusLabel.setTextFill(Color.GREEN);
                disableBoards();
                return;
            }
        } else if ("miss".equals(result)) {
            enemyBoard.getCell(row, col).markAsMiss();
            updateEnemyBoard();
            statusLabel.setText("💨 Промах!");
            statusLabel.setTextFill(Color.ORANGE);
        }
        
        isMyTurn = false;
        statusLabel.setText("Ожидание хода соперника...");
        statusLabel.setTextFill(Color.ORANGE);
        updateEnemyBoard();
    }
    
    private List<int[]> findConnectedShipCells(int startRow, int startCol) {
        List<int[]> shipCells = new ArrayList<>();
        boolean[][] visited = new boolean[10][10];
        
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int r = current[0];
            int c = current[1];
            
            shipCells.add(new int[]{r, c});
            
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                
                if (nr >= 0 && nr < 10 && nc >= 0 && nc < 10 && !visited[nr][nc]) {
                    SeaBattleCell cell = enemyBoard.getCell(nr, nc);
                    if (cell.isHit()) {
                        visited[nr][nc] = true;
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
        
        return shipCells;
    }
    
    private void disableBoards() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (enemyBoardButtons[i][j] != null) {
                    enemyBoardButtons[i][j].setDisable(true);
                }
            }
        }
    }
    
    @Override
    public void startBattle() {
        waitingForOpponent = false;
        isMyTurn = amIHost;
        
        if (isMyTurn) {
            statusLabel.setText("✅ Ваш ход! Стреляйте по полю противника!");
            statusLabel.setTextFill(Color.GREEN);
        } else {
            statusLabel.setText("Ожидание хода соперника...");
            statusLabel.setTextFill(Color.ORANGE);
        }
        
        updateEnemyBoard();
    }
    
    @Override
    public void cleanup() {
        myBoard = null;
        enemyBoard = null;
        myBoardButtons = null;
        enemyBoardButtons = null;
    }
}