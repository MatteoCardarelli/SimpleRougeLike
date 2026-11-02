package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Piani per i test disegnati in ASCII, una stringa per riga:
 *
 * <pre>
 * #  muro            .  pavimento
 * &#64;  player          g  goblin
 * !  pozione a terra    /  spada a terra
 * [  armatura di cuoio   =  anello del vigore
 * a  arciere            -  coltello da lancio
 * +  porta chiusa        $  scrigno con una pozione
 * G  cancello chiuso     L  leva nel muro, che apre tutti i cancelli del disegno
 * >  scale              w  lupo
 * K  porta chiusa a chiave   k  chiave a terra
 * </pre>
 *
 * Gli id si assegnano da 1 in ordine di lettura, da sinistra a destra e dall'alto in basso: in
 * {@code "@g.g"} il player è 1 e i goblin 2 e 3.
 */
public final class FloorFixtures {

    public static final Stats PLAYER_STATS = new Stats(100, 10);
    public static final int PLAYER_FIRST_THRESHOLD = 100;
    public static final int INVENTORY_CAPACITY = 5;

    private FloorFixtures() {}

    public static Floor parse(String... rows) {
        return parse(INVENTORY_CAPACITY, rows);
    }

    public static Floor parse(int inventoryCapacity, String... rows) {
        return parse(new EntityIdSequence(), inventoryCapacity, rows);
    }

    /** Come {@link #parse(String...)}, prendendo gli id da una sequenza esistente. */
    public static Floor parse(EntityIdSequence ids, int inventoryCapacity, String... rows) {
        return parse(ids, inventoryCapacity, Optional.empty(), rows);
    }

    /** Come gli altri, ma con un player che esiste già al posto di {@code @}. */
    public static Floor parse(
            EntityIdSequence ids,
            int inventoryCapacity,
            Optional<Player> existing,
            String... rows) {
        if (rows.length == 0) {
            throw new IllegalArgumentException("Serve almeno una riga");
        }
        int width = rows[0].length();
        int height = rows.length;
        List<TileType> tiles = new ArrayList<>(width * height);
        FloorBuilder builder = new FloorBuilder();
        List<Position> gates = new ArrayList<>();
        List<Position> levers = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            if (rows[y].length() != width) {
                throw new IllegalArgumentException("Le righe devono avere la stessa lunghezza");
            }
            for (int x = 0; x < width; x++) {
                char cell = rows[y].charAt(x);
                Position at = new Position(x, y);
                tiles.add(cell == '#' || cell == 'L' ? TileType.WALL : TileType.FLOOR);
                switch (cell) {
                    case '#', '.' -> {}
                    case '+' -> builder.fixture(Fixture.door(), at);
                    case 'K' -> builder.fixture(Fixture.lockedDoor(), at);
                    case 'k' ->
                            builder.item(new Item(ids.next(), item(StandardItemCatalog.KEY)), at);
                    case '>' -> builder.fixture(Fixture.stairs(), at);
                    case 'G' -> {
                        builder.fixture(Fixture.gate(), at);
                        gates.add(at);
                    }
                    case 'L' -> levers.add(at);
                    case '$' ->
                            builder.fixture(
                                    Fixture.chest(Optional.of(new Item(ids.next(), potion()))), at);
                    case '@' ->
                            builder.player(
                                    existing.orElseGet(
                                            () ->
                                                    new Player(
                                                            ids.next(),
                                                            PLAYER_STATS,
                                                            PLAYER_FIRST_THRESHOLD,
                                                            inventoryCapacity)),
                                    at);
                    case 'g' -> builder.enemy(new Enemy(ids.next(), goblin()), at);
                    case 'a' -> builder.enemy(new Enemy(ids.next(), archer()), at);
                    case 'w' -> builder.enemy(new Enemy(ids.next(), wolf()), at);
                    case '-' ->
                            builder.item(
                                    new Item(ids.next(), item(StandardItemCatalog.THROWING_KNIFE)),
                                    at);
                    case '!' -> builder.item(new Item(ids.next(), potion()), at);
                    case '/' ->
                            builder.item(new Item(ids.next(), item(StandardItemCatalog.SWORD)), at);
                    case '[' ->
                            builder.item(
                                    new Item(ids.next(), item(StandardItemCatalog.LEATHER_ARMOR)),
                                    at);
                    case '=' ->
                            builder.item(
                                    new Item(ids.next(), item(StandardItemCatalog.RING_OF_VIGOR)),
                                    at);
                    default ->
                            throw new IllegalArgumentException(
                                    "Carattere sconosciuto '" + cell + "' in " + at);
                }
            }
        }
        for (Position lever : levers) {
            builder.fixture(Fixture.lever(gates), lever);
        }
        return builder.terrain(new Grid<>(width, height, tiles)).build();
    }

    /** Solo il terreno di un disegno: {@code #} è muro, ogni altro carattere è pavimento. */
    public static Grid<TileType> terrain(String... rows) {
        int width = rows[0].length();
        List<TileType> tiles = new ArrayList<>(width * rows.length);
        for (String row : rows) {
            if (row.length() != width) {
                throw new IllegalArgumentException("Le righe devono avere la stessa lunghezza");
            }
            for (int x = 0; x < width; x++) {
                tiles.add(row.charAt(x) == '#' ? TileType.WALL : TileType.FLOOR);
            }
        }
        return new Grid<>(width, rows.length, tiles);
    }

    public static EnemyDefinition goblin() {
        return new StandardEnemyCatalog().all().get(0);
    }

    public static EnemyDefinition archer() {
        return new StandardEnemyCatalog().all().get(1);
    }

    public static EnemyDefinition wolf() {
        return new StandardEnemyCatalog().all().get(2);
    }

    public static ItemDefinition potion() {
        return new StandardItemCatalog().all().get(0);
    }

    /** La definizione di un oggetto del catalogo del gioco. */
    public static ItemDefinition item(Kind kind) {
        for (ItemDefinition definition : new StandardItemCatalog().all()) {
            if (definition.kind().equals(kind)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("Nessun oggetto " + kind.value());
    }
}
