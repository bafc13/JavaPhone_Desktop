package com.mycompany.javaphone_nir2;

import dev.onvoid.webrtc.media.FourCC;
import dev.onvoid.webrtc.media.video.VideoBufferConverter;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoFrameBuffer;
import dev.onvoid.webrtc.media.video.VideoTrackSink;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class WebRtcVideoPanel extends Pane implements VideoTrackSink {

    private final Canvas canvas = new Canvas();

    private PixelBuffer<IntBuffer> pixelBuffer;
    private WritableImage videoImage;

    private ByteBuffer argbNativeBuffer;
    private IntBuffer fxArgbBuffer;

    private int frameWidth = -1;
    private int frameHeight = -1;

        // 0 = без фильтра, 100 = максимум «желтизны»
    private final IntegerProperty warmthLevel = new SimpleIntegerProperty(this, "warmthLevel", 0);
    public final int getWarmthLevel() { return warmthLevel.get(); }
    public final void setWarmthLevel(int value) { warmthLevel.set(value); }
    public IntegerProperty warmthLevelProperty() { return warmthLevel; }

    // Чтобы не забивать FX-очередь, держим флаг «кадр в обработке»
    private final AtomicBoolean frameInQueue = new AtomicBoolean(false);


    public WebRtcVideoPanel() {
        getChildren().add(canvas);

        widthProperty().addListener((obs, oldVal, newVal) -> requestLayout());
        heightProperty().addListener((obs, oldVal, newVal) -> requestLayout());
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();

        canvas.setWidth(Math.max(0, w));
        canvas.setHeight(Math.max(0, h));

        redraw();
    }

    @Override
    public void onVideoFrame(VideoFrame vf) {
        if (vf == null || vf.buffer == null) {
            return;
        }

        // Если предыдущий кадр ещё не отрисован — этот просто дропаем.
        // Так ты не накопишь тысячи Runnable в FX-очереди.
        if (!frameInQueue.compareAndSet(false, true)) {
            // Мы НЕ будем использовать этот кадр → обязательно освободить.
            try {
                // Если у VideoFrame есть release() — вызываем его.
                vf.release();
            } catch (Throwable ignore) {
                // В некоторых версиях webrtc-java release может быть только у buffer.
                try {
                    vf.buffer.release();
                } catch (Throwable ignored2) {
                    // ничего страшного, просто продолжаем
                }
            }
            return;
        }

        // Мы собираемся использовать кадр позже на FX-потоке.
        // У WebRTC-обёрток есть ref-count → держим свою ссылку.
        try {
            vf.retain();
        } catch (Throwable ignore) {
            // Если retain() нет (редкий случай), просто надеемся, что библиотека
            // держит кадр живым на время вызова sink'а.
        }

        Platform.runLater(() -> {
            try {
                VideoFrameBuffer src = vf.buffer;
                int width = src.getWidth();
                int height = src.getHeight();

                ensureBuffers(width, height);

                int expectedBytes = width * height * 4;

                argbNativeBuffer.clear();

                // Конвертация I420 → ARGB (native)
                VideoBufferConverter.convertFromI420(src, argbNativeBuffer, FourCC.ARGB);

                argbNativeBuffer.position(0);
                argbNativeBuffer.limit(expectedBytes);

                // Перегоняем в IntBuffer для JavaFX
                convertNativeArgbToFxArgb(argbNativeBuffer, fxArgbBuffer, width, height);

                // Дёргаем PixelBuffer, чтобы JavaFX перерисовал Image
                pixelBuffer.updateBuffer(pb -> null);
                redraw();

                src.release();

            } catch (Exception e) {
                System.err.println("Video frame conversion error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Обязательно отпускаем ref на кадр, который мы retain'или
                try {
                    vf.release();
                } catch (Throwable ignore) {
                    try {
                        vf.buffer.release();
                    } catch (Throwable ignored2) {
                        // если и это не поддерживается — значит библиотека сама менеджит память
                    }
                }
                frameInQueue.set(false);
            }
        });
    }

    private void ensureBuffers(int width, int height) {
        int pixelCount = width * height;
        int bytes = pixelCount * 4;

        boolean recreate =
                pixelBuffer == null ||
                videoImage == null ||
                argbNativeBuffer == null ||
                fxArgbBuffer == null ||
                frameWidth != width ||
                frameHeight != height ||
                argbNativeBuffer.capacity() < bytes ||
                fxArgbBuffer.capacity() < pixelCount;

        if (!recreate) return;

        frameWidth = width;
        frameHeight = height;

        argbNativeBuffer = ByteBuffer.allocateDirect(bytes);
        fxArgbBuffer = IntBuffer.allocate(pixelCount);

        pixelBuffer = new PixelBuffer<>(
                width,
                height,
                fxArgbBuffer,
                PixelFormat.getIntArgbPreInstance()
        );

        videoImage = new WritableImage(pixelBuffer);
    }

    /**
     * libyuv/WebRTC для FOURCC_ARGB на little-endian хранит байты в памяти как:
     * [B, G, R, A]
     *
     * JavaFX IntArgbPre ожидает int вида:
     * 0xAARRGGBB
     */
    private void convertNativeArgbToFxArgb(ByteBuffer src, IntBuffer dst, int width, int height) {
        int pixels = width * height;

        if (src.capacity() < pixels * 4) {
            throw new IllegalArgumentException(
                    "Native ARGB buffer too small: capacity=" + src.capacity() + ", required=" + (pixels * 4)
            );
        }

        if (dst.capacity() < pixels) {
            throw new IllegalArgumentException(
                    "FX ARGB buffer too small: capacity=" + dst.capacity() + ", required=" + pixels
            );
        }

        src.position(0);
        dst.clear();

        // Локальная копия уровня тёплоты (0..100). Можно ограничить, чтобы не улететь.
        int warmth = Math.max(0, Math.min(100, getWarmthLevel()));

        // Коэффициенты влияния (подбираются «на глаз»)
        float redBoost   = 0.3f * warmth / 100f;   // до +30% к красному
        float greenBoost = 0.15f * warmth / 100f;  // до +15% к зелёному
        float blueCut    = 0.4f * warmth / 100f;   // до −40% синего

        for (int i = 0; i < pixels; i++) {
            int b = src.get() & 0xFF;
            int g = src.get() & 0xFF;
            int r = src.get() & 0xFF;
            int a = src.get() & 0xFF;

            // Применяем «тёплый» фильтр
            int rAdj = clamp255((int) (r * (1.0f + redBoost)));
            int gAdj = clamp255((int) (g * (1.0f + greenBoost)));
            int bAdj = clamp255((int) (b * (1.0f - blueCut)));

            int argb = (a << 24) | (rAdj << 16) | (gAdj << 8) | bAdj;
            dst.put(argb);
        }

        dst.flip();
    }

    private static int clamp255(int v) {
        return (v < 0) ? 0 : (v > 255 ? 255 : v);
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double panelW = canvas.getWidth();
        double panelH = canvas.getHeight();

        gc.setFill(Color.BLACK);
        gc.clearRect(0, 0, panelW, panelH);
        gc.fillRect(0, 0, panelW, panelH);

        if (videoImage == null || frameWidth <= 0 || frameHeight <= 0 || panelW <= 0 || panelH <= 0) {
            return;
        }

        double videoAspect = (double) frameWidth / frameHeight;
        double panelAspect = panelW / panelH;

        double drawW;
        double drawH;
        double drawX;
        double drawY;

        if (panelAspect > videoAspect) {
            drawW = panelW;
            drawH = drawW / videoAspect;
            drawX = 0;
            drawY = (panelH - drawH) / 2.0;
        } else {
            drawH = panelH;
            drawW = drawH * videoAspect;
            drawX = (panelW - drawW) / 2.0;
            drawY = 0;
        }

        gc.drawImage(videoImage, drawX, drawY, drawW, drawH);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 640;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 480;
    }
}