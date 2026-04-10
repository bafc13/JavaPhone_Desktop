package com.mycompany.javaphone_nir2.games;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ChessModule extends FXGLGame {
    
    private Label statusLabel;
    private Rectangle[][] cells = new Rectangle[8][8];
    private Board chessBoard;
    private Square selectedSquare = null;
    private boolean gameOver = false;
    
    @Override
    public void showUI() {
        gameStage = new Stage();
        gameStage.setTitle("Chess");
        
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20; -fx-background-color: #34495e;");
        
        statusLabel = new Label(waitingForOpponent ? "Waiting for opponent..." : "Your turn!");
        statusLabel.setFont(Font.font(16));
        statusLabel.setTextFill(Color.WHITE);
        
        GridPane boardGrid = createChessBoard();
        
        Button backButton = new Button("Back to Menu");
        backButton.setOnAction(e -> {
            gameStage.close();
            if (controller != null) {
                controller.closeGame();
            }
        });
        
        root.getChildren().addAll(statusLabel, boardGrid, backButton);
        
        Scene scene = new Scene(root, 600, 700);
        gameStage.setScene(scene);
        
        chessBoard = new Board();
        updateBoardUI();
    }
    
    private GridPane createChessBoard() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        
        // Letters (a-h)
        for (int j = 0; j < 8; j++) {
            Label letter = new Label(String.valueOf((char) ('a' + j)));
            letter.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12px;");
            grid.add(letter, j + 1, 0);
        }
        
        // Numbers (1-8)
        for (int i = 0; i < 8; i++) {
            Label number = new Label(String.valueOf(8 - i));
            number.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12px;");
            grid.add(number, 0, i + 1);
        }
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Rectangle cell = new Rectangle(60, 60);
                
                if ((i + j) % 2 == 0) {
                    cell.setFill(Color.BEIGE);
                } else {
                    cell.setFill(Color.BROWN);
                }
                cell.setStroke(Color.BLACK);
                cell.setStrokeWidth(1);
                
                final int row = i;
                final int col = j;
                cell.setOnMouseClicked(e -> onCellClick(row, col));
                
                cells[row][col] = cell;
                grid.add(cell, j + 1, i + 1);
            }
        }
        
        return grid;
    }
    
    private void onCellClick(int row, int col) {
        if (gameOver || !isMyTurn || waitingForOpponent) return;
        
        Square clickedSquare = Square.values()[row * 8 + col];
        
        if (selectedSquare == null) {
            // Select piece
            if (chessBoard.getPiece(clickedSquare) != null) {
                selectedSquare = clickedSquare;
                highlightSquare(row, col, Color.YELLOW);
            }
        } else {
            // Make move
            Move move = new Move(selectedSquare, clickedSquare);
            
            if (chessBoard.isMoveLegal(move, true)) {
                chessBoard.doMove(move);
                updateBoardUI();
                
                isMyTurn = false;
                statusLabel.setText("Waiting for opponent's move...");
                
                // Send move in algebraic notation (e.g., "e2e4")
                String moveStr = selectedSquare.toString().toLowerCase() + 
                                 clickedSquare.toString().toLowerCase();
                sendMove(moveStr);
                
                if (chessBoard.isMated()) {
                    gameOver = true;
                    statusLabel.setText("🎉 YOU WIN! 🎉");
                    statusLabel.setTextFill(Color.GREEN);
                }
            }
            
            selectedSquare = null;
            clearHighlights();
        }
    }
    
    private void highlightSquare(int row, int col, Color color) {
        cells[row][col].setStroke(color);
        cells[row][col].setStrokeWidth(3);
    }
    
    private void clearHighlights() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                cells[i][j].setStroke(Color.BLACK);
                cells[i][j].setStrokeWidth(1);
            }
        }
    }
    
    private void updateBoardUI() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                // TODO: Add piece icons
                // Square square = Square.values()[i * 8 + j];
                // String piece = getPieceSymbol(chessBoard.getPiece(square));
            }
        }
    }
    
    @Override
    public void onOpponentMove(String moveData) {
        // moveData format: "e2e4"
        try {
            Square from = Square.valueOf(moveData.substring(0, 2).toUpperCase());
            Square to = Square.valueOf(moveData.substring(2, 4).toUpperCase());
            Move move = new Move(from, to);
            
            if (chessBoard.isMoveLegal(move, true)) {
                chessBoard.doMove(move);
                updateBoardUI();
                
                if (chessBoard.isMated()) {
                    gameOver = true;
                    statusLabel.setText("😢 YOU LOSE! 😢");
                    statusLabel.setTextFill(Color.RED);
                } else {
                    isMyTurn = true;
                    statusLabel.setText("✅ Your turn!");
                    statusLabel.setTextFill(Color.GREEN);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing move: " + moveData);
        }
    }
    
    @Override
    public void startBattle() {
        waitingForOpponent = false;
        isMyTurn = amIHost;
        
        if (isMyTurn) {
            statusLabel.setText("✅ Your turn!");
            statusLabel.setTextFill(Color.GREEN);
        } else {
            statusLabel.setText("Waiting for opponent's move...");
            statusLabel.setTextFill(Color.ORANGE);
        }
    }
    
    @Override
    public void cleanup() {
        chessBoard = null;
        cells = null;
    }
}