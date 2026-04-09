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

public class WebRtcVideoPanel extends Pane implements VideoTrackSink {

    private final Canvas canvas = new Canvas();

    private PixelBuffer<IntBuffer> pixelBuffer;
    private WritableImage videoImage;

    private ByteBuffer argbNativeBuffer;
    private IntBuffer fxArgbBuffer;

    private int frameWidth = -1;
    private int frameHeight = -1;

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
        Platform.runLater(() -> {
            if (vf == null || vf.buffer == null) return;

            VideoFrameBuffer src = vf.buffer;
            int width = src.getWidth();
            int height = src.getHeight();

            ensureBuffers(width, height);

            try {
                int expectedBytes = width * height * 4;

                argbNativeBuffer.clear();

                // ВАЖНО: используем ARGB, а не RGBA.
                VideoBufferConverter.convertFromI420(src, argbNativeBuffer, FourCC.ARGB);

                // Не используем flip() после native-вызова.
                argbNativeBuffer.position(0);
                argbNativeBuffer.limit(expectedBytes);

                convertNativeArgbToFxArgb(argbNativeBuffer, fxArgbBuffer, width, height);

                pixelBuffer.updateBuffer(pb -> null);
                redraw();

            } catch (Exception e) {
                System.err.println("Video frame conversion error: " + e.getMessage());
                e.printStackTrace();
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

        for (int i = 0; i < pixels; i++) {
            int b = src.get() & 0xFF;
            int g = src.get() & 0xFF;
            int r = src.get() & 0xFF;
            int a = src.get() & 0xFF;

            int argb = (a << 24) | (r << 16) | (g << 8) | b;
            dst.put(argb);
        }

        dst.flip();
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