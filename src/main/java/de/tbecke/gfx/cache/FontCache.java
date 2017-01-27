package de.tbecke.gfx.cache;

import java.awt.image.BufferedImage;

/**
 * Created by tbecke on 24.01.17.
 */
public class FontCache {

    public int width, height;
    public int[] pixels;

    public FontCache( BufferedImage image) {
        width = image.getWidth();
        height = image.getHeight();
        pixels = image.getRGB( 0, 0, width, height, null, 0, width );
        for(int i = 0; i < pixels.length; i++) {
            pixels[i] = (pixels[i] & 0xff) / 64;
        }
    }
}
