package com.mycompany.javaphone_nir2.games;

import com.almasb.fxgl.dsl.FXGL;
import com.mycompany.javaphone_nir2.games.sea_battle.SeaBattleBoard;
import com.mycompany.javaphone_nir2.games.sea_battle.SeaBattleCell;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class SeaBattleModule extends FXGLGame {

    private Label statusLabel;
    private Button[][] myBoardButtons = new Button[10][10];
    private Button[][] enemyBoardButtons = new Button[10][10];

    private SeaBattleBoard myBoard;
    private SeaBattleBoard enemyBoard;

    private int destroyedShipsCount = 0;
    private boolean gameOver = false;

    @Override
    public void showUI() {
        gameStage = new Stage();
        gameStage.setTitle("Морской бой");

        myBoard = new SeaBattleBoard();
        enemyBoard = new SeaBattleBoard();
        myBoard.randomizeShips();

        BorderPane root = new BorderPane();
        themeHelper.applyToContainer(root);
        root.setStyle(null); // убираем inline style

        statusLabel = new Label(waitingForOpponent ? "Ожидание соперника..." : "Ваш ход!");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.getStyleClass().add("game-status");
        root.setTop(statusLabel);

        HBox boards = new HBox(30);
        boards.setAlignment(Pos.CENTER);

        VBox myBoardBox = createBoard("⚓ МОИ КОРАБЛИ", true);
        VBox enemyBoardBox = createBoard("🎯 ОБСТРЕЛ ПРОТИВНИКА", false);

        boards.getChildren().addAll(myBoardBox, enemyBoardBox);
        root.setCenter(boards);

        Button backButton = new Button("Выйти в меню");
        themeHelper.styleButton(backButton);
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
        themeHelper.applyThemeToScene(scene);

        gameStage.setScene(scene);

        updateMyBoard();
        updateEnemyBoard();
    }

    private VBox createBoard(String title, boolean isMyBoard) {
        Label boardLabel = new Label(title);
        boardLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        boardLabel.getStyleClass().add("title-text");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(3);
        grid.setVgap(3);

        // Letters (А-К)
        for (int j = 0; j < 10; j++) {
            Label letter = new Label(String.valueOf((char) ('А' + j)));
            letter.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            letter.getStyleClass().add("body-text");
            letter.setAlignment(Pos.CENTER);
            grid.add(letter, j + 1, 0);
        }

        // Numbers (1-10)
        for (int i = 0; i < 10; i++) {
            Label number = new Label(String.valueOf(i + 1));
            number.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            number.getStyleClass().add("body-text");
            number.setAlignment(Pos.CENTER);
            grid.add(number, 0, i + 1);
        }

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button cell = new Button(" ");
                cell.setPrefSize(38, 38);
                cell.setFont(Font.font(14));

                if (isMyBoard) {
                    // Своё поле
                    cell.getStyleClass().addAll("sea-battle-cell", "sea-battle-cell-my");
                    cell.setDisable(true);
                    myBoardButtons[i][j] = cell;
                } else {
                    // Поле противника
                    cell.getStyleClass().addAll("sea-battle-cell", "sea-battle-cell-enemy");

                    final int row = i;
                    final int col = j;

                    cell.setOnMouseEntered(e -> {
                        if (!gameOver && isMyTurn && !waitingForOpponent && !cell.isDisabled()) {
                            cell.getStyleClass().add("sea-battle-cell-enemy-hover");
                        }
                    });

                    cell.setOnMouseExited(e -> {
                        cell.getStyleClass().remove("sea-battle-cell-enemy-hover");
                    });

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

                // Очищаем все классы, кроме базовых
                cell.getStyleClass().removeAll("sea-battle-water", "sea-battle-ship", "sea-battle-hit", "sea-battle-miss");
                cell.getStyleClass().add("sea-battle-cell-my");

                int value = fullBoard[i][j];
                if (value == SeaBattleCell.SHIP) {
                    cell.setText("□");
                    cell.getStyleClass().add("sea-battle-ship");
                } else if (value == SeaBattleCell.HIT) {
                    cell.setText("✗");
                    cell.getStyleClass().add("sea-battle-hit");
                } else if (value == SeaBattleCell.MISS) {
                    cell.setText("•");
                    cell.getStyleClass().add("sea-battle-miss");
                } else {
                    cell.setText(" ");
                    cell.getStyleClass().add("sea-battle-water");
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

                // Очищаем все классы, кроме базовых
                cell.getStyleClass().removeAll("sea-battle-water", "sea-battle-hit", "sea-battle-miss");
                cell.getStyleClass().add("sea-battle-cell-enemy");

                int value = visibleBoard[i][j];
                if (value == SeaBattleCell.HIT) {
                    if (!cell.getText().equals("✗")) {
                        System.out.println("   Updating [" + i + "," + j + "] to HIT");
                        cell.setText("✗");
                        cell.getStyleClass().add("sea-battle-hit");
                        cell.setDisable(true);
                    }
                } else if (value == SeaBattleCell.MISS) {
                    if (!cell.getText().equals("•")) {
                        System.out.println("   Updating [" + i + "," + j + "] to MISS");
                        cell.setText("•");
                        cell.getStyleClass().add("sea-battle-miss");
                        cell.setDisable(true);
                    }
                } else {
                    cell.setText(" ");
                    cell.getStyleClass().add("sea-battle-water");
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
    
    Button cell = enemyBoardButtons[row][col];
    double cellX = cell.getLayoutX();
    double cellY = cell.getLayoutY();
    
    // Получаем абсолютные координаты клетки
    javafx.geometry.Point2D sceneCoords = cell.localToScene(cellX, cellY);
    double absX = sceneCoords.getX();
    double absY = sceneCoords.getY();
    
    // Временно отключаем кнопку до получения результата
    cell.setDisable(true);
    
    isMyTurn = false;
    statusLabel.setText("Выстрел отправлен...");
    
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
    
    // Получаем клетку и её координаты для анимации
    Button cell = myBoardButtons[row][col];
    javafx.geometry.Point2D cellCoords = getCellCenterScreenCoords(cell);

    switch (result) {
        case 0:
            resultType = "miss";
            message = "💨 Противник промахнулся!";
            statusLabel.getStyleClass().removeAll("game-status-success", "game-status-error");
            statusLabel.getStyleClass().add("game-status-warning");
            FXGL.play("com/mycompany/javaphone_nir2/games/sounds/miss.wav");
            showWaterSplash(cellCoords.getX(), cellCoords.getY());
            break;
        case 1:
            resultType = "hit";
            message = "💥 Противник попал!";
            statusLabel.getStyleClass().removeAll("game-status-success", "game-status-warning");
            statusLabel.getStyleClass().add("game-status-error");
            FXGL.play("com/mycompany/javaphone_nir2/games/sounds/hit.wav");
            showExplosion(cellCoords.getX(), cellCoords.getY());
            break;
        case 2:
            resultType = "kill";
            message = "💀 Противник уничтожил ваш корабль! Осталось кораблей: " + myBoard.getShipsAlive();
            statusLabel.getStyleClass().removeAll("game-status-success", "game-status-warning");
            statusLabel.getStyleClass().add("game-status-error");
            FXGL.play("com/mycompany/javaphone_nir2/games/sounds/destroyed.wav");
            showShipDestroyed(row, col, true);
            break;
        default:
            resultType = "error";
            message = "❌ Ошибка!";
            statusLabel.getStyleClass().add("game-status-error");
    }

    statusLabel.setText(message);
    updateMyBoard();

    sendMove(resultType + "|" + row + "," + col);

    if (myBoard.allShipsDestroyed()) {
        gameOver = true;
        statusLabel.setText("😢 ВЫ ПРОИГРАЛИ! Все ваши корабли уничтожены!");
        statusLabel.getStyleClass().add("game-status-error");
        disableBoards();
        return;
    }

    isMyTurn = true;
    statusLabel.setText("✅ Ваш ход!");
    statusLabel.getStyleClass().removeAll("game-status-error", "game-status-warning");
    statusLabel.getStyleClass().add("game-status-success");
    updateEnemyBoard();
}
    private void createWaterSplash(double cellX, double cellY) {
        javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) gameStage.getScene().getRoot();
        
        // Создаём 5-10 капель воды
        for (int i = 0; i < 8; i++) {
            Circle droplet = new Circle(3, Color.LIGHTBLUE);
            droplet.setCenterX(cellX + 15 + Math.random() * 20);
            droplet.setCenterY(cellY + 15 + Math.random() * 20);
            root.getChildren().add(droplet);
            
            // Анимация падения и исчезновения
            javafx.animation.TranslateTransition fall = new javafx.animation.TranslateTransition(
                Duration.millis(400), droplet);
            fall.setByY(30 + Math.random() * 20);
            fall.setByX(Math.random() * 20 - 10);
            
            FadeTransition fade = new FadeTransition(Duration.millis(400), droplet);
            fade.setFromValue(1);
            fade.setToValue(0);
            
            ParallelTransition parallel = new ParallelTransition(fall, fade);
            parallel.setOnFinished(e -> root.getChildren().remove(droplet));
            parallel.play();
        }
    }
    
    private void createExplosion(double cellX, double cellY) {
        javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) gameStage.getScene().getRoot();
        
        // Создаём искры/осколки
        for (int i = 0; i < 15; i++) {
            Circle spark = new Circle(2, Color.ORANGERED);
            spark.setCenterX(cellX + 19);
            spark.setCenterY(cellY + 19);
            root.getChildren().add(spark);
            
            double angle = Math.random() * 2 * Math.PI;
            double distance = 30 + Math.random() * 40;
            
            javafx.animation.TranslateTransition fly = new javafx.animation.TranslateTransition(
                Duration.millis(500), spark);
            fly.setByX(Math.cos(angle) * distance);
            fly.setByY(Math.sin(angle) * distance);
            
            FadeTransition fade = new FadeTransition(Duration.millis(500), spark);
            fade.setFromValue(1);
            fade.setToValue(0);
            
            ParallelTransition parallel = new ParallelTransition(fly, fade);
            parallel.setOnFinished(e -> root.getChildren().remove(spark));
            parallel.play();
        }
        
        // Добавляем центральную вспышку
        Circle flash = new Circle(10, Color.YELLOW);
        flash.setCenterX(cellX + 19);
        flash.setCenterY(cellY + 19);
        root.getChildren().add(flash);
        
        ScaleTransition scaleFlash = new ScaleTransition(Duration.millis(200), flash);
        scaleFlash.setFromX(0.5);
        scaleFlash.setFromY(0.5);
        scaleFlash.setToX(2);
        scaleFlash.setToY(2);
        
        FadeTransition fadeFlash = new FadeTransition(Duration.millis(200), flash);
        fadeFlash.setFromValue(1);
        fadeFlash.setToValue(0);
        
        ParallelTransition parallelFlash = new ParallelTransition(scaleFlash, fadeFlash);
        parallelFlash.setOnFinished(e -> root.getChildren().remove(flash));
        parallelFlash.play();
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

    // Получаем клетку и её координаты для анимации
    Button cell = enemyBoardButtons[row][col];
    javafx.geometry.Point2D cellCoords = getCellCenterScreenCoords(cell);

    if ("hit".equals(result) || "kill".equals(result)) {
        // Анимация попадания
        showExplosion(cellCoords.getX(), cellCoords.getY());
        FXGL.play("com/mycompany/javaphone_nir2/games/sounds/hit.wav");
        
        enemyBoard.getCell(row, col).markAsHit();
        updateEnemyBoard();

        if ("kill".equals(result)) {
            // Анимация уничтожения корабля (уже вызывается один раз)
            showShipDestroyed(row, col, false);
            
            List<int[]> shipCells = findConnectedShipCells(row, col);
            System.out.println("   Found " + shipCells.size() + " ship cells");

            for (int[] shipCell : shipCells) {
                int r = shipCell[0];
                int c = shipCell[1];

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
            statusLabel.getStyleClass().removeAll("game-status-error", "game-status-warning");
            statusLabel.getStyleClass().add("game-status-success");
            FXGL.play("com/mycompany/javaphone_nir2/games/sounds/destroyed.wav");

            if (destroyedShipsCount >= 10) {
                gameOver = true;
                statusLabel.setText("🏆 ПОБЕДА! Все корабли противника уничтожены!");
                statusLabel.getStyleClass().add("game-status-success");
                disableBoards();
                return;
            }
        } else {
            statusLabel.setText("💥 Попадание!");
            statusLabel.getStyleClass().removeAll("game-status-error", "game-status-warning");
            statusLabel.getStyleClass().add("game-status-success");
        }

        if (destroyedShipsCount >= 10) {
            gameOver = true;
            statusLabel.setText("🏆 ПОБЕДА! Все корабли противника уничтожены!");
            statusLabel.getStyleClass().add("game-status-success");
            disableBoards();
            return;
        }
    } else if ("miss".equals(result)) {
        FXGL.play("com/mycompany/javaphone_nir2/games/sounds/miss.wav");
        showWaterSplash(cellCoords.getX(), cellCoords.getY());
        
        enemyBoard.getCell(row, col).markAsMiss();
        updateEnemyBoard();
        statusLabel.setText("💨 Промах!");
        statusLabel.getStyleClass().removeAll("game-status-success", "game-status-error");
        statusLabel.getStyleClass().add("game-status-warning");
    }

    isMyTurn = false;
    statusLabel.setText("Ожидание хода соперника...");
    statusLabel.getStyleClass().removeAll("game-status-success", "game-status-error");
    statusLabel.getStyleClass().add("game-status-warning");
    updateEnemyBoard();
}

// Вспомогательные методы для анимаций:

private javafx.geometry.Point2D getCellCenterScreenCoords(Button cell) {
    if (cell == null) return new javafx.geometry.Point2D(0, 0);
    double x = cell.getLayoutX() + cell.getWidth() / 2;
    double y = cell.getLayoutY() + cell.getHeight() / 2;
    return cell.localToScene(x, y);
}

private void showWaterSplash(double x, double y) {
    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) gameStage.getScene().getRoot();
    
    // Всплеск воды - капли
    for (int i = 0; i < 8; i++) {
        javafx.scene.shape.Circle droplet = new javafx.scene.shape.Circle(3, javafx.scene.paint.Color.LIGHTBLUE);
        droplet.setCenterX(x);
        droplet.setCenterY(y);
        root.getChildren().add(droplet);
        
        double angle = Math.random() * 2 * Math.PI;
        double distance = 20 + Math.random() * 30;
        
        javafx.animation.TranslateTransition fly = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(400), droplet);
        fly.setByX(Math.cos(angle) * distance);
        fly.setByY(Math.sin(angle) * distance);
        
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(400), droplet);
        fade.setFromValue(1);
        fade.setToValue(0);
        
        javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(fly, fade);
        parallel.setOnFinished(e -> root.getChildren().remove(droplet));
        parallel.play();
    }
    
    // Центральная капля
    javafx.scene.shape.Circle splash = new javafx.scene.shape.Circle(8, javafx.scene.paint.Color.CYAN);
    splash.setCenterX(x);
    splash.setCenterY(y);
    root.getChildren().add(splash);
    
    javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(
        javafx.util.Duration.millis(300), splash);
    scale.setFromX(0.5);
    scale.setFromY(0.5);
    scale.setToX(2);
    scale.setToY(2);
    
    javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
        javafx.util.Duration.millis(300), splash);
    fade.setFromValue(1);
    fade.setToValue(0);
    
    javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(scale, fade);
    parallel.setOnFinished(e -> root.getChildren().remove(splash));
    parallel.play();
}

