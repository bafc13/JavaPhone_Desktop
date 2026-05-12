package com.mycompany.javaphone_nir2.media;

import java.nio.file.Files;
import java.nio.file.Path;

public class MediaUtils {

    public static String extractFileName(String filePath) {
        if (filePath == null) return "Unknown";
        return Path.of(filePath).getFileName().toString();
    }

    public static long extractFileSize(String filePath) {
        if (filePath == null) return 0;
        try {
            return Files.size(Path.of(filePath));
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isImageFile(String filePath) {
        if (filePath == null) return false;
        String name = filePath.toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
               name.endsWith(".gif") || name.endsWith(".webp") || name.endsWith(".bmp");
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        if (bytes < 1048576) return String.format("%.1f КБ", bytes / 1024.0);
        if (bytes < 1073741824) return String.format("%.1f МБ", bytes / 1048576.0);
        return String.format("%.1f ГБ", bytes / 1073741824.0);
    }
}
