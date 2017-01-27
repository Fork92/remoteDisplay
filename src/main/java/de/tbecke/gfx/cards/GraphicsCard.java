package de.tbecke.gfx.cards;

/**
 * Created by tbecke on 17.01.17.
 */
public abstract class GraphicsCard {

    public int[] framebuffer;

    public abstract void render();
    public abstract void tick();

    public abstract void setMode( Mode mode );

    public abstract void setRAM( int addr, char value );
    public abstract void setRAM(int addr, String msg, char flag);
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract int getMaxRam();
}
