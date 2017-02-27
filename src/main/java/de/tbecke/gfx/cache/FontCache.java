package de.tbecke.gfx.cache;

import java.awt.image.BufferedImage;

public class FontCache {

    private final int width;
    private final int[] pixels;

    public FontCache( BufferedImage image) {
        width = image.getWidth();
        int height = image.getHeight();
        pixels = image.getRGB( 0, 0, width, height, null, 0, width );
        for(int i = 0; i < pixels.length; i++) {
            pixels[i] = (pixels[i] & 0xff) / 64;
        }
    }

    public int getWidth() {
        return width;
    }

    public int[] getPixels() {
        return pixels;
    }

}
