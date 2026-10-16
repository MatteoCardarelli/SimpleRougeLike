package com.github.matteoCarda.simplerougelike.model.entity;

import com.github.matteoCarda.simplerougelike.service.ItemService;

/**
 * Classe base astratta per gli oggetti raccoglibili nel gioco.
 * Fornisce un nome e una posizione.
 */
public abstract class Item extends GameObject {
    protected String name;

    /**
     * Costruttore.
     * @param x Coordinata x iniziale.
     * @param y Coordinata y iniziale.
     * @param name Nome dell'oggetto.
     */
    public Item( int x, int y, String name) {
        super(x, y);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Esegue l'azione specifica di questo oggetto, delegando la logica
     * a ItemService.
     * @param player Il giocatore che sta usando l'oggetto.
     * @param itemService L'istanza del servizio per eseguire l'azione.
     * @return true se l'azione ha avuto successo.
     */
    public abstract boolean use(Player player, ItemService itemService);

}
