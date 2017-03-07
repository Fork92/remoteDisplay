package de.egot.components.cards;

import de.egot.components.RAM;
import de.egot.components.cards.cache.FontCache;
import de.egot.utils.Color;
import de.egot.utils.Mode;
import de.egot.utils.OutOfRamException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.IOException;

public class MDA implements GraphicCard {
    //    Const Variablen für die Attribute
    private static final char UNDERLINE = 1 << 1;
    private static final char HIGHSENSITY = 1 << 3;
    private static final char BLINK = 1 << 7;

    private static final Logger LOGGER = LogManager.getLogger( MDA.class );
    private static final int MODE_REGISTER = 0x03B8;
    private static final int WIDTH = 720;
    private static final int HEIGHT = 350;
    //     Statusregister
    private FontCache font;
    //    Hilfsvariablen für blinkende Text
    private long lastBlink;
    private boolean shouldBlink;

    private int[] pixels;

    public MDA() {
        try {
            this.font = new FontCache( ImageIO.read(
                this.getClass().getResourceAsStream( "/MDA.png" ) ), 9, 14, 32 );
        } catch( IOException e ) {
            LOGGER.error( e );
        }
        try {
            RAM.INSTANCE.write( MODE_REGISTER, new byte[] { Mode.ENABLE_VIDEO_OUTPUT.value } );
        } catch( OutOfRamException e ) {
            LOGGER.error( e );
        }

        lastBlink = 0;

        this.pixels = new int[getWidth() * getHeight()];

    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public int[] getFramebuffer() {
        return pixels;
    }

    @Override
    public void tick() {
        lastBlink++;
        if( lastBlink >= 30 ) {
            shouldBlink = !shouldBlink;
            lastBlink = 0;
        }
    }

    @Override
    public void render() {

        try {
            if( ( RAM.INSTANCE.read( MODE_REGISTER ) & Mode.ENABLE_VIDEO_OUTPUT.value ) != Mode.ENABLE_VIDEO_OUTPUT.value ) {
                return;
            }
        } catch( OutOfRamException e ) {
            LOGGER.error( e );
        }

        // read mem
        for( int xy = 0; xy < 25 * 80; xy++ ) {
            int x = xy % 80;
            int y = xy / 80;

            int c = 0;
            int attr = 0;
            try {
                c = RAM.INSTANCE.read( ( ( x + y * 80 ) * 2 ) + 0x0B0000 );
                attr = RAM.INSTANCE.read( ( ( x + y * 80 ) * 2 + 1 ) + 0x0B0000 );
            } catch( Exception e ) {
                LOGGER.error( e );
            }

            for( int fy = 0; fy < 14; fy++ ) {
                for( int fx = 0; fx < 9; fx++ ) {
                    int col = getColor( fx, fy, c, attr );

                    this.pixels[( x * 9 + fx ) + ( ( y * 14 + fy ) * this.getWidth() )] = col;
                }
            }
        }
    }

    private int getColor( int fx, int fy, int c, int attr ) {

        int d = font.getPixels( c )[fx + fy * font.getCharWidth()];

        Color color = getSpecialColor( attr, d );

        if( isAttrSet( HIGHSENSITY, attr ) && attr != HIGHSENSITY && attr != 0x88 ) {
            color = isCharPixel( d ) ? Color.LIGHTGREEN : Color.BLACK;
            if( isAttrSet( UNDERLINE, attr ) && fy == 12 ) {
                color = Color.LIGHTGREEN;
            }
        } else if( isAttrSet( UNDERLINE, attr ) && fy == 12 ) {
            color = Color.GREEN;
        }

        try {
            if( isAttrSet( BLINK, attr ) && ( RAM.INSTANCE.read( MODE_REGISTER ) & Mode.BLINK.value ) == 0 && shouldBlink )
                color = Color.BLACK;
        } catch( OutOfRamException e ) {
            LOGGER.error( e );
        }

        return color.getValue();
    }

    private Color getSpecialColor( int attr, int d ) {
        Color color = Color.BLACK;

        int b = 0;
        try {
            b = RAM.INSTANCE.read( MODE_REGISTER );
        } catch( OutOfRamException e ) {
            LOGGER.error( e );
        }

        switch( attr ) {
            case 0x00:
            case 0x08:
            case 0x80:
            case 0x88:
                color = Color.BLACK;
                break;
            case 0x70:
            case 0xf0:
                color = isCharPixel( d ) ? Color.BLACK : Color.LIGHTGREEN;
                try {
                    if( ( RAM.INSTANCE.read( MODE_REGISTER ) & Mode.BLINK.value ) == 0 && shouldBlink && attr == 0xf0 ) {
                        color = Color.BLACK;
                    }
                } catch( OutOfRamException e ) {
                    LOGGER.error( e );
                }
                break;
            case 0x78:
            case 0xf8:
                color = isCharPixel( d ) ? Color.GREEN : Color.LIGHTGREEN;

                try {
                    if( ( RAM.INSTANCE.read( MODE_REGISTER ) & Mode.BLINK.value ) == 0 && shouldBlink && attr == 0xf8 ) {
                        color = isCharPixel( d ) ? Color.GREEN : Color.LIGHTGREEN;
                    }
                } catch( OutOfRamException e ) {
                    e.printStackTrace();
                }

                break;
            default:
                color = isCharPixel( d ) ? Color.GREEN : Color.BLACK;
                break;

        }

        return color;
    }

    private boolean isCharPixel( int d ) {
        return d == 0;
    }


    private boolean isAttrSet( int val, int attr ) {
        return ( attr & val ) > 0;
    }

}
