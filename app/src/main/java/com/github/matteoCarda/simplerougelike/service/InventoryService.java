package com.github.matteoCarda.simplerougelike.service;

import com.github.matteoCarda.simplerougelike.model.entity.Item;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.model.entity.PotionItem;

/**
 * Servizio che gestisce la logica di utilizzo degli oggetti dall'inventario.
 */
public class InventoryService {

    private final CharacterService characterService;

    public InventoryService(CharacterService characterService) {
        this.characterService = characterService;
    }

    /**
     * Applica l'effetto di un oggetto al giocatore e lo rimuove se è consumabile.
     * @param player Il giocatore che usa l'oggetto.
     * @param item L'oggetto da utilizzare.
     * @return true se l'oggetto è stato usato con successo, false altrimenti.
     */
    public boolean useItem(Player player, Item item) {
        if (player == null || item == null || !player.getInventory().contains(item)) {
            return false;
        }

        boolean success = false;

        // Qui mettiamo la logica specifica per tipo di oggetto.
        // Questo è il punto centrale del servizio.
        if (item instanceof PotionItem) {
            PotionItem potion = (PotionItem) item;
            characterService.heal(player, potion.getHealingAmount());
            success = true;
        }
        // else if (item instanceof ScrollOfTeleportation) { ... logica per il teletrasporto ... }
        // else if (item instanceof MagicMissileWand) { ... logica per il danno ... }


        // Se l'oggetto è stato usato con successo e se è consumabile, rimuovilo.
        if (success && isConsumable(item)) {
            player.getInventory().remove(item);
        }

        return success;
    }

    /**
     * Metodo helper per determinare se un oggetto scompare dopo l'uso.
     * @param item L'oggetto da controllare.
     * @return true se l'oggetto è consumabile.
     */
    private boolean isConsumable(Item item) {
        // Per ora, solo le pozioni sono consumabili.
        return item instanceof PotionItem;
    }
}
    