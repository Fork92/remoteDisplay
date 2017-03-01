package de.tbecke.components;

import de.tbecke.components.cards.utils.RamException;

public class RAM {

    public static final RAM INSTANCE = new RAM();
    private static final int SIZE = 1_000_000;
    private byte[] memory;

    private RAM() {
        memory = new byte[SIZE];
        clear();
    }

    public void clear() {
        for( int i = 0xB0000; i < memory.length; i++ ) {
            memory[i] = 0;
        }
    }

    public void write( int address, byte[] value ) throws RamException {
        if( address < 0 && address + value.length >= size() )
            throw new RamException();

        System.arraycopy( value, 0, memory, address, value.length );

    }

    public int size() {
        return SIZE;
    }

    public int read( int address ) throws RamException {
        if( address < 0 && address >= SIZE )
            throw new RamException();

        return memory[address] & 0xFF;
    }

}
