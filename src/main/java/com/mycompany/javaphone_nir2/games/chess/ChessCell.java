/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphone_nir2.games.chess;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class ChessCell extends StackPane {
    private final ImageView pieceImageView;
    private final int row;
    private final int col;

    public ChessCell(int row, int col, double size) {
        this.row = row;
        this.col = col;

        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        // ImageView будет занимать 80% от размера клетки
        pieceImageView = new ImageView();
        pieceImageView.setFitWidth(size * 0.8);
        pieceImageView.setFitHeight(size * 0.8);
        pieceImageView.setPreserveRatio(true);
        getChildren().add(pieceImageView);
    }

    public void setPieceImage(javafx.scene.image.Image image) {
        pieceImageView.setImage(image);
    }

    // Геттеры для получения координат клетки
    public int getRow() { return row; }
    public int getCol() { return col; }
}
