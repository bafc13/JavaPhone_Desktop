package com.mycompany.javaphone_nir2;

import dev.onvoid.webrtc.media.FourCC;
import dev.onvoid.webrtc.media.video.VideoBufferConverter;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoTrackSink;
import java.nio.ByteBuffer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import java.nio.IntBuffer;

/**
 *
 * @author bafc13
 * Кастомный JavaFX-компонент для отображения WebRTC-видеопотока.
 * Реализует VideoTrackSink для прямого приёма кадров.
 */
public class WebRtcVideoPanel extends Region implements VideoTrackSink {

    private final Canvas canvas;
//    private PixelBuffer<Byte> pixelBuffer;
    private PixelBuffer pixelBuffer;
    private WritableImage videoImage;

    private ByteBuffer tempConversionBuffer;

    // Формат пикселей (ARGB 32-bit)
    private static final PixelFormat<IntBuffer> PIXEL_FORMAT = PixelFormat.getIntArgbPreInstance();

    public WebRtcVideoPanel() {
        canvas = new Canvas();
        getChildren().add(canvas);

        // Реактивное изменение размера canvas при ресайзе компонента
        widthProperty().addListener((obs, oldW, newW) -> resizeCanvas());
        heightProperty().addListener((obs, oldH, newH) -> resizeCanvas());

        // Инициализация буфера
        resizeCanvas();
    }

    private void resizeCanvas() {
        int width = (int) Math.max(1, getWidth());
        int height = (int) Math.max(1, getHeight());

        canvas.setWidth(width);
        canvas.setHeight(height);

        // Создаём массив под ARGB-пиксели
        int[] pixelArray = new int[width * height];

        pixelBuffer = new PixelBuffer<>(width, height, java.nio.IntBuffer.wrap(pixelArray), PIXEL_FORMAT);
        videoImage = new WritableImage(pixelBuffer);
    }

    @Override
    protected void layoutChildren() {
        // Canvas занимает всю область компонента
        canvas.resizeRelocate(0, 0, getWidth(), getHeight());
    }

    @Override
    protected double computePrefWidth(double height) {
        return getPrefWidth() == -1 ? 640 : getPrefWidth();
    }

    @Override
    protected double computePrefHeight(double width) {
        return getPrefHeight() == -1 ? 480 : getPrefHeight();
    }

    // Очистка ресурсов при уничтожении компонента
    public void dispose() {
        pixelBuffer = null;
        videoImage = null;
    }

    @Override
    public void onVideoFrame(VideoFrame vf) {
        Platform.runLater(() -> {
            if (videoImage == null || pixelBuffer == null) return;

            int width = (int) canvas.getWidth();
            int height = (int) canvas.getHeight();

            if (tempConversionBuffer == null || tempConversionBuffer.capacity() != width * height * 4) {
                tempConversionBuffer = ByteBuffer.allocateDirect(width * height * 4);
            }

            try {
                // 🔥 Конвертация I420 → RGBA через WebRTC-утилиту
                // Сигнатура может отличаться, проверь документацию dev.onvoid.webrtc
                VideoBufferConverter.convertFromI420(vf.buffer, tempConversionBuffer, FourCC.RGBA);

                tempConversionBuffer.rewind();

                // 🔥 Копирование из ByteBuffer в IntBuffer PixelBuffer
                copyRgbaToArgbBuffer(tempConversionBuffer, (IntBuffer) pixelBuffer.getBuffer(), width, height);

                // Отрисовка
                canvas.getGraphicsContext2D().clearRect(0, 0, width, height);
                canvas.getGraphicsContext2D().drawImage(videoImage, 0, 0, width, height);

            } catch (Exception e) {
                System.err.println("Video frame conversion error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

     /**
     * Копирует данные из RGBA-ByteBuffer в ARGB-IntBuffer.
     * Конвертирует формат: [R,G,B,A] байты → 0xAARRGGBB int.
     */
    private void copyRgbaToArgbBuffer(ByteBuffer src, IntBuffer dst, int width, int height) {
        src.rewind(); // Сбрасываем позицию в начало буфера

        for (int i = 0; i < width * height; i++) {
            // Читаем 4 байта в порядке RGBA
            int r = src.get() & 0xFF;
            int g = src.get() & 0xFF;
            int b = src.get() & 0xFF;
            int a = src.get() & 0xFF;

            // Упаковываем в int в формате ARGB (JavaFX ожидает premultiplied alpha или обычный)
            int argb = (a << 24) | (r << 16) | (g << 8) | b;
            dst.put(i, argb);
        }
    }
}