package de.tbecke.gfx.cards;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public abstract class GraphicsCard {

    private static final Logger LOGGER = LogManager.getLogger( GraphicsCard.class );

    private final String name;
    int[] framebuffer;
    byte[] ram;
    private Map<String, Byte> register;

    GraphicsCard( int ramLength, String name ) {
        ram = new byte[ramLength];
        register = new HashMap<>();
        this.name = name;
    }

    public abstract void render();

    public abstract void tick();

    public void setRAM( int addr, byte[] value ) {
        for( byte aValue : value ) {
            ram[addr++] = aValue;
        }
    }

    public void clear() {
        for( int i = 0; i < ram.length; i++ ) {
            ram[i] = 0;
        }
    }

    public void writeRegister( String addr, byte reg ) {
        if( register.containsKey( addr ) ) {
            byte old = register.get( addr );
            LOGGER.debug( "Old reg: " + old + ", new reg: " + ( old ^ reg ) );
            old ^= reg;
            register.replace( addr, old );
        }
    }

    boolean isBitSet( String addr, byte bit ) {
        return ( register.containsKey( addr ) ) && ( register.get( addr ) & bit ) > 0;
    }

    public String readRegister( String addr ) {
        StringBuilder ret = new StringBuilder();

        if( register.containsKey( addr ) ) {
            ret.append( "Register " ).append( addr ).append( ": " ).append( String.format( "%8s", Integer.toBinaryString( register.get( addr ) & 0xFF ) ).replace( ' ', '0' ) );
        } else {
            ret.append( "Register not Found: " ).append( addr );
        }

        return ret.toString();
    }

    public String readRegister() {
        StringBuilder ret = new StringBuilder();

        for( Map.Entry<String, Byte> entry : register.entrySet() ) {
            ret.append( "Register: " ).append( entry.getKey() ).append( ", Value:" ).append( String.format( "%8s", Integer.toBinaryString( entry.getValue() & 0xFF ) ).replace( ' ', '0' ) ).append( "\n" );
        }

        return ret.toString();
    }

    void addRegister( String addr ) {
        if( !register.containsKey( addr ) ) {
            register.put( addr, (byte) 0 );
        }
    }

    public abstract int getWidth();

    public abstract int getHeight();

    public String getMaxRam() {
        return "0x" + Integer.toHexString( ram.length );
    }

    public int[] getFramebuffer() {
        return framebuffer;
    }

    public String getName() {
        return name;
    }

}
