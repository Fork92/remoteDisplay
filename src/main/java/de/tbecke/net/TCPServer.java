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
    private boolean clientShouldClose = false;

    public TCPServer( String ip, int port, CardManager cardManager ) {

        this.cardManager = cardManager;

        this.logger = LogManager.getLogger( TCPServer.class );
        this.running = true;

        try {
            InetAddress address = InetAddress.getByName( ip );
            InetSocketAddress bindAddr = new InetSocketAddress( address, port );
            this.serverSocket = new ServerSocket();
            this.serverSocket.bind( bindAddr );
            this.logger.info( "Se r v e r  started on address: " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort() );
            this.welcome();
        } catch( IOException e ) {
            this.logger.error( e );
        }

    }

    private void welcome() {
        byte[] msg = ( "Server gestartet auf Adresse: " + this.serverSocket.getInetAddress() + ":" + this.serverSocket.getLocalPort() ).getBytes();
        byte[] ret = new byte[3200];

        for( int i = 0; i < msg.length; i++ ) {
            ret[i * 2] = msg[i];
            ret[i * 2 + 1] = 2;
        }

        this.cardManager.getCurrent().setRAM( 0, ret );
    }

    @Override
    public void run() {
        try {
            while( this.running ) {
                this.client = serverSocket.accept();
                this.clientShouldClose = false;
                BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( client.getInputStream() ) );
                this.bufferedWriter = new BufferedWriter( new OutputStreamWriter( client.getOutputStream() ) );

                String nextLine;

                while( !this.clientShouldClose ) {
                    if( ( nextLine = bufferedReader.readLine() ) != null ) {
                        this.getCommands( nextLine.toUpperCase().split( " " ) );
                        this.bufferedWriter.flush();
                    }
                }
                this.client.close();
            }
            serverSocket.close();
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
                this.printHelp();
                break;
            case "QUIT":
                this.quit();
                break;
            case "CLOSE":
                this.close();
                break;
            case "LIST":
                this.listCards();
                break;
            default:
                this.bufferedWriter.write( "undefined Command: \n\t Use \"HELP [Command]\" for help" );
        }
    }

    private void quit() throws IOException {
        this.bufferedWriter.write( "Shutdown server" );
        this.running = false;
        this.close();
    }

    private void close() throws IOException {
        this.bufferedWriter.write( "Close connection" );
        this.clientShouldClose = true;
    }

    private void mem( String[] args ) throws IOException {
        if( args.length > 1 && !this.checkOutOfBounds( args ) ) {
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
            bufferedWriter.write( "Memory address out of bounds: 0x" + args[1] + "\n" );
        } else {
            bufferedWriter.write( this.cardManager.getCurrent().getMaxRam() + "\n" );
        }
    }

    private boolean checkOutOfBounds( String[] args ) {
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

    private void printHelp() throws IOException {
        String msg = "Available commands are:\n" +
            "\t MEM \t[Addr] ([Status] ...)\n" +
            "\t REG \t ([FLAG])\n" +
            "\t CLEAR\n" +
            "\t GC \t[Cardname] \t\t Available cards are MDA and CGA\n" +
            "\t QUIT \t\t\t\t Closed the connection and shutdown the graphic server\n" +
            "\t CLOSE \t\t\t\t Closed the connection\n" +
            "\t HELP\n";

        bufferedWriter.write( msg );
    }

    private void listCards() throws IOException {
        StringBuilder msg = new StringBuilder( "Available cards are:\n" );

        cardManager.getAvailableCards().forEach( ( k, v ) -> msg.append( v.hasInfo() + "\n" ) );

        bufferedWriter.write( msg.toString() );
    }

}
