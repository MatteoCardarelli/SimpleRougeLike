package com.github.matteoCarda.simplerougelike.service;

import com.github.matteoCarda.simplerougelike.model.entity.Item;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.model.entity.PotionItem;
import com.github.matteoCarda.simplerougelike.behavior.item.ItemUseBehavior;


/**
 * Gestisce le interazioni con gli oggetti {@link Item}.
 */
public class ItemService {

    private final CharacterService characterService;

    public ItemService(CharacterService characterService) {
        this.characterService = characterService;
    }

    /**
     * Metodo principale per l'uso di un oggetto.
     * Esegue l'azione dell'oggetto e lo rimuove se consumabile.
     * @param player Il giocatore che usa l'oggetto.
     * @param item l'oggetto da utilizzare.
     * @retrun true se l'oggetto è stato usato con successo.
     */
    public boolean useItem(Player player, Item item) {
        if (player == null || item == null || !player.getInventory().contains(item)) {return false;}
        ItemUseBehavior behavior = item.getUseBehavior();
        if (behavior == null) return false;
        boolean success = behavior.use(item, player, this.characterService);
        if (success && isConsumable(item)) {
            player.getInventory().remove(item);
        }
        return success;
    }

    private boolean isConsumable(Item item) {
        return item instanceof PotionItem;
    }

    /**
     * Logica eseguita quando un giocatore raccoglie un oggetto dalla mappa.
     * Attualmente, si limita ad aggiungere l'oggetto all'inventario.
     *
     * @param item L'oggetto da raccogliere.
     * @param player Il giocatore che raccoglie l'oggetto.
     */
    public void onPickup(Item item, Player player) {
        // La logica di base è aggiungere l'oggetto all'inventario.
        boolean itemTaken = characterService.takeItem(player, item);

        if (itemTaken) {
            System.out.println(player.getClass().getSimpleName() + " ha raccolto: " + item.getName());
        }

        // In futuro, si potrebbero aggiungere logiche specifiche per tipo di oggetto,
        // es. per oggetti che si attivano immediatamente al contatto.
    }
}
