package com.github.matteoCarda.simplerougelike.model.tile;

/** Rappresenta una casella di tipo "Muro" sulla mappa. Non è mai calpestabile. */
public class WallTile extends Tile {

    /**
     * Costruttore per una casella Muro.
     *
     * @param x La coordinata x della casella.
     * @param y La coordinata y della casella.
     */
    public WallTile(int x, int y) {
        super(x, y);
    }

    /**
     * Determina se un personaggio può camminare su questa casella.
     *
     * @return false, perché il muro non è mai calpestabile.
     */
    @Override
    public boolean isWalkable() {
        return false;
    }
}
