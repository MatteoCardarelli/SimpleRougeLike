package com.github.matteoCarda.simplerougelike.model.entity;

/**
 * Rappresenta un'entità nemica. Estende {@link Character} con statistiche di base fisse e un valore
 * di esperienza.
 */
public class Enemy extends Character {

    private final int experienceValue;

    /**
     * Costruttore.
     *
     * @param x Coordinata x iniziale.
     * @param y Coordinata y iniziale.
     */
    public Enemy(int x, int y) {
        // Statistiche base per un nemico generico: 30 HP, 5 Attack Power.
        super(x, y, 30, 5);
        this.experienceValue = 25; // Valore di esperienza di default
    }

    /** Ritorna il valore di esperienza che questo nemico conferisce quando sconfitto. */
    public int getExperienceValue() {
        return experienceValue;
    }
}
