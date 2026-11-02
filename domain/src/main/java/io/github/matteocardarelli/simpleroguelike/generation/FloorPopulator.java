package io.github.matteocardarelli.simpleroguelike.generation;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Fixture;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorBuilder;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.KeyType;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.dungeon.PlayerDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mette oggetti fissi, player, nemici e oggetti su un terreno, secondo le regole di comparsa: prima
 * porte, cancelli con le loro leve e scrigni ({@link FixtureLayout}), poi il player fuori dalle
 * zone chiuse, ogni nemico ad almeno {@code minSpawnDistance} passi dal player, mai due attori o
 * due oggetti nella stessa cella, mai un oggetto sotto un attore o un oggetto fisso. Il tipo di
 * ogni nemico e di ogni oggetto, anche negli scrigni, si sceglie a caso, in proporzione al suo
 * peso. Un nemico che va in branco nasce con i compagni nelle celle libere più vicine, entro
 * {@value #PACK_RADIUS} passi; il numero totale di nemici resta quello della specifica.
 *
 * <p>Gli id si assegnano solo a piano riuscito, in quest'ordine: player, nemici, oggetti, contenuto
 * degli scrigni.
 */
public final class FloorPopulator {

    /** Quanto lontano dal primo nascono i compagni di un branco. */
    static final int PACK_RADIUS = 3;

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
        return populate(terrain, spec, random, ids, Optional.empty());
    }

    /**
     * Crea il piano con un player che esiste già, quando si scende dal piano di prima: tiene id,
     * statistiche, salute e inventario.
     *
     * @throws FloorGenerationException se il terreno non ha abbastanza celle per la specifica
     */
    public Floor populate(
            Grid<TileType> terrain,
            FloorSpec spec,
            RandomSource random,
            EntityIdSequence ids,
            Optional<Player> existing) {
        List<Position> free = new ArrayList<>();
        for (Position cell : terrain.positions()) {
            if (terrain.get(cell).isWalkable()) {
                free.add(cell);
            }
        }
        if (free.isEmpty()) {
            throw new FloorGenerationException("Il terreno non ha celle attraversabili");
        }
        FixtureLayout layout = FixtureLayout.plan(terrain, spec, random);
        free.removeAll(layout.floorCells());
        List<Position> playerCells = new ArrayList<>(free);
        playerCells.removeAll(layout.closed());
        if (playerCells.isEmpty()) {
            throw new FloorGenerationException("Nessuna cella per il player");
        }
        if (layout.stairs().isPresent()) {
            // Le scale non nascono accanto al player: lontane almeno un quarto del piano, se si
            // può.
            Position stairs = layout.stairs().get();
            int far = (terrain.width() + terrain.height()) / 4;
            List<Position> distant = new ArrayList<>();
            for (Position cell : playerCells) {
                if (cell.distanceTo(stairs) >= far) {
                    distant.add(cell);
                }
            }
            if (!distant.isEmpty()) {
                playerCells = distant;
            }
        }
        Position playerAt = playerCells.get(random.nextInt(playerCells.size()));
        free.remove(playerAt);

        List<Position> farEnough = new ArrayList<>();
        for (Position cell : free) {
            if (cell.distanceTo(playerAt) >= spec.minSpawnDistance()) {
                farEnough.add(cell);
            }
        }
        List<Position> chosen = take(farEnough, spec.enemyCount(), random, "nemici");
        free.removeAll(chosen);
        List<Position> enemyCells = new ArrayList<>();
        List<EnemyDefinition> enemyTypes = new ArrayList<>();
        for (Position cell : chosen) {
            if (enemyCells.size() >= spec.enemyCount()) {
                // Le celle scelte e non usate, perché un branco ha preso il loro posto, tornano
                // libere per gli oggetti.
                free.add(cell);
                continue;
            }
            EnemyDefinition type =
                    WeightedPicker.pick(enemies.all(), EnemyDefinition::spawnWeight, random);
            enemyCells.add(cell);
            enemyTypes.add(type);
            for (int member = 1;
                    member < type.packSize() && enemyCells.size() < spec.enemyCount();
                    member++) {
                Optional<Position> near = nearest(free, cell, playerAt, spec);
                if (!near.isPresent()) {
                    break;
                }
                free.remove(near.get());
                enemyCells.add(near.get());
                enemyTypes.add(type);
            }
        }

        List<Position> itemCells = take(free, spec.itemCount(), random, "oggetti");
        List<ItemDefinition> itemTypes = new ArrayList<>();
        for (int i = 0; i < itemCells.size(); i++) {
            itemTypes.add(WeightedPicker.pick(items.all(), ItemDefinition::spawnWeight, random));
        }

        List<Position> keyCells = keyCells(terrain, layout, playerAt, free, random);

        List<ItemDefinition> chestTypes = new ArrayList<>();
        for (int i = 0; i < layout.chests().size(); i++) {
            chestTypes.add(WeightedPicker.pick(items.all(), ItemDefinition::spawnWeight, random));
        }
        FloorBuilder builder =
                new FloorBuilder()
                        .terrain(terrain)
                        .player(existing.orElseGet(() -> new Player(ids.next(), player)), playerAt);
        for (int i = 0; i < enemyCells.size(); i++) {
            builder.enemy(new Enemy(ids.next(), enemyTypes.get(i)), enemyCells.get(i));
        }
        for (int i = 0; i < itemCells.size(); i++) {
            builder.item(new Item(ids.next(), itemTypes.get(i)), itemCells.get(i));
        }
        for (int i = 0; i < layout.chests().size(); i++) {
            builder.fixture(
                    Fixture.chest(Optional.of(new Item(ids.next(), chestTypes.get(i)))),
                    layout.chests().get(i));
        }
        for (FixtureLayout.Gate gate : layout.gates()) {
            builder.fixture(Fixture.gate(), gate.at());
            builder.fixture(Fixture.lever(List.of(gate.at())), gate.lever());
        }
        for (Position door : layout.doors()) {
            builder.fixture(Fixture.door(), door);
        }
        for (Position door : layout.lockedDoors()) {
            builder.fixture(Fixture.lockedDoor(), door);
        }
        ItemDefinition key = commonKey();
        for (Position cell : keyCells) {
            builder.item(new Item(ids.next(), key), cell);
        }
        layout.stairs().ifPresent(stairs -> builder.fixture(Fixture.stairs(), stairs));
        return builder.build();
    }

    /**
     * La cella libera più vicina a {@code center}, entro {@link #PACK_RADIUS} passi e alla distanza
     * minima dal player: a parità di distanza, la prima nell'ordine delle celle.
     */
    private static Optional<Position> nearest(
            List<Position> free, Position center, Position player, FloorSpec spec) {
        Position best = null;
        for (Position cell : free) {
            int distance = cell.distanceTo(center);
            if (distance <= PACK_RADIUS
                    && cell.distanceTo(player) >= spec.minSpawnDistance()
                    && (best == null || distance < best.distanceTo(center))) {
                best = cell;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Una cella per chiave, una per porta chiusa a chiave, tra quelle libere che il player
     * raggiunge senza passare da porte chiuse a chiave o cancelli: così ogni chiave si trova prima
     * della porta che apre, e nessun piano resta senza uscita.
     *
     * @throws FloorGenerationException se non ci sono abbastanza celle raggiungibili
     */
    private static List<Position> keyCells(
            Grid<TileType> terrain,
            FixtureLayout layout,
            Position playerAt,
            List<Position> free,
            RandomSource random) {
        if (layout.lockedDoors().isEmpty()) {
            return List.of();
        }
        List<Position> barriers = new ArrayList<>(layout.lockedDoors());
        for (FixtureLayout.Gate gate : layout.gates()) {
            barriers.add(gate.at());
        }
        List<Position> reachable = new ArrayList<>();
        java.util.Set<Position> seen = new java.util.LinkedHashSet<>();
        java.util.ArrayDeque<Position> queue = new java.util.ArrayDeque<>();
        seen.add(playerAt);
        queue.add(playerAt);
        while (!queue.isEmpty()) {
            Position cell = queue.poll();
            for (io.github.matteocardarelli.simpleroguelike.model.Direction direction :
                    io.github.matteocardarelli.simpleroguelike.model.Direction.values()) {
                Position next = cell.translate(direction);
                if (terrain.isInside(next)
                        && terrain.get(next).isWalkable()
                        && !barriers.contains(next)
                        && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        for (Position cell : free) {
            if (seen.contains(cell)) {
                reachable.add(cell);
            }
        }
        List<Position> keys = take(reachable, layout.lockedDoors().size(), random, "chiavi");
        free.removeAll(keys);
        return keys;
    }

    private ItemDefinition commonKey() {
        for (ItemDefinition definition : items.all()) {
            if (definition.key().equals(Optional.of(KeyType.COMMON))) {
                return definition;
            }
        }
        throw new FloorGenerationException("Il catalogo non ha una chiave per le porte chiuse");
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
