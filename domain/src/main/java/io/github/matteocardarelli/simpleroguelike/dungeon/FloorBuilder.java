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
 * stessa cella, id tutti diversi. Gli oggetti fissi stanno sul pavimento, tranne la leva che sta in
 * un muro accanto al pavimento; nessun attore su un oggetto fisso che blocca e nessun oggetto a
 * terra su un oggetto fisso.
 */
public final class FloorBuilder {

    private @Nullable Grid<TileType> terrain;
    private @Nullable Player player;
    private final TreeMap<EntityId, Enemy> enemies = new TreeMap<>();
    private final TreeMap<EntityId, Position> positions = new TreeMap<>();
    private final LinkedHashMap<Position, Item> groundItems = new LinkedHashMap<>();
    private final TreeSet<EntityId> itemIds = new TreeSet<>();
    private final LinkedHashMap<Position, Fixture> fixtures = new LinkedHashMap<>();

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

    public FloorBuilder fixture(Fixture fixture, Position at) {
        if (fixtures.containsKey(at)) {
            throw new IllegalArgumentException("C'è già un oggetto fisso in " + at);
        }
        fixtures.put(at, fixture);
        if (fixture.contents().isPresent()) {
            Item inside = fixture.contents().get();
            requireNewId(inside.id());
            itemIds.add(inside.id());
        }
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
        fixtures.forEach(
                (at, fixture) -> {
                    if (fixture.type().onFloor()) {
                        requireWalkable(terrain, at, fixture.kind().value());
                    } else if (!terrain.isInside(at) || terrain.get(at).isWalkable()) {
                        throw new IllegalStateException(
                                fixture.kind().value() + " in " + at + " deve stare in un muro");
                    }
                    if (groundItems.containsKey(at)) {
                        throw new IllegalStateException(
                                "Un oggetto a terra sotto " + fixture.kind().value() + " in " + at);
                    }
                    if (fixture.blocksMovement() && positions.containsValue(at)) {
                        throw new IllegalStateException(
                                "Un attore dentro " + fixture.kind().value() + " in " + at);
                    }
                });
        return new Floor(terrain, player, enemies, positions, groundItems, fixtures);
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
