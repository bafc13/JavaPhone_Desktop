/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphone_nir2.games;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ThemeHelper {
    private static String currentTheme = "light";
    private static Color backgroundColor = Color.web("#dadef7");
    private static Color textColor = Color.web("#1a225d");
    private static Color buttonColor = Color.web("#1a225d");
    
    // Загружаем цвета из CSS файла
    public static void loadTheme(String theme) {
        currentTheme = theme;
        String cssFile = theme.equals("dark") ? "/css/chat_main_dark.css" : "/css/chat_main.css";
        
        try (InputStream is = ThemeHelper.class.getResourceAsStream(cssFile)) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Ищем background-color для .chat-container или .game-container
                    if (line.contains("background-color:") && 
                        (line.contains(".chat-container") || line.contains(".game-container") || line.contains(".root"))) {
                        String color = extractColor(line);
                        if (color != null) {
                            backgroundColor = Color.web(color);
                        }
                    }
                    // Ищем цвет текста
                    if (line.contains("-fx-text-fill:") && 
                        (line.contains(".chat-title-label") || line.contains(".game-status"))) {
                        String color = extractColor(line);
                        if (color != null) {
                            textColor = Color.web(color);
                        }
                    }
                    // Ищем цвет кнопок
                    if (line.contains("-fx-background-color:") && line.contains(".header-button")) {
                        String color = extractColor(line);
                        if (color != null && !color.contains("transparent")) {
                            buttonColor = Color.web(color);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load theme: " + e.getMessage());
            // Используем значения по умолчанию
        }
    }
    
    private static String extractColor(String line) {
        int start = line.indexOf("#");
        if (start == -1) return null;
        int end = start + 7; // #RRGGBB
        if (end <= line.length()) {
            return line.substring(start, end);
        }
        return null;
    }
    
    public static Color getBackgroundColor() { return backgroundColor; }
    public static Color getTextColor() { return textColor; }
    public static Color getButtonColor() { return buttonColor; }
    public static String getBackgroundColorHex() { return toHex(backgroundColor); }
    public static String getTextColorHex() { return toHex(textColor); }
    public static String getButtonColorHex() { return toHex(buttonColor); }
    
    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
}
