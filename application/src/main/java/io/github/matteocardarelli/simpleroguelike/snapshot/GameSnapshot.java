package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;
import java.util.Optional;

/**
 * La partita dopo un turno, come la vede il player: le celle mai viste non rivelano niente, e
 * nemici e oggetti compaiono solo nelle celle visibili adesso. Immutabile.
 *
 * @param seed il seed della partita, per riprodurla
 * @param turn il turno in corso, da 1
 * @param depth il piano in cui si gioca, da 1
 * @param floors quanti piani ha la partita
 * @param danger l'ombra di un nemico visibile in aria, dove atterrerà al prossimo turno: chi ci
 *     resta viene schiacciato
 * @param ware la merce del negozio su cui sta il player, se c'è
 * @param visibleEnemies i nemici vivi nelle celle visibili, in ordine di id
 * @param visibleItems gli oggetti a terra nelle celle visibili
 * @param inventory gli oggetti del player, in ordine di raccolta
 * @param fixtures gli oggetti fissi nelle celle esplorate, come il player li ha visti l'ultima
 *     volta: chi li ha già visti li ricorda anche fuori dal campo visivo
 */
public record GameSnapshot(
        long seed,
        int turn,
        Phase phase,
        Grid<CellView> cells,
        PlayerView player,
        List<ActorView> visibleEnemies,
        List<ItemView> visibleItems,
        List<InventoryEntry> inventory,
        List<FixtureView> fixtures,
        int depth,
        int floors,
        List<Position> danger,
        Optional<WareView> ware,
        List<Position> shop) {

    public GameSnapshot {
        visibleEnemies = List.copyOf(visibleEnemies);
        visibleItems = List.copyOf(visibleItems);
        inventory = List.copyOf(inventory);
        fixtures = List.copyOf(fixtures);
        danger = List.copyOf(danger);
        shop = List.copyOf(shop);
    }
}
