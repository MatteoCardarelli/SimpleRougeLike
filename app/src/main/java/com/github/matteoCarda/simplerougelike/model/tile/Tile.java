package com.github.matteoCarda.simplerougelike.model.tile;

/**
 * Classe base astratta per una singola cella della mappa di gioco. Definisce la posizione e le
 * proprietà fondamentali come la calpestabilità.
 */
public abstract class Tile {
    protected int x;
    protected int y;
    protected boolean isVisible;

    /**
     * Costruttore.
     *
     * @param x Coordinata x.
     * @param y Coordinata y.
     */
    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * Flag che indica se la casella è stata scoperta (esplorata) dal giocatore. Usato per il "Fog
     * of War" persistente.
     */
    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    /**
     * Definisce se un'entità può passare attraverso questa cella.
     *
     * @return true se la cella è calpestabile, false altrimenti.
     */
    public abstract boolean isWalkable();
}
