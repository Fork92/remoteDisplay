package de.tbecke.gfx.cards;

import de.tbecke.gfx.cache.FontCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

/**
 * Created by tbecke on 26.01.17.
 */
public class CGA extends GraphicsCard {

    private static final Logger LOGGER = LogManager.getLogger( CGA.class );

//    MODE Register Flags
    public static final byte MODE_BLINK                 = 1 << 5;
    public static final byte MODE_HIGH_RES_GRAPHIC      = 1 << 4;
    public static final byte MODE_ENABLE_VIDEOOUTPUT    = 1 << 3;
    public static final byte MODE_BLACK_WHITE           = 1 << 2;
    public static final byte MODE_GRAPHIC_MODE          = 1 << 1;
    public static final byte MODE_HIGH_RES              = 1 << 0;

    private char MODE_REGISTER;

    private char[] RAM = new char[16*1024];
    private FontCache font;

    private int width = 640;
    private int height = 200;

    private int lastBlink;
    private boolean blink;

    public CGA() {
        try {
            this.font = new FontCache( ImageIO.read( this.getClass().getResourceAsStream( "CGA.png" ) ) );
        } catch( IOException e ) {
            LOGGER.error( e );
        }

        setMode( MODE_ENABLE_VIDEOOUTPUT );
        this.framebuffer = new int[width*height];
    }

    private void renderText() {
        if( isModeSet(MODE_ENABLE_VIDEOOUTPUT) ) {

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

                            if( isAttrSet( BLINK, attr ) && isBlinkEnabled() && blink) {
                                col = Color.BLACK.getValue();
                            }

                            this.framebuffer[(x*9+fx)+((y*14+fy)*this.getWidth())] = col;
                        }
                    }

                }

            }
        }

    }

    private boolean isModeSet(int mode) {
        return (MODE_REGISTER & mode) > 0;
    }

    @Override
    public void render() {
        if( isModeSet( MODE_GRAPHIC_MODE ) ) {

        } else {
            renderText();
        }
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
    public void setMode( byte mode ) {
        this.MODE_REGISTER |= mode;
    }

    @Override
    public void setRAM( int addr, char value, char flag ) {

    }

    @Override
    public void setRAM( int addr, String msg, char flag ) {

    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getMaxRam() {
        return RAM.length;
    }
}
