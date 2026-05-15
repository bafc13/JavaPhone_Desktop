package com.mycompany.javaphone_nir2.games;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChessModule extends FXGLGame {

    private ThemeApplier themeApplier = new ThemeApplier();
    private Label statusLabel;
    private ChessCell[][] cellButtons = new ChessCell[8][8];
    private Board chessBoard;
    private Square selectedSquare = null;
    private boolean gameOver = false;

    // Colors
    private static final Color DARK_CELL = Color.rgb(69, 79, 151);
    private static final Color LIGHT_CELL = Color.rgb(218, 222, 247);

    // Image cache for chess pieces
    private static final Map<Piece, Image> pieceImages = new HashMap<>();

    static {
        try {
            // White pieces
            pieceImages.put(Piece.WHITE_KING, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/wK.png"));
            pieceImages.put(Piece.WHITE_QUEEN, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/wQ.png"));
            pieceImages.put(Piece.WHITE_ROOK, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/wR.png"));
            pieceImages.put(Piece.WHITE_BISHOP, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/wB.png"));
            pieceImages.put(Piece.WHITE_KNIGHT, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/wN.png"));
            pieceImages.put(Piece.WHITE_PAWN, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/wP.png"));

            // Black pieces
            pieceImages.put(Piece.BLACK_KING, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/bK.png"));
            pieceImages.put(Piece.BLACK_QUEEN, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/bQ.png"));
            pieceImages.put(Piece.BLACK_ROOK, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/bR.png"));
            pieceImages.put(Piece.BLACK_BISHOP, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/bB.png"));
            pieceImages.put(Piece.BLACK_KNIGHT, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/bN.png"));
            pieceImages.put(Piece.BLACK_PAWN, loadImage("src/main/java/com/mycompany/javaphone_nir2/games/chess/bP.png"));
        } catch (Exception e) {
            System.err.println("Error loading chess piece images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Image loadImage(String path) {
        java.io.InputStream is = ChessModule.class.getResourceAsStream(path);
        if (is != null) {
            return new Image(is);
        }
        try {
            return new Image("file:" + path);
        } catch (Exception e) {
            System.err.println("Could not load image: " + path);
            return null;
        }
    }

    @Override
    public void showUI() {
        gameStage = new Stage();
        gameStage.setTitle("Шахматы");

        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20; -fx-background-color: #dadef7;");

        statusLabel = new Label(waitingForOpponent ? "Ожидание соперника..." : "Ваш ход!");
        statusLabel.setFont(Font.font(16));
        statusLabel.setTextFill(Color.WHITE);

        chessBoard = new Board();
        GridPane boardGrid = createChessBoard();

        Button backButton = new Button("Выйти в меню");
        backButton.setOnAction(e -> {
            gameStage.close();
            if (controller != null) {
                controller.closeGame();
            }
        });

        root.getChildren().addAll(statusLabel, boardGrid, backButton);

        Scene scene = new Scene(root, 600, 700);
        themeApplier.applyThemeToScene(scene);
        gameStage.setScene(scene);
        Platform.runLater(() -> updateBoardUI());
    }

    private GridPane createChessBoard() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);

        for (int j = 0; j < 8; j++) {
            Label letter = new Label(String.valueOf((char) ('a' + j)));
            letter.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12px; -fx-font-weight: bold;");
            letter.setAlignment(Pos.CENTER);
            grid.add(letter, j + 1, 0);
        }

        for (int i = 0; i < 8; i++) {
            Label number = new Label(String.valueOf(8 - i));
            number.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12px; -fx-font-weight: bold;");
            number.setAlignment(Pos.CENTER);
            grid.add(number, 0, i + 1);
        }

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessCell cell = new ChessCell(i, j, 60);

                String bgColor;
                if ((i + j) % 2 == 0) {
                    bgColor = toRgbCode(LIGHT_CELL);
                } else {
                    bgColor = toRgbCode(DARK_CELL);
                }
                cell.setStyle("-fx-background-color: " + bgColor + ";");

                final int row = i;
                final int col = j;
                cell.setOnMouseClicked(e -> onCellClick(row, col));

                cellButtons[row][col] = cell;
                grid.add(cell, j + 1, i + 1);
            }
        }

        return grid;
    }

    private String toRgbCode(Color color) {
        return "rgb(" + (int) (color.getRed() * 255) + ", "
                + (int) (color.getGreen() * 255) + ", "
                + (int) (color.getBlue() * 255) + ")";
    }

    private void onCellClick(int row, int col) {
        System.out.println("=== CELL CLICKED ===");
        System.out.println("  row=" + row + ", col=" + col);
        System.out.println("  gameOver=" + gameOver);
        System.out.println("  isMyTurn=" + isMyTurn);

        if (gameOver || !isMyTurn || waitingForOpponent) {
            System.out.println("  ❌ CLICK BLOCKED!");
            return;
        }

        Square clickedSquare = getSquareFromRowCol(row, col);
        System.out.println("  Clicked square: " + clickedSquare);

        if (selectedSquare == null) {
            // Select piece
            Piece piece = chessBoard.getPiece(clickedSquare);
            System.out.println("  Piece at square: " + piece);

            if (piece != null && piece.getPieceSide() == getMySide()) {
                selectedSquare = clickedSquare;
                highlightSquare(row, col);
                System.out.println("  ✅ Selected piece at " + selectedSquare);
                statusLabel.setText("Выбрана фигура. Теперь выберите клетку для хода.");
                statusLabel.setTextFill(Color.YELLOW);
            } else {
                System.out.println("  ❌ Cannot select this piece");
                if (piece == null) {
                    statusLabel.setText("❌ Здесь нет фигуры!");
                } else {
                    statusLabel.setText("❌ Это не ваша фигура!");
                }
                statusLabel.setTextFill(Color.RED);
            }
        } else {
            // Make move
            Move move = new Move(selectedSquare, clickedSquare);
            System.out.println("  Attempting move: " + selectedSquare + " -> " + clickedSquare);
            
            // ПРОСТАЯ ПРОВЕРКА - используем встроенный метод isMoveLegal
            if (chessBoard.isMoveLegal(move, true)) {
                System.out.println("  Move is legal!");
                
                // Выполняем ход
                chessBoard.doMove(move);
                updateBoardUI();
                
                isMyTurn = false;
                statusLabel.setText("Ожидание хода соперника...");
                statusLabel.setTextFill(Color.ORANGE);
                
                // Отправляем ход сопернику
                String moveStr = selectedSquare.toString().toLowerCase()
                        + clickedSquare.toString().toLowerCase();
                System.out.println("  Sending move: " + moveStr);
                sendMove(moveStr);
                
                checkGameOver();
            } else {
                System.out.println("  Move is illegal!");
                statusLabel.setText("❌ Некорректный ход!");
                statusLabel.setTextFill(Color.RED);
            }
            
            selectedSquare = null;
            clearHighlights();
        }
    }

    private void highlightSquare(int row, int col) {
        clearHighlights();
        ChessCell cell = cellButtons[row][col];
        if (cell != null) {
            cell.setStyle(cell.getStyle() + "-fx-border-color: yellow; -fx-border-width: 3px;");
        }
    }

    private void clearHighlights() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessCell cell = cellButtons[i][j];
                if (cell != null) {
                    String bgColor;
                    if ((i + j) % 2 == 0) {
                        bgColor = toRgbCode(LIGHT_CELL);
                    } else {
                        bgColor = toRgbCode(DARK_CELL);
                    }
                    String style = "-fx-background-color: " + bgColor + ";";
                    cell.setStyle(style);
                }
            }
        }
    }

    private void updateBoardUI() {
        System.out.println("=== UPDATE BOARD UI ===");
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Square sq = getSquareFromRowCol(i, j);
                Piece p = chessBoard.getPiece(sq);
                Image img = getPieceImage(p);
                cellButtons[i][j].setPieceImage(img);
            }
        }
    }

    private Image getPieceImage(Piece piece) {
        if (piece == null) {
            return null;
        }
        return pieceImages.get(piece);
    }

    private void checkGameOver() {
        if (chessBoard.isMated()) {
            gameOver = true;
            Side loser = chessBoard.getSideToMove();
            Side winner = (loser == Side.WHITE) ? Side.BLACK : Side.WHITE;
            
            if (winner == getMySide()) {
                statusLabel.setText("🎉 ВЫ ПОБЕДИЛИ! 🎉");
                statusLabel.setTextFill(Color.GREEN);
            } else {
                statusLabel.setText("😢 ВЫ ПРОИГРАЛИ! 😢");
                statusLabel.setTextFill(Color.RED);
            }
        } else if (chessBoard.isStaleMate()) {
            gameOver = true;
            statusLabel.setText("🤝 ПАТ! НИЧЬЯ! 🤝");
            statusLabel.setTextFill(Color.ORANGE);
        }
    }

    private Side getMySide() {
        return amIHost ? Side.WHITE : Side.BLACK;
    }

    private Square getSquareFromRowCol(int row, int col) {
        int rank = 8 - row;
        char file = (char) ('A' + col);
        String squareName = file + "" + rank;
        return Square.valueOf(squareName);
    }

    @Override
    public void onOpponentMove(String moveData) {
        System.out.println("Chess opponent move: " + moveData);

        try {
            if (moveData.length() < 4) {
                System.err.println("Invalid move format: " + moveData);
                return;
            }

            String fromStr = moveData.substring(0, 2).toUpperCase();
            String toStr = moveData.substring(2, 4).toUpperCase();

            Square from = Square.valueOf(fromStr);
            Square to = Square.valueOf(toStr);

            Move move = new Move(from, to);
            
            // Проверяем легальность хода соперника
            if (chessBoard.isMoveLegal(move, true)) {
                if (chessBoard.doMove(move)) {
                    System.out.println("Opponent move: " + from + " -> " + to);
                    
                    Platform.runLater(() -> {
                        updateBoardUI();
                        checkGameOver();
                    });
                    
                    if (!gameOver) {
                        isMyTurn = true;
                        statusLabel.setText("✅ Ваш ход!");
                        statusLabel.setTextFill(Color.GREEN);
                    }
                } else {
                    System.err.println("Failed to execute opponent move: " + moveData);
                }
            } else {
                System.err.println("Illegal move from opponent: " + moveData);
            }
        } catch (Exception e) {
            System.err.println("Error processing opponent move: " + moveData);
            e.printStackTrace();
        }
    }

    private Side getOpponentSide() {
        return getMySide() == Side.WHITE ? Side.BLACK : Side.WHITE;
    }

    @Override
    public void startBattle() {
        System.out.println("=== CHESS START BATTLE ===");
        System.out.println("  amIHost=" + amIHost);

        waitingForOpponent = false;
        isMyTurn = amIHost;

        System.out.println("  isMyTurn=" + isMyTurn);

        Platform.runLater(() -> {
            updateBoardUI();

            if (isMyTurn) {
                statusLabel.setText("✅ Ваш ход!");
                statusLabel.setTextFill(Color.GREEN);
            } else {
                statusLabel.setText("Ожидание хода соперника...");
                statusLabel.setTextFill(Color.ORANGE);
            }
        });
    }

    @Override
    public void cleanup() {
        chessBoard = null;
        cellButtons = null;
    }

    private static class ChessCell extends StackPane {
        private final ImageView pieceImageView;
        private final int row;
        private final int col;

        public ChessCell(int row, int col, double size) {
            this.row = row;
            this.col = col;

            setPrefSize(size, size);
            setMinSize(size, size);
            setMaxSize(size, size);

            pieceImageView = new ImageView();
            pieceImageView.setFitWidth(size * 0.8);
            pieceImageView.setFitHeight(size * 0.8);
            pieceImageView.setPreserveRatio(true);
            getChildren().add(pieceImageView);
        }

        public void setPieceImage(Image image) {
            pieceImageView.setImage(image);
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }
    }
}