package com.carepath.extraction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Crops a single value cell out of a page image and prepares it for a focused OCR pass:
 * upscale (bounded), grayscale, optional Otsu threshold, white margin.
 * Only the small crop is copied, so memory use stays bounded regardless of page size.
 */
public final class CellImagePreparer {

    /** Hard cap on the prepared image size, to protect memory on small hosts. */
    public static final int MAX_PIXELS = 1_500_000;
    private static final int CROP_GROW = 3;   // include a few pixels around the box (decimal points sit low)
    private static final int MARGIN = 16;     // white border Tesseract likes

    public BufferedImage prepare(BufferedImage page, Rectangle cell, double scale, boolean binarize) {
        Rectangle bounds = new Rectangle(0, 0, page.getWidth(), page.getHeight());
        Rectangle r = new Rectangle(cell);
        r.grow(CROP_GROW, CROP_GROW);
        r = r.intersection(bounds);
        if (r.isEmpty()) {
            throw new IllegalArgumentException("Cell box is outside the page: " + cell);
        }

        double s = scale;
        while ((long) Math.ceil(r.width * s) * (long) Math.ceil(r.height * s) > MAX_PIXELS) {
            s *= 0.9;
        }
        int w = Math.max(1, (int) Math.round(r.width * s));
        int h = Math.max(1, (int) Math.round(r.height * s));

        BufferedImage rgb = new BufferedImage(w + 2 * MARGIN, h + 2 * MARGIN, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(page.getSubimage(r.x, r.y, r.width, r.height), MARGIN, MARGIN, w, h, null);
        } finally {
            g.dispose();
        }

        BufferedImage gray = toGray(rgb);
        byte[] px = ((DataBufferByte) gray.getRaster().getDataBuffer()).getData();
        if (mean(px) < 100) invert(px);          // light text on dark background
        if (binarize) threshold(px, otsu(px));
        return gray;
    }

    private static BufferedImage toGray(BufferedImage rgb) {
        int w = rgb.getWidth(), h = rgb.getHeight();
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        byte[] out = ((DataBufferByte) gray.getRaster().getDataBuffer()).getData();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            rgb.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int p = row[x];
                int lum = (int) (0.299 * ((p >> 16) & 0xFF) + 0.587 * ((p >> 8) & 0xFF) + 0.114 * (p & 0xFF));
                out[y * w + x] = (byte) lum;
            }
        }
        return gray;
    }

    private static double mean(byte[] px) {
        long sum = 0;
        for (byte b : px) sum += b & 0xFF;
        return px.length == 0 ? 255 : (double) sum / px.length;
    }

    private static void invert(byte[] px) {
        for (int i = 0; i < px.length; i++) px[i] = (byte) (255 - (px[i] & 0xFF));
    }

    private static int otsu(byte[] px) {
        int[] hist = new int[256];
        for (byte b : px) hist[b & 0xFF]++;
        long total = px.length, sumAll = 0;
        for (int i = 0; i < 256; i++) sumAll += (long) i * hist[i];
        long sumB = 0, wB = 0;
        double best = -1;
        int threshold = 128;
        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0) continue;
            long wF = total - wB;
            if (wF == 0) break;
            sumB += (long) t * hist[t];
            double mB = (double) sumB / wB, mF = (double) (sumAll - sumB) / wF;
            double between = (double) wB * wF * (mB - mF) * (mB - mF);
            if (between > best) { best = between; threshold = t; }
        }
        return threshold;
    }

    private static void threshold(byte[] px, int t) {
        for (int i = 0; i < px.length; i++) px[i] = (byte) ((px[i] & 0xFF) > t ? 255 : 0);
    }
}
