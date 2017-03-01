package de.tbecke.gfx;

import de.tbecke.components.cards.CGA;
import de.tbecke.components.cards.GraphicsCard;
import de.tbecke.components.cards.MDA;

import java.util.HashMap;
import java.util.Map;

public class CardManager {

    private static CardManager instance;
    private final Map<String, GraphicsCard> cards;
    private GraphicsCard current;

    private CardManager() {
        this.cards = new HashMap<>();
        this.cards.put( "MDA", new MDA() );
        this.cards.put( "CGA", new CGA() );
        this.setCurrent( "MDA" );
    }

    public static CardManager getInstance() {
        if( instance == null ) {
            instance = new CardManager();
        }

        return instance;
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
        }

    }

}
