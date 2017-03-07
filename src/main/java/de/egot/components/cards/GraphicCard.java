package de.egot.components.cards;

import de.egot.utils.Renderable;

public interface GraphicCard extends Renderable {

    int getWidth();

    int getHeight();

    int[] getFramebuffer();

    void tick();
}
