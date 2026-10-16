package com.github.matteoCarda.simplerougelike.behavior.item;

import com.github.matteoCarda.simplerougelike.model.entity.Item;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.service.CharacterService;

/**
 * Interfaccia per il pattern Strategy.
 * Definisce il comportamento di un oggetto quando viene usato.
 */
public interface ItemUseBehavior {
    /**
     * Esegue l'azione.
     * @param item L'oggetto che possiede questo comportamento.
     * @param player Il giocatore che sta usando l'oggetto.
     * @param characterService Il servizio per eseguire le azioni sui personaggi.
     * @return true se l'azione ha avuto successo.
     */
    boolean use(Item item, Player player, CharacterService characterService);
}
    