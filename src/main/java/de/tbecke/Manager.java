package de.tbecke;

import de.tbecke.components.Display;
import de.tbecke.components.NIC;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Created by tbecke on 25.02.17.
 */
public class Manager implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger( Manager.class );

    private Display display;
    private NIC nic;
    private JFrame frame;

    private boolean running;


    public Manager( String title ) {
        frame = new JFrame( title );

        display = new Display();
        nic = new NIC( "localhost", 1337 );

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
                display.render();
            }

            if( lastTimer1 + 1000 < System.currentTimeMillis() ) {
                LOGGER.debug( "FPS: " + frames + ", UPS: " + ticks );
                frames = 0;
                ticks = 0;
                lastTimer1 += 1000;
            }

        }
    }

    private boolean isRunning() {
        return running;
    }

}
