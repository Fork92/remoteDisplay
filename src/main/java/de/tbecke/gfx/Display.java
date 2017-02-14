package de.tbecke.gfx;

import de.tbecke.gfx.cards.CardManager;
import de.tbecke.net.TCPServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Display extends Canvas implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger( Display.class );
    private JFrame frame;
    private boolean running;
    private transient CardManager cardManager;
    private transient BufferedImage image;
    private int[] pixels;
    private transient Thread server;
    private transient TCPServer tcpServer;

    public Display() {
        this.frame = new JFrame( "RemoteDisplay" );

        this.cardManager = new CardManager( this );
        this.tcpServer = new TCPServer( "localhost", 1337, cardManager );


        this.frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        this.frame.setLayout( new BorderLayout() );
        this.frame.add( this, BorderLayout.CENTER );
        this.frame.pack();
        this.frame.setResizable( false );
        this.frame.setLocationRelativeTo( null );
        this.frame.pack();
        this.frame.setVisible( true );

        this.start();

    }

    private void start() {
        this.running = true;
        new Thread( this, "RemoteDisplay" ).start();
        server = new Thread( tcpServer, "Server" );
        server.start();
    }

    public void setDisplay( int w, int h ) {
        this.image = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
        this.pixels = ( (DataBufferInt) this.image.getRaster().getDataBuffer() ).getData();

        Dimension dimension = new Dimension( w, h );
        this.setMinimumSize( dimension );
        this.setMaximumSize( dimension );
        this.setPreferredSize( dimension );

        this.frame.pack();
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
                // clean up the code
                Thread.currentThread().interrupt();
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

    private void ticks() {
        if( !server.isAlive() ) {
            this.stop();
        }
        cardManager.getCurrent().tick();
    }

    private void stop() {
        running = false;
        LOGGER.debug( "Display shutting down" );
        this.frame.dispose();
    }

    private void render() {

        BufferStrategy bufferStrategy = this.getBufferStrategy();


        if( bufferStrategy == null ) {
            this.createBufferStrategy( 3 );
            return;
        }

        cardManager.getCurrent().render();
        System.arraycopy( cardManager.getCurrent().getFramebuffer(), 0, pixels, 0, pixels.length );

        Graphics graphics = bufferStrategy.getDrawGraphics();
        graphics.setColor( Color.WHITE );
        graphics.fillRect( 0, 0, this.getWidth(), this.getHeight() );
        graphics.setColor( Color.BLACK );

        graphics.drawImage( image, 0, 0, this.getWidth(), this.getHeight(), null );

        graphics.dispose();
        bufferStrategy.show();

    }
}
