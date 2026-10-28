package com.github.matteoCarda.simplerougelike.model.entity;

/**
 * Classe base astratta per tutte le entità "viventi" del gioco (es. Player, Enemy). Contiene le
 * statistiche fondamentali come salute e potenza d'attacco.
 */
public abstract class Character extends GameObject {
    protected int health;
    protected int maxHealth;
    protected double attackPower;

    /**
     * Costruttore.
     *
     * @param x Coordinata x iniziale.
     * @param y Coordinata y iniziale.
     * @param health Salute iniziale (e massima).
     * @param attackPower Potenza d'attacco base.
     */
    public Character(int x, int y, int health, double attackPower) {
        super(x, y);
        this.health = health;
        this.maxHealth = health;
        this.attackPower = attackPower;
    }

    // --- Getters e Setters ---

    public int getHealth() {
        return this.health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getMaxHealth() {
        return this.maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public double getAttackPower() {
        return this.attackPower;
    }

    public void setAttackPower(double attackPower) {
        this.attackPower = attackPower;
    }
}
