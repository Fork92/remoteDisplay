package de.egot;

import de.egot.components.Display;
import de.egot.components.NIC;
import de.egot.utils.CardManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Created by egot on 25.02.17.
 */
public class Manager implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger( Manager.class );

    private Display display;
    private NIC nic;
    private JFrame frame;

    private boolean running;


    public Manager( String title, String host, int port ) {
        frame = new JFrame( title );

        display = new Display();
        nic = new NIC( host, port );

        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setLayout( new BorderLayout() );
        frame.add( display, BorderLayout.CENTER );
        frame.pack();
        frame.setResizable( false );
        frame.setLocationRelativeTo( null );
        frame.pack();
        frame.setVisible( true );

        this.start();

    }

    private void start() {
        running = true;

        new Thread( nic ).start();
        new Thread( this ).start();
    }

    public static void main( String[] args ) {
        if( args.length < 2 ) {
            CardManager.getInstance().setCurrent( "MDA" );
        } else if( args.length == 3 && "-c".equals( args[2] ) ) {
            CardManager.getInstance().setCurrent( args[3] );
        }

        int i = 0;
        String host = "127.0.0.1";
        int port = 1337;

        while( i < args.length ) {
            switch( args[i] ) {
                case "-c":
                    CardManager.getInstance().setCurrent( args[i + 1] );
                    break;
                case "-h":
                    host = args[i + 1];
                    break;
                case "-p":
                    port = Integer.parseInt( args[i + 1] );
                    break;
                default:
                    break;
            }
            i += 2;
        }

        new Manager( "EGoT", host, port );
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double unprocessed = 0;
        double nsPerTick = 1000000000.0 / 60;
        int frames = 0;
        int ticks = 0;
        long lastTimer1 = System.currentTimeMillis();

        while( isRunning() ) {
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

        this.stop();
    }

    private void stop() {
        frame.setVisible( false );
        frame.dispose();
    }

    private void ticks() {
        CardManager.getInstance().getCurrent().tick();
    }

    private void render() {
        CardManager.getInstance().getCurrent().render();
        display.render();
    }

    private boolean isRunning() {
        return running && nic.isRunning();
    }

}
