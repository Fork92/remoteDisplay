package de.tbecke.gfx.cards;

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

    public static Mode getByValue( byte c ) {
        for( Mode mode : values() ) {
            if( mode.value == c ) {
                return mode;
            }
        }
        return null;
    }

}
