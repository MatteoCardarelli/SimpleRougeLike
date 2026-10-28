package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;

/**
 * L'unico modo di creare un {@link Floor}. {@link #build()} controlla le invarianti del piano:
 * attori e oggetti dentro il terreno e su celle attraversabili, mai due attori o due oggetti nella
 * stessa cella, id tutti diversi.
 */
public final class FloorBuilder {

    private @Nullable Grid<TileType> terrain;
    private @Nullable Player player;
    private final TreeMap<EntityId, Enemy> enemies = new TreeMap<>();
    private final TreeMap<EntityId, Position> positions = new TreeMap<>();
    private final LinkedHashMap<Position, Item> groundItems = new LinkedHashMap<>();
    private final TreeSet<EntityId> itemIds = new TreeSet<>();

    public FloorBuilder terrain(Grid<TileType> terrain) {
        this.terrain = terrain;
        return this;
    }

    public FloorBuilder player(Player player, Position at) {
        if (this.player != null) {
            throw new IllegalStateException("Il player è già sul piano");
        }
        placeActor(player.id(), at);
        this.player = player;
        return this;
    }

    public FloorBuilder enemy(Enemy enemy, Position at) {
        placeActor(enemy.id(), at);
        enemies.put(enemy.id(), enemy);
        return this;
    }

    public FloorBuilder item(Item item, Position at) {
        requireNewId(item.id());
        if (groundItems.containsKey(at)) {
            throw new IllegalArgumentException("C'è già un oggetto in " + at);
        }
        groundItems.put(at, item);
        itemIds.add(item.id());
        return this;
    }

    /**
     * Crea il piano. Il builder si può riusare: il piano copia quello che ha ricevuto.
     *
     * @throws IllegalStateException se mancano il terreno o il player, o se un attore o un oggetto
     *     è fuori dal terreno o su una cella che non si può attraversare
     */
    public Floor build() {
        Grid<TileType> terrain = this.terrain;
        Player player = this.player;
        if (terrain == null || player == null) {
            throw new IllegalStateException("Un piano ha bisogno di terreno e player");
        }
        positions.forEach((id, at) -> requireWalkable(terrain, at, id.toString()));
        groundItems.forEach((at, item) -> requireWalkable(terrain, at, item.id().toString()));
        return new Floor(terrain, player, enemies, positions, groundItems);
    }

    private void placeActor(EntityId id, Position at) {
        requireNewId(id);
        if (positions.containsValue(at)) {
            throw new IllegalArgumentException("C'è già un attore in " + at);
        }
        positions.put(id, at);
    }

    private void requireNewId(EntityId id) {
        if (positions.containsKey(id) || itemIds.contains(id)) {
            throw new IllegalArgumentException("Id già usato: " + id);
        }
    }

    private static void requireWalkable(Grid<TileType> terrain, Position at, String what) {
        if (!terrain.isInside(at) || !terrain.get(at).isWalkable()) {
            throw new IllegalStateException(what + " è in " + at + ", che non si può attraversare");
        }
    }
}
