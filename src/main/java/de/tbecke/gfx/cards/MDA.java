package de.tbecke.gfx.cards;

import de.tbecke.gfx.cache.FontCache;

import javax.imageio.ImageIO;
import java.io.IOException;

/**
 * Created by tbecke on 23.01.17.
 */
public class MDA extends GraphicsCard {

//  Const Variablen für das MODE_REGISTER
    private final byte MODE_BLINK                 = 1 << 5;
    private final byte MODE_OUTPUT_ENABLED        = 1 << 3;

//    Const Variablen für die Attribute
    public final static char UNDERLINE            = 1 << 1;
    public final static char DEFAUL               = 1 << 2;
    public final static char HIGHSENSITY          = 1 << 3;
    public final static char BLINK                = 1 << 7;
//     Statusregister
    private byte MODE_REGISTER;
    private char[] RAM = new char[4096];
    private FontCache font;
    private final int width = 720;
    private final int height = 350;


//    TODO: in eigene Datei verschieben.
    private enum Color {
        GREEN(0x00ff00),
        DARK_GREEN(0x006500),
        BLACK(0x000000);

        private final int code;
        Color(int c) {code = c;}
        public int getValue(){return code;}
    }

//    Hilfsvariablen für blinkende Text
    private long lastBlink;
    private boolean blink = false;

    public MDA() {
        try {
            this.font = new FontCache( ImageIO.read(
                this.getClass().getResourceAsStream( "/MDA.png" )));
        } catch( IOException e ) {
            e.printStackTrace();
        }
        this.setMode( MODE_OUTPUT_ENABLED );

        RAM[0] = 'A';
        RAM[1] = 0x89;
        lastBlink =0;

        this.framebuffer = new int[this.getWidth() * this.getHeight()];
    }



    @Override
    public void tick() {
        lastBlink++;
        if(lastBlink >= 30) {
            blink = !blink;
            lastBlink = 0;
        }
    }

    @Override
    public void render( ) {
        if( isModeSet(MODE_OUTPUT_ENABLED) ) {

            // read mem
            for(int y = 0; y < 25; y++) {
                for(int x = 0; x < 80; x++) {
                    int c = RAM[(x + y * 80) * 2];
                    int attr = RAM[(x + y * 80) * 2 + 1];
                    int charX = c % 32 * 9;
                    int charY = c / 32 * 14;

                    for(int fy = 0; fy < 14; fy++) {
                        for(int fx = 0; fx < 9; fx++) {
                            int col = getColor( font.pixels[(charX+fx) + ((charY+fy) * font.width)], attr ).getValue();

                           if ( isAttrSet( (UNDERLINE), attr ) && fy == 12) {
                                col = Color.DARK_GREEN.getValue();
                                if( isAttrSet( HIGHSENSITY, attr )) {
                                    col = Color.GREEN.getValue();
                                }
                            }

                            if( isAttrSet( BLINK, attr ) && isModeSet(MODE_BLINK) && blink) {
                                col = Color.BLACK.getValue();
                            }

                            this.framebuffer[(x*9+fx)+((y*14+fy)*this.getWidth())] = col;
                        }
                    }

                }

            }
        }

    }

    private boolean isAttrSet(int val, int attr) {
        return (attr & val) > 0;
    }

    private Color getColor( int d, int attr) {
        if( attr == 0x00 || attr == 0x08 || attr == 0x80 || attr == 0x88) {
            return Color.BLACK;
        }

        if(attr == 0x78 || attr == 0xf8) {
            if(d == 0) return Color.DARK_GREEN;
            else return Color.GREEN;
        }

        if(attr == 0xf0 || attr == 0x70) {
            if(d == 0) return Color.BLACK;
            else return Color.GREEN;
        }

        if((attr & 0x08) > 0) {
            if( d == 0 ) return Color.GREEN;
            else return Color.BLACK;
        }


        if(d == 0) return Color.DARK_GREEN;
        else return Color.BLACK;
    }

    public int getMaxRam() {
        return RAM.length;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight(){
        return height;
    }

    public void setMode (byte s) {
        this.MODE_REGISTER = s;
    }

    public void setRAM(int addr, char value, char flag) {

        RAM[addr] = value;
    }

    public void setRAM(int addr, String msg, char flag) {

        for(char c : msg.toCharArray()) {
            RAM[addr++] = c;
            RAM[addr++] = flag;
        }
    }

    public boolean isModeSet(byte mode) {
        return (MODE_REGISTER & mode) > 0;
    }

}
