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

/**
 * Pane for camera showing
 * implements VideoTrackSink to render frames that came via WebRTC
 *
 * Responsible for: 1. Show camera 2. Render (draw) frames
 * 3. Apply camera filter
 */
public class WebRtcVideoPanel extends Pane implements VideoTrackSink {

    private final Canvas canvas = new Canvas();

    private PixelBuffer<IntBuffer> pixelBuffer;
    private WritableImage videoImage;

    private ByteBuffer argbNativeBuffer;
    private IntBuffer fxArgbBuffer;

    private int frameWidth = -1;
    private int frameHeight = -1;
    // 0 = without filter, 100 = maximum filter value (yellow color)
    private final IntegerProperty warmthLevel = new SimpleIntegerProperty(this, "warmthLevel", 0);
    public final int getWarmthLevel() { return warmthLevel.get(); }
    public final void setWarmthLevel(int value) { warmthLevel.set(value); }

    /** Property for warmth level (filter value) */
    public IntegerProperty warmthLevelProperty() { return warmthLevel; }

    // flag frame in processing
    private final AtomicBoolean frameInQueue = new AtomicBoolean(false);

    /** Ctor that adds canvas into pane and init listeners for size */
    public WebRtcVideoPanel() {
        getChildren().add(canvas);

        widthProperty().addListener((obs, oldVal, newVal) -> requestLayout());
        heightProperty().addListener((obs, oldVal, newVal) -> requestLayout());
    }

    /** Method responsible for redraw children with actual size */
    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();

        canvas.setWidth(Math.max(0, w));
        canvas.setHeight(Math.max(0, h));

        redraw();
    }

    /**
     * Method responsible for dram videoFrame that came from WebRTC
     * @param vf video frame to draw
     */
    @Override
    public void onVideoFrame(VideoFrame vf) {
        if (vf == null || vf.buffer == null) {
            return;
        }

        // if previous frame not processed - do not process new frame
        // to avoid creating a queue
        if (!frameInQueue.compareAndSet(false, true)) {
            try {
                vf.release();
            } catch (Throwable ignore) {
                try {
                    vf.buffer.release();
                } catch (Throwable ignored2) {}
            }
            return;
        }

        // retain frame
        try {
            vf.retain();
        } catch (Throwable ignore) {}

        Platform.runLater(() -> {
            try {
                VideoFrameBuffer src = vf.buffer;
                int width = src.getWidth();
                int height = src.getHeight();

                ensureBuffers(width, height);

                int expectedBytes = width * height * 4;

                argbNativeBuffer.clear();

                // convert I420 to ARGB (native)
                VideoBufferConverter.convertFromI420(src, argbNativeBuffer, FourCC.ARGB);

                argbNativeBuffer.position(0);
                argbNativeBuffer.limit(expectedBytes);

                // convert into IntBuffer
                convertNativeArgbToFxArgb(argbNativeBuffer, fxArgbBuffer, width, height);

                pixelBuffer.updateBuffer(pb -> null);
                redraw();

                src.release();

            } catch (Exception e) {
                System.err.println("Video frame conversion error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    vf.release(); //release frame that we draw
                } catch (Throwable ignore) {
                    try {
                        vf.buffer.release();
                    } catch (Throwable ignored2) {}
                }
                frameInQueue.set(false);
            }
        });
    }

    /** This method responsible for ensure buffers with necessary size */
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
     * libyuv/WebRTC for FOURCC_ARGB on little-endian have bytes in heap like:
     * [B, G, R, A]
     *
     * JavaFX IntArgbPre waiting int like:
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

        int warmth = Math.max(0, Math.min(100, getWarmthLevel()));

        // warmth influence coefficients
        float redBoost   = 0.3f * warmth / 100f;   // +30% on red
        float greenBoost = 0.15f * warmth / 100f;  // +15% on green
        float blueCut    = 0.4f * warmth / 100f;   // −40% on blue

        for (int i = 0; i < pixels; i++) {
            int b = src.get() & 0xFF;
            int g = src.get() & 0xFF;
            int r = src.get() & 0xFF;
            int a = src.get() & 0xFF;

            // apply filter
            int rAdj = clamp255((int) (r * (1.0f + redBoost)));
            int gAdj = clamp255((int) (g * (1.0f + greenBoost)));
            int bAdj = clamp255((int) (b * (1.0f - blueCut)));

            int argb = (a << 24) | (rAdj << 16) | (gAdj << 8) | bAdj;
            dst.put(argb);
        }

        dst.flip();
    }

    /** This method responsible for forced limitation of a pixel
     * value (or color component) to the range from 0 to 255 */
    private static int clamp255(int v) {
        return (v < 0) ? 0 : (v > 255 ? 255 : v);
    }

    /** This method responsible for redraw canvas */
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

    /** This method responsible for return default pref width */
    @Override
    protected double computePrefWidth(double height) {
        return 640;
    }

    /** This method responsible for return default pref height */
    @Override
    protected double computePrefHeight(double width) {
        return 480;
    }
}