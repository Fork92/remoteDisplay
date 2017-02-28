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

/**
 * Die CGA Grafikkarte hat eine Auflösung von max. 640x200px
 * Der Speicherbereich der CGA beginnt an Adresse 0x0B0000 und beträgt 16k
 * An den Adressen 0x0003D8 und 0x0003D9 liegen die Register für die Modis und der Farbe.
 * Register 0x0003D8 Moderegister
 * <ul>
 * <li>Bit 0: 1 für 80 Zeichen/Zeile Auflösung, 0 für 40 Zeichen, hat nur im Textmode auswirkungen</li>
 * <li>Bit 1: 1 um den Grafikmode zu aktivieren, 0 für den Textmode</li>
 * <li>Bit 2: wird nicht genutzt</li>
 * <li>Bit 3: 1 um die Videoausgabe zu aktivieren, 0 zum deaktivieren(Das bild wird eingefroren</li>
 * <li>Bit 4: 1 um die Highresolution Grafik(640x200px) zu aktivieren, 0 für LowRes Grafik(320x200px), hat nur im Grafik modus auswirkungen</li>
 * <li>Bit 5: 1 um Blinken zu aktivieren, 0 zum Deaktivieren, hat nur im Textmodus auswirkungen</li>
 * <li>Bit 6: wird nicht genutzt</li>
 * <li>Bit 7: wird nicht genutzt</li>
 * </ul>
 * <br>
 * Register 0x0003D9 Colorregister
 * <ul>
 * <li>Bit 0: Blue, in HighRes mode wird dieses Bit für die Vordergrundfarbe genutzt in LowRes Mode für die Hintergrund Farbe</li>
 * <li>Bit 1: Green, in HighRes mode wird dieses Bit für die Vordergrundfarbe genutzt in LowRes Mode für die Hintergrund Farbe</li>
 * <li>Bit 2: Red, in HighRes mode wird dieses Bit für die Vordergrundfarbe genutzt in LowRes Mode für die Hintergrund Farbe</li>
 * <li>Bit 3: Intensity, in HighRes mode wird dieses Bit für die Vordergrundfarbe genutzt in LowRes Mode für die Hintergrund Farbe</li>
 * <li>Bit 4: 1 für Helle Fordergrundfarbe im LowRes Mode</li>
 * <li>Bit 5: Wählt die Farbpalette aus, nur im LowRes mode.</li>
 * <li>Bit 6:</li>
 * <li>Bit 7:</li>
 * </ul>
 *
 * @author Tobias Becker
 * @version 1.0
 */
public class CGA implements GraphicsCard {

    private static final Logger LOGGER = LogManager.getLogger( CGA.class );
    private static final int WIDTH = 640;
    private static final int HEIGHT = 200;
    private static final int MODE_REGISTER = 0x03D8;
    private static final int COLOR_REGISTER = 0x03D9;

    private FontCache font;
    private int[][] colorSet;
    private int[] colorSetBW;

    private long lastBlink;
    private boolean shouldBlink;

    private int[] pixels;

    /**
     * Erzeugt eine neue Instance der CGA Grafikkarte.
     */
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

        int changeableColor = 0;
        int brightColor = 0;
        try {
            changeableColor = ( ( RAM.INSTANCE.read( 0x0003D9 ) & 0b1000 ) )
                + ( ( RAM.INSTANCE.read( 0x0003D9 ) & 0b0100 ) )
                + ( ( RAM.INSTANCE.read( 0x0003D9 ) & 0b0010 ) )
                + ( ( RAM.INSTANCE.read( 0x0003D9 ) & 0b0001 ) );
            brightColor = +RAM.INSTANCE.read( 0x0003D9 & 0b10000 ) != 0 ? 8 : 0;
        } catch( RamException e ) {
            LOGGER.error( e );
        }

        colorSet = new int[2][8];

        colorSet[0][0] = Color.getValueByID( changeableColor );
        colorSet[0][1] = Color.getValueByID( 2 + brightColor );
        colorSet[0][2] = Color.getValueByID( 4 + brightColor );
        colorSet[0][3] = Color.getValueByID( 5 + brightColor );

        colorSet[1][0] = Color.getValueByID( changeableColor );
        colorSet[1][1] = Color.getValueByID( 3 + brightColor );
        colorSet[1][2] = Color.getValueByID( 5 + brightColor );
        colorSet[1][3] = Color.getValueByID( 7 + brightColor );

        colorSetBW = new int[2];
        colorSetBW[0] = Color.getValueByID( 0 );
        colorSetBW[1] = Color.getValueByID( changeableColor );
    }

    /**
     * @return Die Breite des Framebuffers in px als int
     */
    @Override
    public int getWidth() {
        return WIDTH;
    }

    /**
     * @return Die höhe des Framebuffers in px als int.
     */
    @Override
    public int getHeight() {
        return HEIGHT;
    }

    /**
     * @return Die Pixel des Framebuffers als int[]
     */
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

    /**
     *
     */
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

                int col = getColor( id, attr );

                if( isHighRes ) {
                    this.pixels[( x * 8 + fx ) + ( ( y * 8 + fy ) * CGA.WIDTH )] = col;
                } else {
                    this.pixels[( ( x * 8 + fx ) * 2 ) + ( ( y * 8 + fy ) * CGA.WIDTH )] = col;
                    this.pixels[( ( x * 8 + fx ) * 2 + 1 ) + ( ( y * 8 + fy ) * CGA.WIDTH )] = col;
                }
            }

        }

    }

    private int getColor( int id, int attr ) {

        int col = 0;

        int mode = 0;
        try {
            mode = RAM.INSTANCE.read( MODE_REGISTER );
        } catch( RamException e ) {
            e.printStackTrace();
        }

        if( ( mode & Mode.GRAPHIC_MODE.value ) != Mode.GRAPHIC_MODE.value ) {
            if( ( attr & 0x80 ) == 0 )
                col = Color.getValueByID( id );
            else
                col = Color.getValueByID( id & 0x07 );

            if( ( mode & Mode.BLINK.value ) == Mode.BLINK.value && shouldBlink && ( attr & 0x80 ) == 0x80 ) {
                col = Color.BLACK.getValue();
            }
        }

        return col;

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
        int colSet = 0;
        try {
            colSet = ( RAM.INSTANCE.read( 0x0003D9 ) & 0x10 ) != 0 ? 1 : 0;
        } catch( RamException e ) {
            e.printStackTrace();
        }

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
                    pixels[x + ( y * ( 80 * pixelsPerByte * 2 ) )] = colorSet[colSet][id[p]];
                } else {
                    pixels[( x * 2 ) + ( y * ( 80 * pixelsPerByte * 2 ) )] = colorSet[colSet][id[p]];
                    pixels[( x * 2 + 1 ) + ( y * ( 80 * pixelsPerByte * 2 ) )] = colorSet[colSet][id[p]];
                }
            }

        }
    }

    private int[] getId( int b ) {
        int[] ids = new int[4];
        ids[0] = ( b & 0xc0 ) >> 6;
        ids[1] = ( b & 0x30 ) >> 4;
        ids[2] = ( b & 0x0c ) >> 2;
        ids[3] = b & 0x03;

        return ids;
    }

}
