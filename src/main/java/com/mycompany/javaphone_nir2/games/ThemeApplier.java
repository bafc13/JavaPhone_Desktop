/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphone_nir2.games;

//import com.mycompany.javaphone_nir2.settings.Settings;
import com.mycompany.javaphone_nir2.models.SettingsManager;
import javafx.application.Platform;
import javafx.scene.Scene;

public class ThemeApplier {
    
    private Scene scene;
    private final SettingsManager Settings = SettingsManager.getInstance();
    /**
     * Применяет тему к сцене (как в комментарии коллеги)
     */
    public void applyThemeToScene(Scene scene) {
        this.scene = scene;
        
        // Получаем текущую тему из настроек
        
        
        String currentTheme = Settings.getInstance().getTheme();
        Platform.runLater(()->{
            
        
        // Очищаем старые стили
        scene.getStylesheets().removeIf(s -> 
            s.contains("chat_main.css") || s.contains("chat_main_dark.css")
        );
        
        // Добавляем нужный CSS файл
        String cssFile = "dark".equals(currentTheme) ? 
            "/com/mycompany/javaphone_nir2/css/chat_main_dark.css" : "/com/mycompany/javaphone_nir2/css/chat_main.css";
        
        try {
            scene.getStylesheets().add(
                getClass().getResource(cssFile).toExternalForm()
            );
            System.out.println("🎨 Theme applied: " + currentTheme);
        } catch (Exception e) {
            System.err.println("❌ Failed to load theme CSS: " + cssFile);
        }
        
        // Добавляем специфичные для игр стили (опционально)
        try {
            scene.getStylesheets().add(
                getClass().getResource("/com/mycompany/javaphone_nir2/css/game_styles.css").toExternalForm()
            );
        } catch (Exception e) {
            // game_styles.css опционален
        }
        });
        // Подписываемся на изменения темы
        subscribeToThemeChanges();
    }
    
    /**
     * Слушаем изменения темы (как в комментарии коллеги)
     */
    private void subscribeToThemeChanges() {
        Settings.getInstance().themeProperty().addListener((obs, oldTheme, newTheme) -> {
            Platform.runLater(()->{
            if (scene != null) {
                scene.getStylesheets().removeIf(s -> 
                    s.contains("chat_main.css") || s.contains("chat_main_dark.css")
                );
                
                String cssFile = "dark".equals(newTheme) ? 
                    "/com/mycompany/javaphone_nir2/css/chat_main_dark.css" : "/com/mycompany/javaphone_nir2/css/chat_main.css";
                
                scene.getStylesheets().add(
                    getClass().getResource(cssFile).toExternalForm()
                );
                
                System.out.println("🎨 Game theme updated to: " + newTheme);
            }});
        });
    }
}