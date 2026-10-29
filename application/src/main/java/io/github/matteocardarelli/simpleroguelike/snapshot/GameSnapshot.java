package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.Grid;
import java.util.List;

/**
 * La partita dopo un turno, come la vede il player: le celle mai viste non rivelano niente, e
 * nemici e oggetti compaiono solo nelle celle visibili adesso. Immutabile.
 *
 * @param seed il seed della partita, per riprodurla
 * @param turn il turno in corso, da 1
 * @param visibleEnemies i nemici vivi nelle celle visibili, in ordine di id
 * @param visibleItems gli oggetti a terra nelle celle visibili
 * @param inventory gli oggetti del player, in ordine di raccolta
 */
public record GameSnapshot(
        long seed,
        int turn,
        Phase phase,
        Grid<CellView> cells,
        PlayerView player,
        List<ActorView> visibleEnemies,
        List<ItemView> visibleItems,
        List<InventoryEntry> inventory) {

    public GameSnapshot {
        visibleEnemies = List.copyOf(visibleEnemies);
        visibleItems = List.copyOf(visibleItems);
        inventory = List.copyOf(inventory);
    }
}
