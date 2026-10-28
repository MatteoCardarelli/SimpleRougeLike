package com.github.matteoCarda.simplerougelike.model.entity;

import java.util.ArrayList;
import java.util.List;

/** Rappresenta il giocatore. Estende {@link Character} con statistiche fisse e un inventario. */
public class Player extends Character {

    // Inventario per gli oggetti raccolti.
    private final List<Item> inventory;
    private int level;
    private int experience;
    private int experienceToNextLevel;

    /**
     * Costruttore.
     *
     * @param x Coordinata x iniziale.
     * @param y Coordinata y iniziale.
     */
    public Player(int x, int y) {
        // Statistiche base per il giocatore.
        super(x, y, 100, 10);
        this.inventory = new ArrayList<>();
        this.level = 1;
        this.experience = 0;
        this.experienceToNextLevel = 100;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    public void setExperienceToNextLevel(int experienceToNextLevel) {
        this.experienceToNextLevel = experienceToNextLevel;
    }

    /** Ritorna l'inventario del giocatore. */
    public List<Item> getInventory() {
        return inventory;
    }
}
