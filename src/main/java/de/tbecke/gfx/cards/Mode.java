package de.tbecke.gfx.cards;

/**
 * Created by tbecke on 27.01.17.
 */
public enum Mode {
    BLINK( (char) ( 1 << 5 ) ),
    HIGH_RES_GRAPHIC( (char) ( 1 << 4 ) ),
    ENABLE_VIDEO_OUTPUT( (char) ( 1 << 3 ) ),
    BLACK_WHITE( (char) ( 1 << 2 ) ),
    GRAPHIC_MODE( (char) ( 1 << 1 ) ),
    HIGH_RES( (char) ( 1 << 0 ) );

    public final char value;

    Mode( char m ) {
        this.value = m;
    }

}
