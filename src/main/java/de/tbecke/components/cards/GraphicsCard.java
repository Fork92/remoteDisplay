package de.tbecke.components.cards;

import de.tbecke.gfx.utils.Renderable;

public interface GraphicsCard extends Renderable {

    int getWidth();

    int getHeight();

    int[] getFramebuffer();
}