private void showExplosion(double x, double y) {
    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) gameStage.getScene().getRoot();
    
    // Искры/осколки
    for (int i = 0; i < 12; i++) {
        javafx.scene.shape.Circle spark = new javafx.scene.shape.Circle(2, 
            javafx.scene.paint.Color.ORANGERED);
        spark.setCenterX(x);
        spark.setCenterY(y);
        root.getChildren().add(spark);
        
        double angle = Math.random() * 2 * Math.PI;
        double distance = 25 + Math.random() * 40;
        
        javafx.animation.TranslateTransition fly = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(500), spark);
        fly.setByX(Math.cos(angle) * distance);
        fly.setByY(Math.sin(angle) * distance);
        
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(500), spark);
        fade.setFromValue(1);
        fade.setToValue(0);
        
        javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(fly, fade);
        parallel.setOnFinished(e -> root.getChildren().remove(spark));
        parallel.play();
    }
    
    // Центральная вспышка
    javafx.scene.shape.Circle flash = new javafx.scene.shape.Circle(12, javafx.scene.paint.Color.YELLOW);
    flash.setCenterX(x);
    flash.setCenterY(y);
    root.getChildren().add(flash);
    
    javafx.animation.ScaleTransition scaleFlash = new javafx.animation.ScaleTransition(
        javafx.util.Duration.millis(200), flash);
    scaleFlash.setFromX(0.5);
    scaleFlash.setFromY(0.5);
    scaleFlash.setToX(2.5);
    scaleFlash.setToY(2.5);
    
    javafx.animation.FadeTransition fadeFlash = new javafx.animation.FadeTransition(
        javafx.util.Duration.millis(200), flash);
    fadeFlash.setFromValue(1);
    fadeFlash.setToValue(0);
    
    javafx.animation.ParallelTransition parallelFlash = new javafx.animation.ParallelTransition(scaleFlash, fadeFlash);
    parallelFlash.setOnFinished(e -> root.getChildren().remove(flash));
    parallelFlash.play();
}

