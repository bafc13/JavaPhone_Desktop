package com.mycompany.javaphone_nir2.games;

import com.mycompany.javaphone_nir2.models.SettingsManager;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class для применения тем в игровом модуле
 * Использует CSS-файлы чата для единого стиля
 */
public class GameThemeHelper {
    
    private static GameThemeHelper instance;
    private SettingsManager settingsManager;
    private String currentTheme = "light";
    private List<Runnable> themeListeners = new ArrayList<>();
    
    private GameThemeHelper() {}
    
    public static GameThemeHelper getInstance() {
        if (instance == null) {
            instance = new GameThemeHelper();
        }
        return instance;
    }
    
    public void init() {
        if (settingsManager != null) return; // уже инициализирован
        
        this.settingsManager = SettingsManager.getInstance();
        this.currentTheme = settingsManager.getTheme();
        System.out.println("🎨 GameThemeHelper initialized with theme: " + currentTheme);
        
        // Подписываемся на изменения темы
        settingsManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            System.out.println("🎨 Theme changed from " + oldTheme + " to " + newTheme);
            this.currentTheme = newTheme;
            // Уведомляем всех слушателей
            for (Runnable listener : themeListeners) {
                listener.run();
            }
        });
    }
    
    public void addThemeListener(Runnable listener) {
        if (!themeListeners.contains(listener)) {
            themeListeners.add(listener);
        }
    }
    
    public void removeThemeListener(Runnable listener) {
        themeListeners.remove(listener);
    }
    
    public String getCurrentTheme() {
        return currentTheme;
    }
    
    public boolean isDarkTheme() {
        return "dark".equals(currentTheme);
    }
    
    /**
     * Получить CSS файл для текущей темы (из чата)
     */
    private String getThemeCss() {
        String cssFile = isDarkTheme() ? "css/chat_main_dark.css" : "css/chat_main.css";
        java.net.URL url = getClass().getResource(cssFile);
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(cssFile);
        }
        return url != null ? url.toExternalForm() : null;
    }
    
    /**
     * Применить тему к Scene - использует CSS чата
     */
    public void applyThemeToScene(Scene scene) {
        if (scene == null) return;
        
        String cssUrl = getThemeCss();
        if (cssUrl != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(cssUrl);
            System.out.println("🎨 Applied theme CSS: " + cssUrl);
        } else {
            System.err.println("⚠️ Could not find theme CSS");
        }
    }
    
    /**
     * Стилизовать кнопку (добавляем класс из чата)
     */
    public void styleButton(Button button) {
        button.getStyleClass().add("header-button");
    }
    
    /**
     * Стилизовать текст
     */
    public void styleText(Text text, boolean isTitle) {
        text.getStyleClass().clear();
        if (isTitle) {
            text.getStyleClass().add("title-text");
        } else {
            text.getStyleClass().add("body-text");
        }
    }
    
    /**
     * Стилизовать Label
     */
    public void styleLabel(Label label, boolean isTitle) {
        label.getStyleClass().clear();
        if (isTitle) {
            label.getStyleClass().add("title-text");
        } else {
            label.getStyleClass().add("body-text");
        }
    }
    
    /**
     * Применить к контейнеру
     */
    public void applyToContainer(Region container) {
        container.getStyleClass().add("game-container");
    }
    
    /**
     * Стили для клетки крестиков-ноликов
     */
    public void styleTicTacToeCell(Button cell, String symbol, boolean isWinning) {
        cell.getStyleClass().clear();
        cell.getStyleClass().add("tic-tac-toe-cell");
        
        if ("X".equals(symbol)) {
            cell.getStyleClass().add("tic-tac-toe-cell-x");
        } else if ("O".equals(symbol)) {
            cell.getStyleClass().add("tic-tac-toe-cell-o");
        }
        
        if (isWinning) {
            cell.setStyle("-fx-effect: dropshadow(gaussian, #f1c40f, 20, 0.8, 0, 0);");
        } else {
            cell.setStyle(null);
        }
    }
}