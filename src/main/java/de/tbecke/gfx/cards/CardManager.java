package de.tbecke.gfx.cards;

import de.tbecke.gfx.Display;

import java.util.HashMap;
import java.util.Map;

public class CardManager {

    private Map<String, GraphicsCard> cards;
    private GraphicsCard current;
    private Display display;

    public CardManager( Display display ) {
        this.cards = new HashMap<>();
        this.cards.put( "MDA", new MDA() );
        this.cards.put( "CGA", new CGA() );
        this.display = display;
        this.setCurrent( "CGA" );
    }

    public Map<String, GraphicsCard> getAvailableCards() {
        return cards;
    }

    public GraphicsCard getCurrent() {
        return this.current;
    }

    public void setCurrent( String id ) {
        if( this.cards.containsKey( id ) ) {
            this.current = this.cards.get( id );
            display.setDisplay( current.getWidth(), current.getHeight() );
        }

    }

}
