package com.github.matteoCarda.simplerougelike.model.entity;

/**
 * Classe base per tutti gli oggetti posizionabili sulla mappa. Fornisce coordinate (x,y) e metodi
 * base per la posizione.
 */
public abstract class GameObject {
    protected int x;
    protected int y;

    /**
     * Costruttore.
     *
     * @param x Coordinata x iniziale.
     * @param y Coordinata y iniziale.
     */
    public GameObject(int x, int y) {
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
     * Aggiorna la posizione dell'oggetto.
     *
     * @param x Nuova coordinata x.
     * @param y Nuova coordinata y.
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
