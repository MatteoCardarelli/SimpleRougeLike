package com.github.matteoCarda.simplerougelike.model.entity;

import com.github.matteoCarda.simplerougelike.behavior.item.ItemUseBehavior;
import com.github.matteoCarda.simplerougelike.service.ItemService;

/**
 * Classe base astratta per gli oggetti raccoglibili nel gioco.
 * Fornisce un nome e una posizione.
 */
public abstract class Item extends GameObject {
    protected String name;
    protected boolean consumable;
    protected final ItemUseBehavior useBehavior;



    /**
     * Costruttore.
     * @param x Coordinata x iniziale.
     * @param y Coordinata y iniziale.
     * @param name Nome dell'oggetto.
     */
    public Item( int x, int y, String name, ItemUseBehavior useBehavior) {
        super(x, y);
        this.name = name;
        this.useBehavior = useBehavior;
    }

    public String getName() {
        return name;
    }

    public boolean isConsumable() { return consumable; }


    public ItemUseBehavior getUseBehavior() {
        return useBehavior;
    }
}
