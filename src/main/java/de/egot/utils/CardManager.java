package de.egot.utils;

import de.egot.components.cards.CGA;
import de.egot.components.cards.GraphicCard;
import de.egot.components.cards.MDA;

import java.util.HashMap;
import java.util.Map;

public class CardManager {

    private static CardManager instance;
    private final Map<String, GraphicCard> cards;
    private GraphicCard current;

    private CardManager() {
        this.cards = new HashMap<>();
        this.cards.put( "MDA", new MDA() );
        this.cards.put( "CGA", new CGA() );
    }

    public static CardManager getInstance() {
        if( instance == null ) {
            instance = new CardManager();
        }

        return instance;
    }

    public GraphicCard getCurrent() {
        return this.current;
    }

    public void setCurrent( String id ) {
        if( this.cards.containsKey( id ) ) {
            this.current = this.cards.get( id );
        }

    }

}
