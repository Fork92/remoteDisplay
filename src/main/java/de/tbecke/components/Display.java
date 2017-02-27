package de.tbecke.components;

import de.tbecke.gfx.CardManager;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Created by tbecke on 25.02.17.
 */
public class Display extends Canvas {

    private Dimension resolution;
    private int[] pixels;
    private transient BufferedImage image;

    public Display() {

        resolution = new Dimension( CardManager.getInstance().getCurrent().getWidth(), CardManager.getInstance().getCurrent().getHeight() );

        this.setPreferredSize( resolution );
        this.setMaximumSize( resolution );
        this.setMinimumSize( resolution );

        image = new BufferedImage( this.resolution.width, this.resolution.height, BufferedImage.TYPE_INT_RGB );
        pixels = ( (DataBufferInt) image.getRaster().getDataBuffer() ).getData();

    }

    public void render() {

        BufferStrategy bufferStrategy = this.getBufferStrategy();

        if( bufferStrategy == null ) {
            this.createBufferStrategy( 3 );
            return;
        }

        CardManager.getInstance().getCurrent().render();
        System.arraycopy( CardManager.getInstance().getCurrent().getFramebuffer(), 0, pixels, 0, pixels.length );

        Graphics graphics = bufferStrategy.getDrawGraphics();
        graphics.setColor( Color.BLACK );
        graphics.fillRect( 0, 0, this.getWidth(), this.getHeight() );

        graphics.drawImage( image, 0, 0, this.getWidth(), this.getHeight(), null );

        graphics.dispose();
        bufferStrategy.show();

    }

}
