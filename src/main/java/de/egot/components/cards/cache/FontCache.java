package de.egot.components.cards.cache;

import java.awt.image.BufferedImage;

public class FontCache {

    private final int width;
    private final int[] pixels;
    private final int charH;
    private final int charW;
    private final int xNum;

    public FontCache( BufferedImage image, int charW, int charH, int xNum ) {
        width = image.getWidth();
        int height = image.getHeight();
        this.charH = charH;
        this.charW = charW;
        this.xNum = xNum;
        pixels = image.getRGB( 0, 0, width, height, null, 0, width );
        for(int i = 0; i < pixels.length; i++) {
            pixels[i] = (pixels[i] & 0xff) / 64;
        }
    }

    public int getCharWidth() {
        return charW;
    }

    public int[] getPixels( int charCode ) {

        int startx = charCode % xNum * charW;
        int starty = charCode / xNum * charH;
        int[] ret = new int[charH * charW];

        for( int i = 0; i < charH * charW; i++ ) {
            int fx = i % charW;
            int fy = i / charW;
            ret[i] = pixels[( startx + fx ) + ( ( starty + fy ) * getWidth() )];
        }

        return ret;
    }

    public int getWidth() {
        return width;
    }

}
