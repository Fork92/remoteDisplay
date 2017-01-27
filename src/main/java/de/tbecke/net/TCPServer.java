package de.tbecke.net;

import de.tbecke.gfx.cards.GraphicsCard;
import de.tbecke.gfx.cards.MDA;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by tbecke on 20.12.16.
 */
public class TCPServer implements Runnable {

    private final Logger LOGGER = LogManager.getLogger( TCPServer.class );

    private ServerSocket serverSocket;
    private GraphicsCard graphicsCard;

    private Socket client;

    private BufferedReader bufferedReader = null;
    private BufferedWriter bufferedWriter = null;

    public TCPServer( int port, GraphicsCard graphicsCard ) {

        this.graphicsCard = graphicsCard;

        try {
            serverSocket = new ServerSocket( port );
            LOGGER.info( "Server started on address: " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort() );
        } catch( IOException e ) {
            LOGGER.error( e );
        }

        graphicsCard.setRAM( 0, "Starting server...", (char) ( MDA.UNDERLINE | MDA.HIGHSENSITY ) );

    }

    @Override
    public void run() {
        client = null;
        try {
            graphicsCard.setRAM( 160, "Server Started, waiting for Connection...", (char) ( MDA.UNDERLINE | MDA.HIGHSENSITY ) );
            client = serverSocket.accept();
        } catch( IOException e ) {
            LOGGER.error( e );
        }
        graphicsCard.setRAM( 320, "Client connected, on Port: " + client.getLocalPort(), (char) ( MDA.UNDERLINE | MDA.HIGHSENSITY ) );

        while( !client.isClosed() && client.isConnected() ) {
            try {
                bufferedReader = new BufferedReader( new InputStreamReader( client.getInputStream() ) );
                bufferedWriter = new BufferedWriter( new OutputStreamWriter( client.getOutputStream() ) );

                String nextLine;

                if( ( nextLine = bufferedReader.readLine() ) != null ) {

                    String[] switchStr = nextLine.toUpperCase().split( " " );

                    switch( switchStr[0] ) {
                        case "CLEAR":
                            this.clear();
                            break;
                        case "MEM":
                            this.write( switchStr );
                            break;
                        case "MODE":
                            break;
                        case "HELP":
                            this.printHelp( switchStr );
                            break;
                        case "CLOSE":
                            this.bufferedWriter.write( "Close connection" );
                            this.client.close();
                            break;
                        default:
                            this.bufferedWriter.write( "undefined Command: \n\t Use \"HELP [Command]\" for help" );
                    }
                    this.bufferedWriter.flush();

                }


            } catch( IOException e ) {
                LOGGER.error( e );
            }
        }
    }

    private void clear() {
        for(int i = 0; i < graphicsCard.getMaxRam(); i++) {
            graphicsCard.setRAM( i, ' ', ' ' );
        }
    }

    private void write( String[] args) {
        int start = Integer.parseInt( args[1] );
        LOGGER.debug( args.length );

            LOGGER.debug( "Write at address: " + (start) + ", value with: " + args[2] + ", and flag: " + Integer.toHexString( Integer.parseInt( args[args.length - 1] )));
            graphicsCard.setRAM( start, args[2], (char)(Integer.parseInt( args[args.length - 1] ) ));
    }

    private void printHelp( String[] args ) {
        String msg = "Available commands are:\n" +
            "\t POINT \t[x][y] [Status]\n" +
            "\t LINE \t[Sx][Sy] [Ex][Ey] [Status]\n" +
            "\t CIRCLE \t[x][y][r] [Status]\n" +
            "\t MEM \t[Addr]([Addr], ...) [Status]\n" +
            "\t MODE \t [FLAG]\n";

        if( args.length == 2 ) {

            LOGGER.debug( args[1] );

            switch( args[1] ) {
                case "POINT":
                    msg = "Help for command POINT:\n" +
                        "\t [x]: the x Point on the Display\n" +
                        "\t [y]: the y Point on the Display\n" +
                        "\t [status]: the status Flag for this Element on the Display\n";
                    break;
                case "LINE":
                    msg = "Help for command LINE:\n" +
                        "\t [Sx]: the start x Point on the Display\n" +
                        "\t [Sy]: the start y Point on the Display\n" +
                        "\t [Ex]: the end x Point on the Display\n" +
                        "\t [Ey]: the end y Point on the Display\n" +
                        "\t [status]: the status Flag for this Element on the Display\n";
                    break;
                case "CIRCLE":
                    msg = "Help for command CIRCLE:\n" +
                        "\t [x]: the x Point on the Display\n" +
                        "\t [y]: the y Point on the Display\n" +
                        "\t [r]: the the radius for the Circle\n" +
                        "\t [status]: the status Flag for this Element on the Display\n";
                    break;
                case "MEM":
                    msg = "Help for command MEM:\n" +
                        "\t [Addr]: the Address to be manipulated. The address has the following format: 0x...(Hexcode)\n" +
                        "\t [addr-addr]: is also possible and specifies a memory area.\n" +
                        "\t [status]: the status Flag for this Element on the Display\n";
                    break;
                case "STATUS":
                    msg = "Help for the STATUS FLAG:\n" +
                        "\t The status is a [TODO]\n";
                    break;
                case "MODE":
                    msg = "MODE is usec to switch from the Text mode to the Graphics mode and back:\n" +
                        "\t [FLAG]: 0 for text mode and 1 for Graphics mode";
                    break;
            }
        } else if( args.length > 2 ) {
            msg = "To Many Arguments\n\n" + msg;
        }


        try {
            bufferedWriter.write( msg );
            bufferedWriter.flush();
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }

}
