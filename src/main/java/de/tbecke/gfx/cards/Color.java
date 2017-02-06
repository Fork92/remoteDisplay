package de.tbecke.gfx.cards;

public enum Color {

    BLACK( 0, 0x000000 ),
    BLUE( 1, 0x0000AA ),
    GREEN( 2, 0x00AA00 ),
    CYAN( 3, 0x00AAAA ),
    RED( 4, 0xAA0000 ),
    MAGENTA( 5, 0xAA00AA ),
    BROWN( 6, 0xAA5500 ),
    LIGHTGRAY( 7, 0xAAAAAA ),
    GRAY( 8, 0x555555 ),
    LIGHTBLUE( 9, 0x5555FF ),
    LIGHTGREEN( 10, 0x55FF55 ),
    LIGHTCYAN( 11, 0x55FFFF ),
    LIGHTRED( 12, 0xFF5555 ),
    LIGHTMAGENTA( 13, 0xFF55FF ),
    YELLOW( 14, 0xFFFF55 ),
    WHITE( 15, 0xFFFFFF );

    private int id;
    private int value;

    Color( int id, int value ) {
        this.id = id;
        this.value = value;
    }

    public static Color getColorByID( int id ) {
        for( Color color : values() ) {
            if( color.id == id )
                return color;
        }

        return null;
    }

    public static int getValueByID( int id ) {
        for( Color color : values() ) {
            if( color.id == id )
                return color.value;
        }

        return 0;
    }

    public int getValue() {
        return value;
    }

}
