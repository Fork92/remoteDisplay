package de.tbecke.gfx.cards;

import de.tbecke.gfx.cache.FontCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.IOException;

public class CGA extends GraphicsCard {

    private static final Logger LOGGER = LogManager.getLogger( CGA.class );
    private static final int WIDTH = 640;
    private static final int HEIGHT = 200;
    private static final String MODE_REGISTER = "0x03D8";
    private static final String COLOR_REGISTER = "0x03D9";
    private FontCache font;
    private int[] colorSet0;
    private int[] colorSet1;
    private int[] colorSetBW;
    private int maxCol;

    private int lastBlink;
    private boolean shouldBlink;

    CGA() {
        super( 16 * 1000, "CGA" );
        try {
            this.font = new FontCache( ImageIO.read( this.getClass().getResourceAsStream( "/CGA.png" ) ) );
        } catch( IOException e ) {
            LOGGER.error( e );
        }

        addRegister( MODE_REGISTER );
        addRegister( COLOR_REGISTER );

        writeRegister( MODE_REGISTER, Mode.ENABLE_VIDEO_OUTPUT.value );
        writeRegister( COLOR_REGISTER, (byte) 0x18 );
        setRenderWidth();

        initColors();

        this.framebuffer = new int[WIDTH * HEIGHT];
    }

    private void initColors() {
        colorSet0 = new int[8];
        colorSet0[0] = Color.getValueByID( 0 );
        colorSet0[1] = Color.getValueByID( 2 );
        colorSet0[2] = Color.getValueByID( 4 );
        colorSet0[3] = Color.getValueByID( 5 );
        colorSet0[4] = Color.getValueByID( 0 );
        colorSet0[5] = Color.getValueByID( 10 );
        colorSet0[6] = Color.getValueByID( 12 );
        colorSet0[7] = Color.getValueByID( 14 );

        colorSet1 = new int[8];
        colorSet1[0] = Color.getValueByID( 0 );
        colorSet1[1] = Color.getValueByID( 3 );
        colorSet1[2] = Color.getValueByID( 5 );
        colorSet1[3] = Color.getValueByID( 7 );
        colorSet1[4] = Color.getValueByID( 0 );
        colorSet1[5] = Color.getValueByID( 11 );
        colorSet1[6] = Color.getValueByID( 13 );
        colorSet1[7] = Color.getValueByID( 15 );

        colorSetBW = new int[2];
        colorSetBW[0] = Color.getValueByID( 0 );
        colorSetBW[1] = Color.getValueByID( 15 );
    }

