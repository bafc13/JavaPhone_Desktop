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
import javafx.stage.Stage;

public class SeaBattleModule extends FXGLGame {

    private Label statusLabel;
    private Button[][] myBoardButtons = new Button[10][10];
    private Button[][] enemyBoardButtons = new Button[10][10];

    private SeaBattleBoard myBoard;      // Player's own board
    private SeaBattleBoard enemyBoard;   // Enemy board (shots fired)

    private int destroyedShipsCount = 0;  // Ships destroyed by player
    private boolean gameOver = false;

    @Override
    public void showUI() {
        gameStage = new Stage();
        gameStage.setTitle("Морской бой");

        // Initialize boards
        myBoard = new SeaBattleBoard();
        enemyBoard = new SeaBattleBoard();

        // Randomly place player's ships
        myBoard.randomizeShips();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #34495e; -fx-padding: 20;");

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

        Scene scene = new Scene(root, 1000, 700);
        gameStage.setScene(scene);

        updateMyBoard();
        updateEnemyBoard();
    }

    private VBox createBoard(String title, boolean isMyBoard) {
        Label boardLabel = new Label(title);
        boardLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(2);
        grid.setVgap(2);

        // Letters (А-К)
        for (int j = 0; j < 10; j++) {
            Label letter = new Label(String.valueOf((char) ('А' + j)));
            letter.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 10px;");
            grid.add(letter, j + 1, 0);
        }

        // Numbers (1-10)
        for (int i = 0; i < 10; i++) {
            Label number = new Label(String.valueOf(i + 1));
            number.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 10px;");
            grid.add(number, 0, i + 1);
        }

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button cell = new Button(" ");
                cell.setPrefSize(35, 35);

                if (isMyBoard) {
                    cell.setStyle("-fx-background-color: #3498db; -fx-border-color: #2c3e50;");
                    cell.setDisable(true);
                    myBoardButtons[i][j] = cell;
                } else {
                    cell.setStyle("-fx-background-color: #e67e22; -fx-border-color: #2c3e50;");
                    final int row = i;
                    final int col = j;
                    cell.setOnAction(e -> makeShoot(row, col));
                    enemyBoardButtons[i][j] = cell;
                }

                grid.add(cell, j + 1, i + 1);
            }
        }

        VBox container = new VBox(5, boardLabel, grid);
        container.setAlignment(Pos.CENTER);
        return container;
    }

    // Update player's own board display
    private void updateMyBoard() {
        int[][] fullBoard = myBoard.getFullBoard();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button cell = myBoardButtons[i][j];
                if (cell == null) {
                    continue;
                }

                int value = fullBoard[i][j];
                if (value == SeaBattleCell.SHIP) {
                    cell.setText("■");
                    cell.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: #ecf0f1; -fx-font-size: 16px;");
                } else if (value == SeaBattleCell.HIT) {
                    cell.setText("💥");
                    cell.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px;");
                } else if (value == SeaBattleCell.MISS) {
                    cell.setText("·");
                    cell.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px;");
                } else {
                    cell.setText(" ");
                    cell.setStyle("-fx-background-color: #3498db; -fx-border-color: #2c3e50;");
                }
            }
        }
    }

    // Update enemy board display (only shots results)
    private void updateEnemyBoard() {
        System.out.println("🔄 updateEnemyBoard() called");
        int[][] visibleBoard = enemyBoard.getVisibleBoard();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button cell = enemyBoardButtons[i][j];
                if (cell == null) {
                    continue;
                }

                int value = visibleBoard[i][j];
                if (value == SeaBattleCell.HIT) {
                    if (!cell.getText().equals("💥")) {
                        System.out.println("   Updating [" + i + "," + j + "] to HIT");
                        cell.setText("💥");
                        cell.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px");
                        cell.setDisable(true);
                    }
                } else if (value == SeaBattleCell.MISS) {
                    if (!cell.getText().equals("·")) {
                        System.out.println("   Updating [" + i + "," + j + "] to MISS");
                        cell.setText("·");
                        cell.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px;");
                        cell.setDisable(true);
                    }
                } else {
                    cell.setText(" ");
                    cell.setStyle("-fx-background-color: #e67e22; -fx-border-color: #2c3e50;");
                    if (!waitingForOpponent && !gameOver && isMyTurn) {
                        cell.setDisable(false);
                    }
                }
            }
        }
    }

    // Player makes a shot
    private void makeShoot(int row, int col) {
        if (gameOver || !isMyTurn || waitingForOpponent) {
            return;
        }

        // Check if already shot here
        if (enemyBoard.getCell(row, col).isHit() || enemyBoard.getCell(row, col).isMiss()) {
            statusLabel.setText("❌ Вы уже стреляли в эту клетку!");
            statusLabel.setTextFill(Color.RED);
            return;
        }

        isMyTurn = false;
        statusLabel.setText("Выстрел отправлен...");
        statusLabel.setTextFill(Color.ORANGE);

        // Send shot to opponent
        sendMove(row + "," + col);
    }

    // Called when opponent's shot is received
    @Override
    public void onOpponentMove(String moveData) {
        String[] coords = moveData.split(",");
        int row = Integer.parseInt(coords[0]);
        int col = Integer.parseInt(coords[1]);

        // Process shot on player's board
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

        // Send result back to opponent
        sendMove(resultType + "|" + row + "," + col);

        // Check if player lost
        if (myBoard.allShipsDestroyed()) {
            gameOver = true;
            statusLabel.setText("😢 ВЫ ПРОИГРАЛИ! Все ваши корабли уничтожены!");
            statusLabel.setTextFill(Color.RED);
            disableBoards();
            return;
        }

        // Give turn back to player
        isMyTurn = true;
        statusLabel.setText("✅ Ваш ход!");
        statusLabel.setTextFill(Color.GREEN);

    }

    // Called when shot result is received
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

        // Process result on enemy board
        if ("hit".equals(result) || "kill".equals(result)) {
            // Mark as HIT on enemy board
            enemyBoard.getCell(row, col).markAsHit();
            updateEnemyBoard();

            if ("kill".equals(result)) {
                // Mark the hit cell as HIT first
                enemyBoard.getCell(row, col).markAsHit();

                // Find all connected HIT cells (the entire ship)
                List<int[]> shipCells = findConnectedShipCells(row, col);

                System.out.println("   Found " + shipCells.size() + " ship cells");

                // Mark surrounding area for each ship cell
                for (int[] cell : shipCells) {
                    int r = cell[0];
                    int c = cell[1];

                    // Check all 8 surrounding cells
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
            // Mark as MISS on enemy board
            enemyBoard.getCell(row, col).markAsMiss();
            updateEnemyBoard();
            statusLabel.setText("💨 Промах!");
            statusLabel.setTextFill(Color.ORANGE);
        }

        // Turn passes to opponent
        isMyTurn = false;
        statusLabel.setText("Ожидание хода соперника...");
        statusLabel.setTextFill(Color.ORANGE);
    }

    private List<int[]> findConnectedShipCells(int startRow, int startCol) {
        List<int[]> shipCells = new ArrayList<>();
        boolean[][] visited = new boolean[10][10];

        // Use BFS to find all connected HIT cells
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int r = current[0];
            int c = current[1];

            shipCells.add(new int[]{r, c});

            // Check 4-directional neighbors (up, down, left, right)
            // Ships are not diagonal, so only cardinal directions
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];

                if (nr >= 0 && nr < 10 && nc >= 0 && nc < 10 && !visited[nr][nc]) {
                    SeaBattleCell cell = enemyBoard.getCell(nr, nc);
                    if (cell.isHit()) {  // Only HIT cells belong to the same ship
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
