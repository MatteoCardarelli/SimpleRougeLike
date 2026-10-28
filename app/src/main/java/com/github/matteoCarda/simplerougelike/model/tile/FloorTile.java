package com.github.matteoCarda.simplerougelike.model.tile;

/** Una {@link Tile} calpestabile che rappresenta il pavimento. */
public class FloorTile extends Tile {

    /**
     * Costruttore.
     *
     * @param x Coordinata x.
     * @param y Coordinata y.
     */
    public FloorTile(int x, int y) {
        super(x, y);
    }

    /**
     * Il pavimento è sempre calpestabile.
     *
     * @return Sempre true.
     */
    @Override
    public boolean isWalkable() {
        return true;
    }
}
