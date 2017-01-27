package de.tbecke.gfx;

import de.tbecke.gfx.cards.GraphicsCard;
import de.tbecke.gfx.cards.MDA;
import de.tbecke.net.TCPServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Created by tbecke on 20.12.16.
 */
public class Display extends Canvas implements Runnable {

    private final Logger LOGGER = LogManager.getLogger( Display.class );
    private JFrame frame;
    private Dimension dimension;
    private boolean running;
    private GraphicsCard graphicsCard;
    private BufferedImage image;
    private int[] pixels;
    private TCPServer server;

    public Display() {
        this.graphicsCard = new MDA();

        this.server = new TCPServer(1337, this.graphicsCard);
        this.image = new BufferedImage( this.graphicsCard.getWidth(), this.graphicsCard.getHeight(),
            BufferedImage.TYPE_INT_RGB );
        this.pixels = ((DataBufferInt) this.image.getRaster().getDataBuffer()).getData();

        this.dimension = new Dimension( this.graphicsCard.getWidth(), this.graphicsCard.getHeight() );

        this.setMinimumSize( this.dimension );
        this.setMaximumSize( this.dimension );
        this.setPreferredSize( this.dimension );

        this.frame = new JFrame( "RemoteDisplay" );
        this.frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        this.frame.setLayout( new BorderLayout() );
        this.frame.add( this, BorderLayout.CENTER );
        this.frame.pack();
        this.frame.setResizable( false );
        this.frame.setLocationRelativeTo( null );

        this.frame.setVisible( true );

        this.start();

    }

    public void start() {
        this.running = true;
        new Thread( this, "RemoteDisplay" ).start();
        new Thread( server, "Server" ).start();
    }

    public void stop() {
        running = false;
    }


    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double unprocessed = 0;
        double nsPerTick = 1000000000.0 / 60;
        int frames = 0;
        int ticks = 0;
        long lastTimer1 = System.currentTimeMillis();

        while( running ) {
            long now = System.nanoTime();
            unprocessed += ( now - lastTime ) / nsPerTick;
            lastTime = now;
            boolean shouldRender = false;

            while( unprocessed >= 1 ) {
                ticks++;
                ticks();
                unprocessed -= 1;
                shouldRender = true;
            }

            try {
                Thread.sleep( 2 );
            } catch( InterruptedException e ) {
                LOGGER.error( e );
            }

            if( shouldRender ) {
                frames++;
                render();
            }

            if( lastTimer1 + 1000 < System.currentTimeMillis() ) {
                LOGGER.debug( "FPS: " + frames + ", UPS: " + ticks );
                frames = 0;
                ticks = 0;
                lastTimer1 += 1000;
            }

        }

    }

    public void ticks() {
        graphicsCard.tick();
    }

    public void render() {

        BufferStrategy bufferStrategy = this.getBufferStrategy();


        if( bufferStrategy == null ) {
            this.createBufferStrategy( 3 );
            return;
        }

        graphicsCard.render();

        for(int i = 0; i < pixels.length; i++) {
            pixels[i] = graphicsCard.framebuffer[i];
        }

        Graphics graphics = bufferStrategy.getDrawGraphics();
        graphics.setColor( Color.WHITE );
        graphics.fillRect( 0, 0, this.getWidth(), this.getHeight() );
        graphics.setColor( Color.BLACK );

        graphics.drawImage( image, 0, 0, this.getWidth(), this.getHeight(), null );

        graphics.dispose();
        bufferStrategy.show();

    }
}
