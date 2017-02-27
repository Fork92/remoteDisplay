package de.tbecke.components.cards;

import de.tbecke.components.RAM;
import de.tbecke.components.cards.utils.Color;
import de.tbecke.components.cards.utils.Mode;
import de.tbecke.components.cards.utils.RamException;
import de.tbecke.gfx.cache.FontCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.IOException;

public class CGA implements GraphicsCard {

    private static final Logger LOGGER = LogManager.getLogger( CGA.class );
    private static final int WIDTH = 640;
    private static final int HEIGHT = 200;
    private static final int MODE_REGISTER = 0x03D8;
    private static final int COLOR_REGISTER = 0x03D9;

    private FontCache font;
    private int[] colorSet0;
    private int[] colorSet1;
    private int[] colorSetBW;

    private int[] pixels;

    public CGA() {

        try {
            this.font = new FontCache( ImageIO.read( this.getClass().getResourceAsStream( "/CGA.png" ) ) );
        } catch( IOException e ) {
            LOGGER.error( e );
        }

        try {
            RAM.INSTANCE.write( MODE_REGISTER, new byte[] { Mode.ENABLE_VIDEO_OUTPUT.value } );
            RAM.INSTANCE.write( COLOR_REGISTER, new byte[] { 0x18 } );
        } catch( RamException e ) {
            LOGGER.error( e );
        }

        initColors();

        this.pixels = new int[getWidth() * getHeight()];
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

    private int getRenderWidth() {
        int b = 0;
        try {
            b = RAM.INSTANCE.read( MODE_REGISTER );
        } catch( RamException e ) {
            LOGGER.error( e );
        }

        if( ( b & Mode.HIGH_RES.value ) != 0 ) {
            return 80;
        } else {
            return 40;
        }
    }

    @Override
    public void render() {

        int b = 0;
        try {
            b = RAM.INSTANCE.read( MODE_REGISTER );
        } catch( RamException e ) {
            LOGGER.error( e );
        }

        if( ( b & Mode.ENABLE_VIDEO_OUTPUT.value ) == Mode.ENABLE_VIDEO_OUTPUT.value ) {
            if( ( b & Mode.GRAPHIC_MODE.value ) == Mode.GRAPHIC_MODE.value ) {
                this.renderGraphic();
            } else {
                this.renderText();
            }
        }
    }


    private void renderText() {
        int b = 0;
        try {
            b = RAM.INSTANCE.read( MODE_REGISTER );
        } catch( RamException e ) {
            LOGGER.error( e );
        }
        boolean isHighRes = ( b & Mode.HIGH_RES.value ) == Mode.HIGH_RES.value;
        int rW = getRenderWidth();
        // read mem
        for( int xy = 0; xy < 25 * rW; xy++ ) {

            int x = xy % rW;
            int y = xy / rW;

            int c = 0;
            int attr = 0;
            try {
                c = RAM.INSTANCE.read( ( ( x + y * rW ) * 2 ) + 0x0B0000 );
                attr = RAM.INSTANCE.read( ( ( x + y * rW ) * 2 + 1 ) + 0x0B0000 );
            } catch( Exception e ) {
                LOGGER.error( e );
            }
            int charX = c % 32 * 8;
            int charY = c / 32 * 8;

            for( byte p = 0; p < 64; p++ ) {
                int fx = p % 8;
                int fy = p / 8;

                int id = font.getPixels()[( charX + fx ) + ( ( charY + fy ) * font.getWidth() )] == 0 ? attr & 0x0f : attr >> 4;

                int col = this.getColor( id );

                if( isHighRes ) {
                    this.pixels[( x * 8 + fx ) + ( ( y * 8 + fy ) * CGA.WIDTH )] = col;
                } else {
                    this.pixels[( ( x * 8 + fx ) * 2 ) + ( ( y * 8 + fy ) * CGA.WIDTH )] = col;
                    this.pixels[( ( x * 8 + fx ) * 2 + 1 ) + ( ( y * 8 + fy ) * CGA.WIDTH )] = col;
                }
            }

        }

    }

    private void renderGraphic() {
        int pixelsPerByte = 4;

        int b = 0;
        try {
            b = RAM.INSTANCE.read( MODE_REGISTER );
        } catch( RamException e ) {
            LOGGER.error( e );
        }

        if( ( b & Mode.HIGH_RES_GRAPHIC.value ) == Mode.HIGH_RES_GRAPHIC.value ) {
            pixelsPerByte = 8;
        }


        renderInterlacing( 0x0B0000, ( 80 * getHeight() / 2 ) + 0x0B0000, 0, pixelsPerByte );
        renderInterlacing( ( 80 * getHeight() / 2 ) + 0x0B0000, ( 80 * getHeight() ) + 0xB0000, 1, pixelsPerByte );
    }


    private void renderGraphic() {
        int pixelsPerByte = 4;

        int b = 0;
        try {
            b = RAM.INSTANCE.read( MODE_REGISTER );
        } catch( RamException e ) {
            LOGGER.error( e );
        }

        if( ( b & Mode.HIGH_RES_GRAPHIC.value ) == Mode.HIGH_RES_GRAPHIC.value ) {
            pixelsPerByte = 8;
        }


        renderInterlacing( 0x0B0000, ( 80 * getHeight() / 2 ) + 0x0B0000, 0, pixelsPerByte );
        renderInterlacing( ( 80 * getHeight() / 2 ) + 0x0B0000, ( 80 * getHeight() ) + 0xB0000, 1, pixelsPerByte );
    }

    private void renderInterlacing( int ramStart, int ramEnd, int xStart, int pixelsPerByte ) {
        for( int i = ramStart; i < ramEnd; i++ ) {
            int[] id = new int[0];
            try {
                id = getId( RAM.INSTANCE.read( i ) );
            } catch( Exception e ) {
                LOGGER.error( e );
            }

            for( int p = 0; p < id.length; p++ ) {

                int x = ( ( ( i - ramStart ) * pixelsPerByte ) + p ) % ( 80 * pixelsPerByte );
                int y = ( ( ( ( ( i - ramStart ) * pixelsPerByte ) + p ) / ( 80 * pixelsPerByte ) ) * 2 ) + xStart;
                if( pixelsPerByte == 8 ) {
                    pixels[x + ( y * ( 80 * pixelsPerByte * 2 ) )] = getColor( id[p] );
                } else {
                    pixels[( x * 2 ) + ( y * ( 80 * pixelsPerByte * 2 ) )] = getColor( id[p] );
                    pixels[( x * 2 + 1 ) + ( y * ( 80 * pixelsPerByte * 2 ) )] = getColor( id[p] );
                }
            }

        }
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    private int getColor( int id ) {
        int col;
        int mode = 0;
        int color = 0;
        try {
            mode = RAM.INSTANCE.read( MODE_REGISTER );
            color = RAM.INSTANCE.read( COLOR_REGISTER );
        } catch( RamException e ) {
            LOGGER.error( e );
        }


        if( ( mode & Mode.GRAPHIC_MODE.value ) != 0 ) {
            if( ( mode & Mode.HIGH_RES_GRAPHIC.value ) != 0 ) {
                col = colorSetBW[id];
            } else if( ( color & (byte) ( 1 << 5 ) ) != 0 ) {
                if( ( color & (byte) ( 1 << 4 ) ) != 0 ) {
                    col = colorSet1[id + 4];
                } else {
                    col = colorSet1[id];
                }
            } else {
                if( ( color & (byte) ( 1 << 4 ) ) != 0 ) {
                    col = colorSet0[id + 4];
                } else {
                    col = colorSet0[id];
                }
            }
        } else {
            col = Color.getValueByID( id );

            if( ( mode & Mode.BLINK.value ) != 0 ) {
                col = Color.BLACK.getValue();
            }
        }


        return col;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    private int[] getId( int b ) {
        int[] ids = new int[4];
        ids[0] = ( b & 0xc0 ) >> 6;
        ids[1] = ( b & 0x30 ) >> 4;
        ids[2] = ( b & 0x0c ) >> 2;
        ids[3] = b & 0x03;

        return ids;
    }

    @Override
    public int[] getFramebuffer() {
        return pixels;
    }

}
