package de.tbecke.components.cards.utils;

public enum Mode {
    BLINK( (byte) ( 32 ) ),
    HIGH_RES_GRAPHIC( (byte) ( 16 ) ),
    ENABLE_VIDEO_OUTPUT( (byte) ( 8 ) ),
    GRAPHIC_MODE( (byte) ( 2 ) ),
    HIGH_RES( (byte) ( 1 ) );

    public final byte value;

    Mode( byte m ) {
        this.value = m;
    }

}
