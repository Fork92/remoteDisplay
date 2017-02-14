package de.tbecke.gfx.cards;

import de.tbecke.gfx.cache.FontCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.IOException;

public class MDA extends GraphicsCard {
    //    Const Variablen für die Attribute
    private static final char UNDERLINE = 1 << 1;
    private static final char HIGHSENSITY = 1 << 3;
    private static final char BLINK = 1 << 7;
    private static final Logger LOGGER = LogManager.getLogger( MDA.class );
    private static final String MODE_REGISTER = "0x03B8";
    private static final int WIDTH = 720;
    private static final int HEIGHT = 350;
    //     Statusregister
    private FontCache font;
    //    Hilfsvariablen für blinkende Text
    private long lastBlink;
    private boolean shouldBlink = false;

    MDA() {
        super( 4096, "MDA" );
        try {
            this.font = new FontCache( ImageIO.read(
                this.getClass().getResourceAsStream( "/MDA.png" ) ) );
        } catch( IOException e ) {
            LOGGER.error( e );
        }
        addRegister( MODE_REGISTER );

        writeRegister( MODE_REGISTER, Mode.ENABLE_VIDEO_OUTPUT.value );
        writeRegister( MODE_REGISTER, Mode.BLINK.value );

        lastBlink = 0;

        this.framebuffer = new int[this.getWidth() * this.getHeight()];
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
        if( isBitSet( MODE_REGISTER, Mode.ENABLE_VIDEO_OUTPUT.value ) ) {

            // read mem
            for( int y = 0; y < 25; y++ ) {
                for( int x = 0; x < 80; x++ ) {
                    int c = ram[( x + y * 80 ) * 2];
                    int attr = ram[( x + y * 80 ) * 2 + 1];
                    int charX = c % 32 * 9;
                    int charY = c / 32 * 14;

                    for( int fy = 0; fy < 14; fy++ ) {
                        for( int fx = 0; fx < 9; fx++ ) {
                            int col = getColor( font.getPixels()[( charX + fx ) + ( ( charY + fy ) * font.getWidth() )], attr ).getValue();

                            if( isAttrSet( UNDERLINE, attr ) && fy == 12 ) {
                                col = Color.GREEN.getValue();
                                if( isAttrSet( HIGHSENSITY, attr ) ) {
                                    col = Color.LIGHTGREEN.getValue();
                                }
                            }

                            if( isAttrSet( BLINK, attr ) && isBitSet( MODE_REGISTER, Mode.BLINK.value ) && shouldBlink ) {
                                col = Color.BLACK.getValue();
                            }

                            this.framebuffer[( x * 9 + fx ) + ( ( y * 14 + fy ) * this.getWidth() )] = col;
                        }
                    }

                }

            }
        }

    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    private Color getColor( int d, int attr ) {
        Color color = ( isCharPixel( d ) && isNotSpecialAttr( attr ) ) ? Color.GREEN : Color.BLACK;

        if(attr == 0x78 || attr == 0xf8) {
            color = d == 0 ? Color.GREEN : Color.LIGHTGREEN;
        } else if( attr == 0x70 || attr == 0xf0 ) {
            color = d == 0 ? Color.BLACK : Color.GREEN;
        } else if( isAttrSet( HIGHSENSITY, attr ) && attr != HIGHSENSITY ) {
            color = d == 0 ? Color.LIGHTGREEN : Color.BLACK;
        }

        return color;
    }

    private boolean isCharPixel( int d ) {
        return d == 0;
    }

    private boolean isNotSpecialAttr( int attr ) {
        return ( attr != 0x00 ) && ( attr != 0x08 ) && ( attr != 0x80 ) && ( attr != 0x88 );
    }

    private boolean isAttrSet( int val, int attr ) {
        return ( attr & val ) > 0;
    }

    @Override
    public String hasInfo() {
        return "\t- MDA - This is a Text only card. Each character use 2 bytes.\n" +
            "\t        The first byte is the character code and the second byte are attributes.\n" +
            "\t        Available attributes are: Bit 1 - underline\n" +
            "\t                                  Bit 3 - high intensity\n" +
            "\t                                  Bit 7 - blink\n" +
            "\t        The Attributes has eight exceptions:\n" +
            "\t                                  0x00, 0x08, 0x80, 0x88 display as black space\n" +
            "\t                                  0x70 display as black on green\n" +
            "\t                                  0x78 display as dark green on green\n" +
            "\t                                  0xF0 display as blinking version of 0x70(is blinking enabled), as black on green otherwise\n" +
            "\t                                  0xF0 display as blinking version of 0x78(is blinking enabled), as dark green on green otherwise\n" +
            "\t        The MDA card has one register at: 0x03B8\n" +
            "\t        Available modes are: Bit 3 - Enable Video output\n" +
            "\t                             Bit 5 - Enable Blink\n";
    }
}
