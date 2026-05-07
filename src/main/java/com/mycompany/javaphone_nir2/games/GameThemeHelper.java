package com.mycompany.javaphone_nir2.games;

import com.mycompany.javaphone_nir2.models.SettingsManager;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * Helper class для применения тем в игровом модуле
 * Использует SettingsManager для получения текущей темы
 */
public class GameThemeHelper {
    
    private static GameThemeHelper instance;
    private SettingsManager settingsManager;
    private String currentTheme = "light";
    
    private GameThemeHelper() {}
    
    public static GameThemeHelper getInstance() {
        if (instance == null) {
            instance = new GameThemeHelper();
        }
        return instance;
    }
    
    /**
     * Инициализация - получаем ссылку на SettingsManager
     */
    public void init() {
        this.settingsManager = SettingsManager.getInstance();
        this.currentTheme = settingsManager.getTheme();
        System.out.println("🎨 GameThemeHelper initialized with theme: " + currentTheme);
        
        // Подписываемся на изменения темы
        settingsManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            System.out.println("🎨 Theme changed from " + oldTheme + " to " + newTheme);
            this.currentTheme = newTheme;
            onThemeChanged();
        });
    }
    
    /**
     * Получить текущую тему
     */
    public String getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Проверить, тёмная ли тема
     */
    public boolean isDarkTheme() {
        return "dark".equals(currentTheme);
    }
    
    /**
     * Получить цвет фона для текущей темы
     */
    public String getBackgroundColor() {
        return isDarkTheme() ? "#1e1e2e" : "#f5f5f5";
    }
    
    /**
     * Получить цвет текста для текущей темы
     */
    public String getTextColor() {
        return isDarkTheme() ? "#ffffff" : "#000000";
    }
    
    /**
     * Получить цвет кнопок для текущей темы
     */
    public String getButtonColor() {
        return isDarkTheme() ? "#4a4a5a" : "#27ae60";
    }
    
    /**
     * Получить цвет поля (для крестиков-ноликов)
     */
    public String getBoardColor() {
        return isDarkTheme() ? "#2d2d3d" : "#ffffff";
    }
    
    /**
     * Получить цвет линий сетки
     */
    public String getGridColor() {
        return isDarkTheme() ? "#6c6c7c" : "#333333";
    }
    
    /**
     * Получить цвет для X/O
     */
    public String getXColor() {
        return isDarkTheme() ? "#4ecdc4" : "#e74c3c";
    }
    
    public String getOColor() {
        return isDarkTheme() ? "#ffe66d" : "#3498db";
    }
    
    /**
     * Применить тему к Scene через CSS
     */
    public void applyThemeToScene(Scene scene) {
        if (scene == null) return;
        applyInlineTheme(scene);
    }
    
    /**
     * Применяем inline стили
     */
    private void applyInlineTheme(Scene scene) {
        if (isDarkTheme()) {
            scene.getRoot().setStyle(
                "-fx-base: #1e1e2e; " +
                "-fx-background: #1e1e2e; " +
                "-fx-control-inner-background: #2d2d3d; " +
                "-fx-text-fill: #ffffff;"
            );
        } else {
            scene.getRoot().setStyle(
                "-fx-base: #f5f5f5; " +
                "-fx-background: #f5f5f5; " +
                "-fx-control-inner-background: #ffffff; " +
                "-fx-text-fill: #000000;"
            );
        }
    }
    
    /**
     * Стилизовать кнопку
     */
    public void styleButton(Button button) {
        String bgColor = getButtonColor();
        button.setStyle(String.format(
            "-fx-font-size: 14px; -fx-padding: 10 20; " +
            "-fx-background-color: %s; -fx-text-fill: #ffffff; " +
            "-fx-background-radius: 5; -fx-cursor: hand;",
            bgColor
        ));
    }
    
    /**
     * Стилизовать текст
     */
    public void styleText(Text text, boolean isTitle) {
        String color = getTextColor();
        if (isTitle) {
            text.setStyle(String.format(
                "-fx-font-size: 24px; -fx-font-weight: bold; -fx-fill: %s;",
                color
            ));
        } else {
            text.setStyle(String.format(
                "-fx-font-size: 14px; -fx-fill: %s;",
                color
            ));
        }
    }
    
    /**
     * Стилизовать Label
     */
    public void styleLabel(Label label, boolean isTitle) {
        String color = getTextColor();
        if (isTitle) {
            label.setStyle(String.format(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: %s;",
                color
            ));
        } else {
            label.setStyle(String.format(
                "-fx-font-size: 14px; -fx-text-fill: %s;",
                color
            ));
        }
    }
    
    /**
     * Применить тему к контейнеру
     */
    public void applyToContainer(Region container) {
        container.setStyle(String.format(
            "-fx-background-color: %s; -fx-padding: 20;",
            getBackgroundColor()
        ));
    }
    
    /**
     * Callback при смене темы - переопределить в играх при необходимости
     */
    protected void onThemeChanged() {
        // Базовый класс ничего не делает, игры переопределят
    }
    
    /**
     * Получить цвет фона для игрового поля
     */
    public String getGameBackground() {
        return isDarkTheme() ? "#1a1a2e" : "#f0f0f0";
    }
}