private void showShipDestroyed(int row, int col, boolean isMyBoard) {
    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) gameStage.getScene().getRoot();
    
    // Находим все клетки корабля
    List<int[]> shipCells;
    if (isMyBoard) {
        shipCells = findConnectedShipCellsOnMyBoard(row, col);
    } else {
        shipCells = findConnectedShipCells(row, col);
    }
    
    // Создаём взрывы на каждой клетке корабля с задержкой
    for (int i = 0; i < shipCells.size(); i++) {
        int[] shipCell = shipCells.get(i);
        Button shipButton = isMyBoard ? myBoardButtons[shipCell[0]][shipCell[1]] : enemyBoardButtons[shipCell[0]][shipCell[1]];
        
        if (shipButton != null) {
            javafx.geometry.Point2D coords = getCellCenterScreenCoords(shipButton);
            
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(i * 150));
            delay.setOnFinished(e -> {
                showExplosion(coords.getX(), coords.getY());
                
                // Добавляем дополнительные искры для эффекта
                for (int s = 0; s < 5; s++) {
                    javafx.scene.shape.Circle extraSpark = new javafx.scene.shape.Circle(2, 
                        javafx.scene.paint.Color.RED);
                    extraSpark.setCenterX(coords.getX());
                    extraSpark.setCenterY(coords.getY());
                    root.getChildren().add(extraSpark);
                    
                    double angle = Math.random() * 2 * Math.PI;
                    javafx.animation.TranslateTransition fly = new javafx.animation.TranslateTransition(
                        javafx.util.Duration.millis(400), extraSpark);
                    fly.setByX(Math.cos(angle) * 30);
                    fly.setByY(Math.sin(angle) * 30);
                    
                    javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(400), extraSpark);
                    fade.setFromValue(1);
                    fade.setToValue(0);
                    
                    javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(fly, fade);
                    parallel.setOnFinished(ev -> root.getChildren().remove(extraSpark));
                    parallel.play();
                }
            });
            delay.play();
        }
    }
}

// Дополнительный метод для поиска корабля на своём поле
private List<int[]> findConnectedShipCellsOnMyBoard(int startRow, int startCol) {
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
                int value = myBoard.getFullBoard()[nr][nc];
                if (value == SeaBattleCell.SHIP || value == SeaBattleCell.HIT) {
                    visited[nr][nc] = true;
                    queue.add(new int[]{nr, nc});
                }
            }
        }
    }
    
    return shipCells;
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
            statusLabel.getStyleClass().add("game-status-success");
        } else {
            statusLabel.setText("Ожидание хода соперника...");
            statusLabel.getStyleClass().add("game-status-warning");
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