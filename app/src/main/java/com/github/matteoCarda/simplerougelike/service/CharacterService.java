package com.github.matteoCarda.simplerougelike.service;

import com.github.matteoCarda.simplerougelike.model.entity.Character;
import com.github.matteoCarda.simplerougelike.model.entity.Item;
import com.github.matteoCarda.simplerougelike.model.entity.Player;

/**
 * Gestisce le logiche di stato fondamentali per ogni {@link Character}. Es: salute, stato di vita,
 * inventario (per i Player).
 */
public class CharacterService {

    /**
     * Applica danno a un personaggio, con un minimo di 0 alla salute.
     *
     * @param character Il personaggio che subisce il danno.
     * @param damageAmount La quantità di danno da infliggere.
     */
    public void takeDamage(Character character, double damageAmount) {
        if (character == null || !isAlive(character)) {
            return;
        }
        int newHealth = (int) Math.max(0, character.getHealth() - damageAmount);
        character.setHealth(newHealth);
    }

    /**
     * Cura un personaggio, senza superare la sua salute massima.
     *
     * @param character Il personaggio da curare.
     * @param healingAmount La quantità di salute da ripristinare.
     */
    public void heal(Character character, double healingAmount) {
        int newHealth =
                (int) Math.min(character.getMaxHealth(), character.getHealth() + healingAmount);
        character.setHealth(newHealth);
    }

    /**
     * Controlla se un personaggio ha più di 0 punti vita.
     *
     * @param character Il personaggio da controllare.
     * @return true se ha salute > 0, false altrimenti.
     */
    public boolean isAlive(Character character) {
        return character != null && character.getHealth() > 0;
    }

    /**
     * Aggiunge un oggetto all'inventario di un personaggio, se questo è un {@link Player}.
     *
     * @param character Il personaggio che raccoglie l'oggetto.
     * @param item L'oggetto da aggiungere all'inventario.
     * @return true se l'oggetto è stato aggiunto, false altrimenti.
     */
    public boolean takeItem(Character character, Item item) {
        if (character instanceof Player) {
            ((Player) character).getInventory().add(item);
            return true;
        }
        return false;
    }
}