    private void setRenderWidth() {
        if( isBitSet( MODE_REGISTER, Mode.HIGH_RES.value ) ) {
            maxCol = 80;
        } else {
            maxCol = 40;
        }
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
            if( isBitSet( MODE_REGISTER, Mode.GRAPHIC_MODE.value ) ) {
                this.renderGraphic();
            } else {
                this.renderText();
            }
        }
    }

    private void renderText() {
        setRenderWidth();
        boolean isHighRes = isBitSet( MODE_REGISTER, Mode.HIGH_RES.value );
        // read mem
        for( int y = 0; y < 25; y++ ) {
            for( int x = 0; x < maxCol; x++ ) {
                int c = ram[( x + y * maxCol ) * 2];
                int attr = ram[( x + y * maxCol ) * 2 + 1];
                int charX = c % 32 * 8;
                int charY = c / 32 * 8;

                for( int p = 0; p < 64; p++ ) {
                    int fx = p % 8;
                    int fy = p / 8;

                    int id = font.getPixels()[( charX + fx ) + ( ( charY + fy ) * font.getWidth() )] == 0 ? attr & 0x0f : attr >> 4;

                    int col = this.getColor( id );

                    if( isHighRes ) {
                        this.framebuffer[( x * 8 + fx ) + ( ( y * 8 + fy ) * CGA.WIDTH )] = col;
                    } else {
                        this.framebuffer[( ( x * 8 + fx ) * 2 ) + ( ( y * 8 + fy ) * CGA.WIDTH )] = col;
                        this.framebuffer[( ( x * 8 + fx ) * 2 + 1 ) + ( ( y * 8 + fy ) * CGA.WIDTH )] = col;
                    }
                }
            }
        }

    }

    private void renderGraphic() {
        int pixelsPerByte = 4;
        if( isBitSet( MODE_REGISTER, Mode.HIGH_RES_GRAPHIC.value ) ) {
            pixelsPerByte = 8;
        }

        renderInterlacing( 0, 80 * getHeight() / 2, 0, pixelsPerByte );
        renderInterlacing( 80 * getHeight() / 2, 80 * getHeight(), 1, pixelsPerByte );
    }

    private void renderInterlacing( int ramStart, int ramEnd, int xStart, int pixelsPerByte ) {
        for( int i = ramStart; i < ramEnd; i++ ) {
            int[] id = getId( ram[i] );

            for( int p = 0; p < id.length; p++ ) {

                int x = ( ( ( i - ramStart ) * pixelsPerByte ) + p ) % ( 80 * pixelsPerByte );
                int y = ( ( ( ( ( i - ramStart ) * pixelsPerByte ) + p ) / ( 80 * pixelsPerByte ) ) * 2 ) + xStart;
                if( pixelsPerByte == 8 ) {
                    framebuffer[x + ( y * ( 80 * pixelsPerByte * 2 ) )] = getColor( id[p] );
                } else {
                    framebuffer[( x * 2 ) + ( y * ( 80 * pixelsPerByte * 2 ) )] = getColor( id[p] );
                    framebuffer[( x * 2 + 1 ) + ( y * ( 80 * pixelsPerByte * 2 ) )] = getColor( id[p] );
                }
            }

        }
    }

    private int getColor( int id ) {
        int col = 0;

        if( !isBitSet( MODE_REGISTER, Mode.GRAPHIC_MODE.value ) ) {
            col = Color.getValueByID( id );

            if( isBitSet( MODE_REGISTER, Mode.BLINK.value ) && shouldBlink ) {
                col = Color.BLACK.getValue();
            }
        } else if( isBitSet( MODE_REGISTER, Mode.GRAPHIC_MODE.value ) ) {
            if( isBitSet( MODE_REGISTER, Mode.HIGH_RES_GRAPHIC.value ) ) {
                col = colorSetBW[id];
            } else if( isBitSet( COLOR_REGISTER, (byte) ( 1 << 5 ) ) ) {
                if( isBitSet( COLOR_REGISTER, (byte) ( 1 << 4 ) ) ) {
                    col = colorSet1[id + 4];
                } else {
                    col = colorSet1[id];
                }
            } else {
                if( isBitSet( COLOR_REGISTER, (byte) ( 1 << 4 ) ) ) {
                    col = colorSet0[id + 4];
                } else {
                    col = colorSet0[id];
                }
            }
        }


        return col;
    }

    private int[] getId( byte b ) {
        int[] ids = new int[4];
        ids[0] = ( b & 0xc0 ) >> 6;
        ids[1] = ( b & 0x30 ) >> 4;
        ids[2] = ( b & 0x0c ) >> 2;
        ids[3] = b & 0x03;

        return ids;
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
    public String hasInfo() {
        return "\t- CGA - This card has a text and a graphic mode.\n" +
            "\t        Text mode: Each character use 2 bytes.\n" +
            "\t                   The first byte is the character code and the second byte are color attributes.\n" +
            "\t                   Bit 0 - Blue foreground\n" +
            "\t                   Bit 1 - Green foreground\n" +
            "\t                   Bit 2 - Red foreground\n" +
            "\t                   Bit 3 - Bright foreground\n" +
            "\t                   Bit 4 - Blue background\n" +
            "\t                   Bit 6 - Green background\n" +
            "\t                   Bit 7 - Red background\n" +
            "\t                   Bit 8 - Bright background or if blinking enabled blink\n" +
            "\t        Graphic mode: In graphic mode each row is 80 bytes. Each byte corresponds to four pixel (320x200px)\n" +
            "\t                      or to 8 pixels (640x200px resolution). In each case the Highest numbered bits corresponds\n" +
            "\t                      to the leftmost pixel.\n" +
            "\t                      For low resolution you have two color pallets\n" +
            "\t                         Pallet 0 with Black(changeable), Green, Red, Magenta and the bright versions\n" +
            "\t                         Pallet 1 with Black(changeable), Blue, Magenta, Light gray and the bright versions.\n" +
            "\t                      For high resolution there are one pallet:\n" +
            "\t                         Pallet 0 with Black and White(changeable)\n" +
            "\t                      At beginning of memory are the first set of rows (0, 2, 4, 6, ...)\n" +
            "\t                      At offset 0x1F40(8k) are the second set of rows (1, 3, 5, 7, ....)\n" +
            "\t        The CGA card has two registers at: 0x03D8 and 0x03D9\n" +
            "\t            0x03D8 is the Mode Register:\n" +
            "\t            Available modes are: Bit 0 - High resolution text mode. This bit effects only in text mode\n" +
            "\t                                 Bit 1 - Graphic mode. 1 enable graphic mode 0 disable\n" +
            "\t                                 Bit 3 - Enable Video output\n" +
            "\t                                 Bit 4 - High resolution graphic mode. this bit effects only in graphic mode\n" +
            "\t                                 Bit 5 - Enable blinking\n";
    }
}
