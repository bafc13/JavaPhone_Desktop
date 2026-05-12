package com.mycompany.javaphone_nir2.games;

import com.mycompany.javaphone_nir2.models.SettingsManager;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GameThemeHelper {

    private static GameThemeHelper instance;
    private SettingsManager settingsManager;
    private String currentTheme = "light";
    private List<Scene> managedScenes = new ArrayList<>();
    private List<Runnable> themeListeners = new ArrayList<>();

    private GameThemeHelper() {
    }

    public static GameThemeHelper getInstance() {
        if (instance == null) {
            instance = new GameThemeHelper();
        }
        return instance;
    }

    public void init() {
        if (settingsManager != null) {
            return;
        }

        this.settingsManager = SettingsManager.getInstance();
        this.currentTheme = settingsManager.getTheme();
        System.out.println("🎨 GameThemeHelper initialized with theme: " + currentTheme);

        settingsManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            System.out.println("🎨 Theme changed: " + oldTheme + " → " + newTheme);
            this.currentTheme = newTheme;

            for (Scene scene : managedScenes) {
                applyThemeToScene(scene);
            }

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
     * Получить CSS файл для текущей темы
     */
    private String getThemeCss() {
        String cssFile = isDarkTheme() ? "games_dark.css" : "games.css";
        // Правильный путь к файлам в вашей структуре
        String fullPath = "/com/mycompany/javaphone_nir2/css/" + cssFile;

        System.out.println("🔍 Looking for CSS: " + fullPath);

        URL url = getClass().getResource(fullPath);

        if (url == null) {
            // Пробуем альтернативный путь
            fullPath = "com/mycompany/javaphone_nir2/css/" + cssFile;
            url = Thread.currentThread().getContextClassLoader().getResource(fullPath);
            System.out.println("🔍 Alternative path: " + fullPath + " -> " + url);
        }

        if (url == null) {
            System.err.println("❌ CSS not found: " + cssFile);
            // Выводим список доступных ресурсов для отладки
            URL testUrl = getClass().getResource("/com/mycompany/javaphone_nir2/css/");
            System.out.println("🔍 Checking if folder exists: " + testUrl);
        }

        return url != null ? url.toExternalForm() : null;
    }

    public void applyThemeToScene(Scene scene) {
        if (scene == null) {
            return;
        }

        if (!managedScenes.contains(scene)) {
            managedScenes.add(scene);
        }

        String cssUrl = getThemeCss();
        if (cssUrl != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(cssUrl);
            System.out.println("✅ Applied CSS: " + cssUrl);
        } else {
            System.err.println("⚠️ CSS not found! Using fallback styles.");
            applyFallbackStyles(scene);
        }
    }

    private void applyFallbackStyles(Scene scene) {
        String bgColor = isDarkTheme() ? "#1e1e2e" : "#dadef7";
        scene.getRoot().setStyle("-fx-background-color: " + bgColor + ";");
    }

    public void applyToContainer(Region container) {
        container.getStyleClass().add("game-container");
        // Fallback
        if (getThemeCss() == null) {
            String bgColor = isDarkTheme() ? "#1e1e2e" : "#dadef7";
            container.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 20;");
        }
    }

    public void styleButton(Button button) {
        button.getStyleClass().add("game-button");
        // Fallback если CSS не работает
        if (getThemeCss() == null) {
            String bgColor = isDarkTheme() ? "#3541a1" : "#1a225d";
            button.setStyle(String.format(
                    "-fx-font-size: 14px; -fx-padding: 10 20; "
                    + "-fx-background-color: %s; -fx-text-fill: white; "
                    + "-fx-background-radius: 5; -fx-cursor: hand;",
                    bgColor
            ));
        }
    }

    public void styleRejectButton(Button button) {
        button.getStyleClass().add("reject-button");
        if (getThemeCss() == null) {
            button.setStyle(
                    "-fx-font-size: 14px; -fx-padding: 10 20; "
                    + "-fx-background-color: #ef4444; -fx-text-fill: white; "
                    + "-fx-background-radius: 5; -fx-cursor: hand;"
            );
        }
    }

    public void styleText(Text text, boolean isTitle) {
        if (isTitle) {
            text.getStyleClass().add("title-text");
        } else {
            text.getStyleClass().add("body-text");
        }
        if (getThemeCss() == null) {
            String color = isDarkTheme() ? "#ffffff" : "#2c3e50";
            String size = isTitle ? "24px" : "14px";
            String weight = isTitle ? "bold" : "normal";
            text.setStyle(String.format(
                    "-fx-font-size: %s; -fx-font-weight: %s; -fx-fill: %s;",
                    size, weight, color
            ));
        }
    }

    public void styleLabel(Label label, boolean isTitle) {
        if (isTitle) {
            label.getStyleClass().add("title-text");
        } else {
            label.getStyleClass().add("body-text");
        }
    }

    public void styleTicTacToeCell(Button cell, String symbol, boolean isWinning) {
        cell.getStyleClass().clear();
        cell.getStyleClass().add("tic-tac-toe-cell");

        if (isWinning) {
            cell.getStyleClass().add("tic-tac-toe-cell-winning");
        } else if ("X".equals(symbol)) {
            cell.getStyleClass().add("tic-tac-toe-cell-x");
        } else if ("O".equals(symbol)) {
            cell.getStyleClass().add("tic-tac-toe-cell-o");
        } else {
            cell.getStyleClass().add("tic-tac-toe-cell-empty");
        }

        // Fallback если CSS не работает
        if (getThemeCss() == null) {
            String bgColor;
            String textColor = "#ffffff";

            if (isWinning) {
                bgColor = "#f1c40f";
            } else if ("X".equals(symbol)) {
                bgColor = isDarkTheme() ? "#4ecdc4" : "#3498db";
            } else if ("O".equals(symbol)) {
                bgColor = isDarkTheme() ? "#ffe66d" : "#e74c3c";
                if (isDarkTheme()) {
                    textColor = "#1e1e2e";
                }
            } else {
                bgColor = isDarkTheme() ? "#2d2d3d" : "#ffffff";
                textColor = isDarkTheme() ? "#ffffff" : "#2c3e50";
            }

            String borderColor = isDarkTheme() ? "#6c6c7c" : "#bdc3c7";

            cell.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; "
                    + "-fx-font-size: 48px; -fx-font-weight: bold; "
                    + "-fx-border-color: %s; -fx-border-width: 2; "
                    + "-fx-border-radius: 12; -fx-background-radius: 12; "
                    + "-fx-cursor: %s;",
                    bgColor, textColor, borderColor,
                    symbol.equals(" ") ? "hand" : "default"
            ));
        }
    }

    public void unregisterScene(Scene scene) {
        managedScenes.remove(scene);
    }
}
