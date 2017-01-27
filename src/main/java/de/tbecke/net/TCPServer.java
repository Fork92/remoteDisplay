package de.tbecke.net;

import de.tbecke.gfx.cards.CardManager;
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
    private CardManager cardManager;

    private Socket client;

    private BufferedReader bufferedReader = null;
    private BufferedWriter bufferedWriter = null;

    public TCPServer( int port, CardManager cardManager ) {

        this.cardManager = cardManager;

        try {
            serverSocket = new ServerSocket( port );
            LOGGER.info( "Server started on address: " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort() );
        } catch( IOException e ) {
            LOGGER.error( e );
        }

    }

    @Override
    public void run() {
        client = null;
        try {
            client = serverSocket.accept();
        } catch( IOException e ) {
            LOGGER.error( e );
        }

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
                        case "GC":
                            this.changeCard( switchStr );
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

    private void changeCard( String[] args ) {
        if( args.length == 2 ) {
            cardManager.setCurrent( args[1] );
        } else {
            try {
                bufferedWriter.write( "Useage: GC [cardname]" );
                bufferedWriter.flush();
            } catch( IOException e ) {
                e.printStackTrace();
            }
        }

    }

    private void clear() {
        for( int i = 0; i < cardManager.getCurrent().getMaxRam(); i++ ) {
            cardManager.getCurrent().setRAM( i, (char) 0x00 );
        }
    }

    private void write( String[] args) {
        int start = Integer.parseInt( args[1] );
        LOGGER.debug( args.length );

            LOGGER.debug( "Write at address: " + (start) + ", value with: " + args[2] + ", and flag: " + Integer.toHexString( Integer.parseInt( args[args.length - 1] )));
        cardManager.getCurrent().setRAM( start, args[2], (char) ( Integer.parseInt( args[args.length - 1] ) ) );
    }

    private void printHelp( String[] args ) {
        String msg = "Available commands are:\n" +
            "\t MEM \t[Addr]([Addr], ...) [Status]\n" +
            "\t MODE \t [FLAG]\n";

        if( args.length == 2 ) {

            LOGGER.debug( args[1] );

            switch( args[1] ) {
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
