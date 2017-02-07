package de.tbecke.net;

import de.tbecke.gfx.cards.CardManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer implements Runnable {

    private final Logger logger;

    private ServerSocket serverSocket;
    private CardManager cardManager;

    private BufferedWriter bufferedWriter = null;
    private Socket client = null;
    private boolean running;

    public TCPServer( String ip, int port, CardManager cardManager ) {

        this.cardManager = cardManager;

        this.logger = LogManager.getLogger( TCPServer.class );
        this.running = true;

        try {
            InetAddress address = InetAddress.getByName( ip );
            InetSocketAddress bindAddr = new InetSocketAddress( address, port );
            this.serverSocket = new ServerSocket();
            this.serverSocket.bind( bindAddr );
            this.logger.info( "Server started on address: " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort() );
        } catch( IOException e ) {
            this.logger.error( e );
        }

    }

    @Override
    public void run() {
        try {
            while( this.running ) {
                this.client = serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( client.getInputStream() ) );
                this.bufferedWriter = new BufferedWriter( new OutputStreamWriter( client.getOutputStream() ) );

                String nextLine;

                while( !this.client.isClosed() ) {
                    if( ( nextLine = bufferedReader.readLine() ) != null ) {
                        this.getCommands( nextLine.toUpperCase().split( " " ) );

                        this.bufferedWriter.write( '\n' );
                        this.bufferedWriter.flush();
                    }
                }
            }
        } catch( IOException e ) {
            this.logger.error( "Client closed connection: ", e );
        }

        logger.debug( "Server shutting down" );
    }

    private void getCommands( String[] str ) throws IOException {

        switch( str[0] ) {
            case "CLEAR":
                this.cardManager.getCurrent().clear();
                break;
            case "MEM":
                this.mem( str );
                break;
            case "REG":
                this.reg( str );
                break;
            case "GC":
                this.cardManager.setCurrent( str[1] );
                break;
            case "HELP":
                this.printHelp( str );
                break;
            case "QUITE":
                this.quit();
                break;
            case "CLOSE":
                this.close();
                break;
            default:
                this.bufferedWriter.write( "undefined Command: \n\t Use \"HELP [Command]\" for help" );
        }
    }

    private void quit() throws IOException {
        this.bufferedWriter.write( "Shutdown server" );
        this.close();
        this.running = false;
    }

    private void close() throws IOException {
        this.bufferedWriter.write( "Close connection" );
        client.close();
    }

    private void mem( String[] args ) throws IOException {
        if( args.length > 1 && !this.checkOutofBounds( args ) ) {
            int start = Integer.parseInt( args[1], 16 );
            byte[] val = new byte[args.length - 2];
            for( int i = 0; i < val.length; i++ ) {
                if( args[i + 2].length() != 2 ) {
                    bufferedWriter.write( "wert zu groß oder zu klein: " + args[i + 2].length() );
                    return;
                }

                val[i] = (byte) Integer.parseInt( args[i + 2], 16 );
            }

            this.cardManager.getCurrent().setRAM( start, val );

        } else if( args.length > 1 ) {
            bufferedWriter.write( "Memory address out of bounds: 0x" + args[1] );
        } else {
            bufferedWriter.write( this.cardManager.getCurrent().getMaxRam() );
        }
    }

    private boolean checkOutofBounds( String[] args ) {
        boolean ret = false;

        int start = Integer.parseInt( args[1], 16 );
        int end = Integer.parseInt( args[1], 16 ) + ( args.length - 2 );
        int maxRam = Integer.parseInt( this.cardManager.getCurrent().getMaxRam().substring( 2 ), 16 );

        if( start < 0 || start > maxRam || end < 0 || end > maxRam )
            ret = true;

        return ret;
    }

    private void reg( String[] args ) throws IOException {
        if( args.length == 3 ) {
            this.cardManager.getCurrent().writeRegister( args[1].replace( 'X', 'x' ), (byte) Integer.parseInt( args[2] ) );
        } else if( args.length == 2 ) {
            bufferedWriter.write( this.cardManager.getCurrent().readRegister( args[1].replace( 'X', 'x' ) ) );
        } else {
            bufferedWriter.write( this.cardManager.getCurrent().readRegister() );
        }

    }

    private void printHelp( String[] args ) {
        String msg = "Available commands are:\n" +
            "\t MEM \t[Addr]([Addr], ...) [Status]\n" +
            "\t MODE \t [FLAG]";

        if( args.length == 2 ) {

            logger.debug( args[1] );

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
                default:
                    break;
            }
        } else if( args.length > 2 ) {
            msg = "To Many Arguments\n\n" + msg;
        }


        try {
            bufferedWriter.write( msg );
            bufferedWriter.flush();
        } catch( IOException e ) {
            logger.error( e );
        }
    }

}
