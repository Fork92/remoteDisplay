package de.tbecke.gfx.cards;

import de.tbecke.gfx.cache.FontCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.IOException;

/**
 * Created by tbecke on 26.01.17.
 */
public class CGA extends GraphicsCard {

    private static final Logger LOGGER = LogManager.getLogger( CGA.class );

    private char MODE_REGISTER;

    private char[] RAM = new char[16 * 1024];
    private FontCache font;

    private int width = 640;
    private int height = 200;
    private int maxCol;

    private int lastBlink;
    private boolean blink;

    CGA() {
        try {
            this.font = new FontCache( ImageIO.read( this.getClass().getResourceAsStream( "/CGA.png" ) ) );
        } catch( IOException e ) {
            LOGGER.error( e );
        }

        setMode( Mode.ENABLE_VIDEO_OUTPUT );
        setMode( Mode.HIGH_RES );
        setRenderWidth();

        this.framebuffer = new int[width * height];
    }

    private void setRenderWidth() {
        if( isModeSet( Mode.HIGH_RES ) ) {
            maxCol = 80;
        } else {
            maxCol = 40;
        }
    }

    private boolean isModeSet( Mode mode ) {
        return ( MODE_REGISTER & mode.value ) > 0;
    }

    @Override
    public void render() {
        if( isModeSet( Mode.ENABLE_VIDEO_OUTPUT ) ) {
            if( isModeSet( Mode.GRAPHIC_MODE ) ) {
                this.renderGraphic();
            } else {
                this.renderText();
            }
        }
    }

    private void renderText() {

        // read mem
        for( int y = 0; y < 25; y++ ) {
            for( int x = 0; x < maxCol; x++ ) {
                int c = RAM[( x + y * maxCol ) * 2];
                int attr = RAM[( x + y * maxCol ) * 2 + 1];
                int charX = c % 32 * 8;
                int charY = c / 32 * 8;

                for( int fy = 0; fy < 8; fy++ ) {
                    for( int fx = 0; fx < 8; fx++ ) {

                        int id = font.pixels[( charX + fx ) + ( ( charY + fy ) * font.width )] == 0 ? attr & 0x0f : attr >> 4;

                        int col = Color.getValueByID( id );

                        if( isModeSet( Mode.BLINK ) && blink )
                            col = Color.BLACK.value;

                        if( isModeSet( Mode.HIGH_RES ) ) {
                            this.framebuffer[( x * 8 + fx ) + ( ( y * 8 + fy ) * this.width )] = col;
                        } else {
                            this.framebuffer[( ( x * 8 + fx ) * 2 ) + ( ( y * 8 + fy ) * this.width )] = col;
                            this.framebuffer[( ( x * 8 + fx ) * 2 + 1 ) + ( ( y * 8 + fy ) * this.width )] = col;
                        }

                    }
                }

            }
        }

    }

    private void renderGraphic() {

    }

    @Override
    public void tick() {
        lastBlink++;
        if( lastBlink >= 30 ) {
            blink = !blink;
            lastBlink = 0;
        }
    }

    @Override
    public void setMode( Mode mode ) {
        this.MODE_REGISTER ^= mode.value;
    }

    @Override
    public void setRAM( int addr, char value ) {
        RAM[addr] = value;
    }

    @Override
    public void setRAM( int addr, String msg, char flag ) {
        if( !isModeSet( Mode.GRAPHIC_MODE ) ) {
            for( char c : msg.toCharArray() ) {
                RAM[addr++] = c;
                RAM[addr++] = flag;
            }
        }
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
