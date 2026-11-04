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
import io.github.matteocardarelli.simpleroguelike.dungeon.Shop;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    /** La probabilità, in percentuale, di un oggetto raro dietro una porta chiusa a chiave. */
    static final int LOCKED_REWARD_CHANCE = 25;

    /** Il peso di comparsa massimo di un oggetto raro. */
    static final int RARE_WEIGHT = 3;

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
        if (spec.bossCount() == 0) {
            return populateOn(terrain, spec, random, ids, existing, Optional.empty());
        }
        EnemyDefinition boss = bossDefinition();
        Arena arena = Arena.carve(terrain, boss.footprint().size(), random);
        return populateOn(arena.terrain(), spec, random, ids, existing, Optional.of(arena));
    }

    private EnemyDefinition merchantDefinition() {
        for (EnemyDefinition definition : enemies.all()) {
            if (definition.merchant()) {
                return definition;
            }
        }
        throw new FloorGenerationException("Il catalogo non ha un mercante per il negozio");
    }

    /** Quello che si vende: gli oggetti che compaiono a caso, tranne le monete. */
    private List<ItemDefinition> wares() {
        List<ItemDefinition> wares = new ArrayList<>();
        for (ItemDefinition definition : items.all()) {
            if (definition.spawnWeight() > 0 && definition.coinValue() == 0) {
                wares.add(definition);
            }
        }
        return wares;
    }

    private EnemyDefinition bossDefinition() {
        for (EnemyDefinition definition : enemies.all()) {
            if (definition.boss()) {
                return definition;
            }
        }
        throw new FloorGenerationException("Il catalogo non ha un boss per il suo piano");
    }

    private Floor populateOn(
            Grid<TileType> terrain,
            FloorSpec spec,
            RandomSource random,
            EntityIdSequence ids,
            Optional<Player> existing,
            Optional<Arena> arena) {
        Set<Position> reserved = new LinkedHashSet<>();
        if (arena.isPresent()) {
            reserved.addAll(arena.get().interior());
            reserved.add(arena.get().door());
        }
        List<Position> free = new ArrayList<>();
        for (Position cell : terrain.positions()) {
            if (terrain.get(cell).isWalkable()) {
                free.add(cell);
            }
        }
        if (free.isEmpty()) {
            throw new FloorGenerationException("Il terreno non ha celle attraversabili");
        }
        free.removeAll(reserved);
        FixtureLayout layout = FixtureLayout.plan(terrain, spec, random, reserved);
        free.removeAll(layout.floorCells());
        List<Position> playerCells = new ArrayList<>(free);
        playerCells.removeAll(layout.closed());
        if (arena.isPresent()) {
            // Il player nasce dove arriva il corridoio dell'arena.
            playerCells.retainAll(arena.get().outside());
        }
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
        Optional<ShopPlan> shopPlan = Optional.empty();
        if (spec.bossCount() == 0 && random.nextInt(100) < spec.shopChance()) {
            shopPlan = ShopPlan.find(terrain, free, layout.closed(), random);
        }
        if (shopPlan.isPresent()) {
            free.removeAll(shopPlan.get().room());
            playerCells.removeAll(shopPlan.get().zone());
            if (playerCells.isEmpty()) {
                throw new FloorGenerationException("Nessuna cella per il player fuori dal negozio");
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

        List<Position> keyCells =
                keyCells(terrain, layout, arena.map(Arena::door), playerAt, free, random);

        List<Position> rewardCells =
                rewardCells(terrain, layout, arena.map(Arena::door), playerAt, free, random);
        List<ItemDefinition> rewardTypes = new ArrayList<>();
        for (int i = 0; i < rewardCells.size(); i++) {
            rewardTypes.add(WeightedPicker.pick(rare(), ItemDefinition::spawnWeight, random));
        }

        List<ItemDefinition> chestTypes = new ArrayList<>();
        for (int i = 0; i < layout.chests().size(); i++) {
            chestTypes.add(WeightedPicker.pick(items.all(), ItemDefinition::spawnWeight, random));
        }
        FloorBuilder builder =
                new FloorBuilder()
                        .terrain(terrain)
                        .player(existing.orElseGet(() -> new Player(ids.next(), player)), playerAt);
        for (int i = 0; i < enemyCells.size(); i++) {
            builder.enemy(
                    new Enemy(ids.next(), enemyTypes.get(i), spec.enemyLevel()), enemyCells.get(i));
        }
        for (int i = 0; i < itemCells.size(); i++) {
            builder.item(new Item(ids.next(), itemTypes.get(i)), itemCells.get(i));
        }
        for (int i = 0; i < rewardCells.size(); i++) {
            builder.item(new Item(ids.next(), rewardTypes.get(i)), rewardCells.get(i));
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
        for (int i = 0; i < keyCells.size(); i++) {
            KeyType type = i < layout.lockedDoors().size() ? KeyType.COMMON : KeyType.BOSS;
            builder.item(new Item(ids.next(), key(type)), keyCells.get(i));
        }
        if (arena.isPresent()) {
            builder.fixture(Fixture.bossDoor(), arena.get().door());
            builder.enemy(new Enemy(ids.next(), bossDefinition()), arena.get().bossAnchor());
        }
        layout.stairs().ifPresent(stairs -> builder.fixture(Fixture.stairs(), stairs));
        Optional<Shop> shop = Optional.empty();
        if (shopPlan.isPresent()) {
            ShopPlan plan = shopPlan.get();
            EntityId merchant = ids.next();
            builder.enemy(new Enemy(merchant, merchantDefinition()), plan.merchant());
            Map<EntityId, Integer> prices = new LinkedHashMap<>();
            for (Position cell : plan.wares()) {
                ItemDefinition type =
                        WeightedPicker.pick(wares(), ItemDefinition::spawnWeight, random);
                Item item = new Item(ids.next(), type);
                builder.item(item, cell);
                prices.put(item.id(), ShopPlan.price(type));
            }
            shop = Optional.of(new Shop(merchant, plan.zone(), prices));
        }
        Floor floor = builder.build();
        shop.ifPresent(floor::openShop);
        return floor;
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
     * Una cella per chiave, una per porta chiusa a chiave più quella del boss, tra quelle libere
     * che il player raggiunge senza passare da porte chiuse a chiave, cancelli o dalla porta del
     * boss: così ogni chiave si trova prima della porta che apre, e nessun piano resta senza
     * uscita.
     *
     * @throws FloorGenerationException se non ci sono abbastanza celle raggiungibili
     */
    private static List<Position> keyCells(
            Grid<TileType> terrain,
            FixtureLayout layout,
            Optional<Position> bossDoor,
            Position playerAt,
            List<Position> free,
            RandomSource random) {
        int count = layout.lockedDoors().size() + (bossDoor.isPresent() ? 1 : 0);
        if (count == 0) {
            return List.of();
        }
        Set<Position> seen = reachable(terrain, playerAt, barriers(layout, bossDoor));
        List<Position> reachable = new ArrayList<>();
        for (Position cell : free) {
            if (seen.contains(cell)) {
                reachable.add(cell);
            }
        }
        List<Position> keys = take(reachable, count, random, "chiavi");
        free.removeAll(keys);
        return keys;
    }

    /**
     * Per ogni porta chiusa a chiave, una volta su {@code 100 / }{@value #LOCKED_REWARD_CHANCE}, la
     * cella libera più vicina alla porta dalla parte che il player non raggiunge senza aprirla: lì
     * c'è un oggetto raro. Una porta che non separa niente non ne ha.
     */
    private static List<Position> rewardCells(
            Grid<TileType> terrain,
            FixtureLayout layout,
            Optional<Position> bossDoor,
            Position playerAt,
            List<Position> free,
            RandomSource random) {
        List<Position> rewards = new ArrayList<>();
        if (layout.lockedDoors().isEmpty()) {
            return rewards;
        }
        List<Position> barriers = barriers(layout, bossDoor);
        Set<Position> before = reachable(terrain, playerAt, barriers);
        for (Position door : layout.lockedDoors()) {
            if (random.nextInt(100) >= LOCKED_REWARD_CHANCE) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                Position behind = door.translate(direction);
                if (!terrain.isInside(behind)
                        || !terrain.get(behind).isWalkable()
                        || before.contains(behind)) {
                    continue;
                }
                Set<Position> side = reachable(terrain, behind, barriers);
                Position best = null;
                for (Position cell : free) {
                    if (side.contains(cell)
                            && (best == null || cell.distanceTo(door) < best.distanceTo(door))) {
                        best = cell;
                    }
                }
                if (best != null) {
                    free.remove(best);
                    rewards.add(best);
                }
                break;
            }
        }
        return rewards;
    }

    /**
     * Le celle che si attraversano solo aprendole: porte chiuse a chiave, cancelli, la porta del
     * boss.
     */
    private static List<Position> barriers(FixtureLayout layout, Optional<Position> bossDoor) {
        List<Position> barriers = new ArrayList<>(layout.lockedDoors());
        bossDoor.ifPresent(barriers::add);
        for (FixtureLayout.Gate gate : layout.gates()) {
            barriers.add(gate.at());
        }
        return barriers;
    }

    /**
     * Le celle attraversabili che si raggiungono da {@code start} senza passare da {@code
     * barriers}.
     */
    private static Set<Position> reachable(
            Grid<TileType> terrain, Position start, List<Position> barriers) {
        Set<Position> seen = new LinkedHashSet<>();
        ArrayDeque<Position> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Position cell = queue.poll();
            for (Direction direction : Direction.values()) {
                Position next = cell.translate(direction);
                if (terrain.isInside(next)
                        && terrain.get(next).isWalkable()
                        && !barriers.contains(next)
                        && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return seen;
    }

    /** Gli oggetti rari, quelli che compaiono poco: il premio dietro una porta chiusa. */
    private List<ItemDefinition> rare() {
        List<ItemDefinition> rare = new ArrayList<>();
        for (ItemDefinition definition : items.all()) {
            if (definition.spawnWeight() > 0
                    && definition.spawnWeight() <= RARE_WEIGHT
                    && definition.coinValue() == 0
                    && !definition.key().isPresent()) {
                rare.add(definition);
            }
        }
        if (rare.isEmpty()) {
            throw new FloorGenerationException("Il catalogo non ha oggetti rari");
        }
        return rare;
    }

    private ItemDefinition key(KeyType type) {
        for (ItemDefinition definition : items.all()) {
            if (definition.key().equals(Optional.of(type))) {
                return definition;
            }
        }
        throw new FloorGenerationException("Il catalogo non ha una chiave " + type);
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
