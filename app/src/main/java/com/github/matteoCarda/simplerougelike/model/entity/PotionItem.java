package com.github.matteoCarda.simplerougelike.model.entity;

import com.github.matteoCarda.simplerougelike.behavior.item.HealBehavior;

/**
 * Un oggetto {@link Item} che ripristina la salute del giocatore quando usato.
 */
public class PotionItem extends Item{

    private final int healingAmount;

    /**
     * Costruttore.
     * @param x Coordinata x iniziale.
     * @param y Coordinata y iniziale.
     */
    public PotionItem (int x, int y){
        super(x, y, "Pozione", HealBehavior.INSTANCE);
        // Valore di cura fisso per questo tipo di pozione.
        this.healingAmount = 25;
        this.consumable = true;
    }

    /**
     * Ritorna la quantità di salute ripristinata da questa pozione.
     */
    public double getHealingAmount() {
        return healingAmount;
    }

}
