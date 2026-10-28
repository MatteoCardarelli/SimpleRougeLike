package io.github.matteocardarelli.simpleroguelike.generation;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorBuilder;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.dungeon.PlayerDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Mette player, nemici e oggetti su un terreno, secondo le regole di comparsa: ogni nemico ad
 * almeno {@code minSpawnDistance} passi dal player, mai due attori o due oggetti nella stessa
 * cella, mai un oggetto sotto un attore. Il tipo di ogni nemico e di ogni oggetto si sceglie a
 * caso, in proporzione al suo peso.
 *
 * <p>Gli id si assegnano solo a piano riuscito, in quest'ordine: player, nemici, oggetti.
 */
public final class FloorPopulator {

    private final ItemCatalog items;
    private final EnemyCatalog enemies;
    private final PlayerDefinition player;

    public FloorPopulator(ItemCatalog items, EnemyCatalog enemies, PlayerDefinition player) {
        this.items = items;
        this.enemies = enemies;
        this.player = player;
    }

    /**
     * Crea il piano.
     *
     * @throws FloorGenerationException se il terreno non ha abbastanza celle per la specifica
     */
    public Floor populate(
            Grid<TileType> terrain, FloorSpec spec, RandomSource random, EntityIdSequence ids) {
        List<Position> free = new ArrayList<>();
        for (Position cell : terrain.positions()) {
            if (terrain.get(cell).isWalkable()) {
                free.add(cell);
            }
        }
        if (free.isEmpty()) {
            throw new FloorGenerationException("Il terreno non ha celle attraversabili");
        }
        Position playerAt = free.remove(random.nextInt(free.size()));

        List<Position> farEnough = new ArrayList<>();
        for (Position cell : free) {
            if (cell.distanceTo(playerAt) >= spec.minSpawnDistance()) {
                farEnough.add(cell);
            }
        }
        List<Position> enemyCells = take(farEnough, spec.enemyCount(), random, "nemici");
        free.removeAll(enemyCells);
        List<EnemyDefinition> enemyTypes = new ArrayList<>();
        for (int i = 0; i < enemyCells.size(); i++) {
            enemyTypes.add(
                    WeightedPicker.pick(enemies.all(), EnemyDefinition::spawnWeight, random));
        }

        List<Position> itemCells = take(free, spec.itemCount(), random, "oggetti");
        List<ItemDefinition> itemTypes = new ArrayList<>();
        for (int i = 0; i < itemCells.size(); i++) {
            itemTypes.add(WeightedPicker.pick(items.all(), ItemDefinition::spawnWeight, random));
        }

        FloorBuilder builder =
                new FloorBuilder()
                        .terrain(terrain)
                        .player(new Player(ids.next(), player), playerAt);
        for (int i = 0; i < enemyCells.size(); i++) {
            builder.enemy(new Enemy(ids.next(), enemyTypes.get(i)), enemyCells.get(i));
        }
        for (int i = 0; i < itemCells.size(); i++) {
            builder.item(new Item(ids.next(), itemTypes.get(i)), itemCells.get(i));
        }
        return builder.build();
    }

    /** Toglie {@code count} celle a caso da {@code cells} e le restituisce. */
    private static List<Position> take(
            List<Position> cells, int count, RandomSource random, String what) {
        if (cells.size() < count) {
            throw new FloorGenerationException(
                    "Servono " + count + " celle per i " + what + ", ce ne sono " + cells.size());
        }
        List<Position> taken = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            taken.add(cells.remove(random.nextInt(cells.size())));
        }
        return taken;
    }
}
