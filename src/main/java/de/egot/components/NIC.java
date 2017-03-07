package de.egot.components;

import de.egot.utils.OutOfRamException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NIC implements Runnable {

    private final Logger logger;

    private ServerSocket serverSocket;

    private BufferedWriter bufferedWriter = null;
    private Socket client = null;
    private boolean running;
    private boolean clientShouldClose = false;

    public NIC( String ip, int port ) {

        this.logger = LogManager.getLogger( NIC.class );
        this.running = true;

        try {
            InetAddress address = InetAddress.getByName( ip );
            InetSocketAddress bindAddr = new InetSocketAddress( address, port );
            this.serverSocket = new ServerSocket();
            this.serverSocket.bind( bindAddr );
            this.logger.info( "Server  started on address: " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort() );
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

        try {
            RAM.INSTANCE.write( 0x0B0000, ret );
        } catch( Exception e ) {
            logger.error( e );
        }
    }

    @Override
    public void run() {

        while( this.running ) {
            waitForConnection();

            readLines();

            closeConnection();
        }

        try {
            serverSocket.close();
        } catch( IOException e ) {
            logger.error( "Client closed connection: ", e );
        }
    }

    private void closeConnection() {
        try {
            if( client != null && client.isConnected() ) {
                client.close();
                client = null;
                clientShouldClose = false;
            }
        } catch( IOException e ) {
            logger.error( e );
        }
    }

    private void readLines() {
        String nextLine;

        if( client == null )
            return;

        try {
            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( client.getInputStream() ) );
            while( !clientShouldClose ) {
                if( ( nextLine = bufferedReader.readLine() ) != null ) {
                    getCommands( nextLine.toUpperCase().split( " " ) );
                    bufferedWriter.flush();
                }
            }
        } catch( IOException | OutOfRamException e ) {
            logger.error( "client not connected" );
            logger.error( e );
        }
    }

    private void getCommands( String[] str ) throws IOException, OutOfRamException {

        switch( str[0] ) {
            case "CLEAR":
                RAM.INSTANCE.clear();
                break;
            case "WRITE":
                this.write( str );
                break;
            case "READ":
                this.read( str );
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
            default:
                this.bufferedWriter.write( "undefined Command: \n\t Use \"HELP [Command]\" for help\n" );
                this.printHelp();
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

    private void write( String[] args ) throws IOException {
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

            try {
                RAM.INSTANCE.write( start, val );
            } catch( Exception e ) {
                logger.error( e );
            }

        } else if( args.length > 1 ) {
            bufferedWriter.write( "Memory address out of bounds: 0x" + args[1] + "\n" );
        } else {
            bufferedWriter.write( "Ungültige Anzahl an Argumenten.\n" );
            printHelp();
        }
    }

    private boolean checkOutOfBounds( String[] args ) {
        boolean ret = false;

        int start = Integer.parseInt( args[1], 16 );
        int end = Integer.parseInt( args[1], 16 ) + ( args.length - 2 );
        int maxRam = RAM.INSTANCE.size();

        if( start < 0 || start > maxRam || end < 0 || end > maxRam )
            ret = true;

        return ret;
    }

    private void printHelp() throws IOException {
        String msg = "Available commands are:\n" +
            "\t WRITE \t[Addr] [Byte] [Byte] ...\n" +
            "\t READ \t [Addr]\n" +
            "\t CLEAR\n" +
            "\t QUIT \t\t\t\t Closed the connection and shutdown the graphic server\n" +
            "\t CLOSE \t\t\t\t Closed the connection\n" +
            "\t HELP\n";

        bufferedWriter.write( msg );
    }

    private void read( String[] args ) throws IOException, OutOfRamException {
        StringBuilder builder = new StringBuilder();

        if( args.length == 2 ) {
            builder.append( String.format( "%8s\n", Integer.toBinaryString( RAM.INSTANCE.read( Integer.parseInt( args[1], 16 ) ) ) ).replace( " ", "0" ) );
        }

        bufferedWriter.write( builder.toString() );

    }

    private void waitForConnection() {

        logger.debug( "Wait for new connection..." );

        if( client != null ) {
            return;
        }

        try {
            client = serverSocket.accept();
            bufferedWriter = new BufferedWriter( new OutputStreamWriter( client.getOutputStream() ) );
        } catch( IOException e ) {
            logger.error( e );
        }


    }

    public boolean isRunning() {
        return running;
    }

}
