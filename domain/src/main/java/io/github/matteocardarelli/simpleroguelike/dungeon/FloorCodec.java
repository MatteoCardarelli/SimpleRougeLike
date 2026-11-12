package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Scrive un piano come righe di testo e lo rilegge, per il salvataggio della partita. Il formato è
 * scritto a mano, una riga per cosa, con i campi separati da spazi: il terreno riga per riga, il
 * player con inventario ed equipaggiamento, i nemici, gli oggetti a terra, gli oggetti fissi, le
 * celle esplorate e visibili, il negozio. I tipi si scrivono per {@code Kind} e si ritrovano nei
 * cataloghi: le definizioni non si salvano.
 */
public final class FloorCodec {

    private final ItemCatalog items;
    private final EnemyCatalog enemies;

    public FloorCodec(ItemCatalog items, EnemyCatalog enemies) {
        this.items = items;
        this.enemies = enemies;
    }

    /** Le righe del piano. */
    public List<String> write(Floor floor) {
        List<String> lines = new ArrayList<>();
        Grid<TileType> terrain = floor.terrain();
        lines.add("floor " + terrain.width() + " " + terrain.height());
        for (int y = 0; y < terrain.height(); y++) {
            StringBuilder row = new StringBuilder("row ");
            for (int x = 0; x < terrain.width(); x++) {
                row.append(terrain.get(new Position(x, y)).isWalkable() ? '.' : '#');
            }
            lines.add(row.toString());
        }
        Player player = floor.player();
        Position at = floor.positionOf(player.id());
        Stats base = player.baseStats();
        lines.add(
                join(
                        "player",
                        player.id().value(),
                        at.x(),
                        at.y(),
                        base.maxHealth(),
                        base.attack(),
                        base.defense(),
                        base.accuracy(),
                        base.evasion(),
                        player.health(),
                        player.level(),
                        player.experience(),
                        player.experienceToNextLevel(),
                        player.coins(),
                        player.inventory().capacity(),
                        player.keys(KeyType.COMMON),
                        player.keys(KeyType.BOSS)));
        for (Item item : player.inventory().items()) {
            lines.add(
                    join(
                            "inv",
                            item.id().value(),
                            item.kind().value(),
                            item.count(),
                            player.isEquipped(item.id()) ? 1 : 0));
        }
        for (Enemy enemy : floor.enemies()) {
            Position where = floor.positionOf(enemy.id());
            String jump = enemy.pendingJump().map(p -> p.x() + " " + p.y()).orElse("- -");
            lines.add(
                    join(
                            "enemy",
                            enemy.id().value(),
                            enemy.kind().value(),
                            where.x(),
                            where.y(),
                            enemy.health(),
                            jump,
                            // Il campo "stanco" del formato 1: oggi la fase sta in fondo.
                            0,
                            enemy.isAngry() ? 1 : 0,
                            enemy.hasJustRetreated() ? 1 : 0,
                            enemy.level(),
                            enemy.jumpPhase().name()));
        }
        for (Map.Entry<Position, Item> entry : floor.itemsOnGround().entrySet()) {
            Item item = entry.getValue();
            lines.add(
                    join(
                            "ground",
                            entry.getKey().x(),
                            entry.getKey().y(),
                            item.id().value(),
                            item.kind().value(),
                            item.count()));
        }
        for (Map.Entry<Position, Fixture> entry : floor.fixtures().entrySet()) {
            Fixture fixture = entry.getValue();
            StringBuilder line =
                    new StringBuilder(
                            join(
                                    "fixture",
                                    entry.getKey().x(),
                                    entry.getKey().y(),
                                    fixture.type().name(),
                                    fixture.isOpen() ? 1 : 0,
                                    fixture.contents()
                                            .map(
                                                    i ->
                                                            join(
                                                                    i.id().value(),
                                                                    i.kind().value(),
                                                                    i.count()))
                                            .orElse("- - -"),
                                    fixture.linked().size()));
            for (Position linked : fixture.linked()) {
                line.append(' ').append(linked.x()).append(' ').append(linked.y());
            }
            lines.add(line.toString());
        }
        lines.add("explored " + cells(floor.explored(), terrain));
        lines.add("visible " + cells(floor.visible(), terrain));
        floor.defeatedBoss().ifPresent(id -> lines.add("defeated " + id.value()));
        if (!floor.bossRoom().isEmpty()) {
            StringBuilder room = new StringBuilder("room");
            for (Position cell : floor.bossRoom().positions()) {
                room.append(' ').append(cell.x()).append(' ').append(cell.y());
            }
            lines.add(room.toString());
        }
        Optional<Shop> shop = floor.shop();
        if (shop.isPresent()) {
            lines.add("shop " + shop.get().merchant().value());
            for (Position cell : shop.get().zone()) {
                lines.add(join("zone", cell.x(), cell.y()));
            }
            for (Map.Entry<EntityId, Integer> price : shop.get().prices().entrySet()) {
                lines.add(join("price", price.getKey().value(), price.getValue()));
            }
            for (EntityId unpaid : shop.get().unpaid()) {
                lines.add("unpaid " + unpaid.value());
            }
        }
        lines.add("end");
        return Collections.unmodifiableList(lines);
    }

    /**
     * Rilegge un piano scritto da {@link #write}.
     *
     * @throws IllegalArgumentException se le righe non sono un piano valido
     */
    public Floor read(List<String> lines) {
        try {
            return readFloor(lines);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Piano salvato incompleto", e);
        }
    }

    private Floor readFloor(List<String> lines) {
        Reader in = new Reader(lines);
        String[] head = in.next("floor");
        int width = Integer.parseInt(head[1]);
        int height = Integer.parseInt(head[2]);
        List<TileType> tiles = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            String row = in.next("row")[1];
            if (row.length() != width) {
                throw new IllegalArgumentException("Riga del terreno lunga " + row.length());
            }
            for (int x = 0; x < width; x++) {
                tiles.add(row.charAt(x) == '#' ? TileType.WALL : TileType.FLOOR);
            }
        }
        Grid<TileType> terrain = new Grid<>(width, height, tiles);
        String[] p = in.next("player");
        Stats base =
                new Stats(
                        integer(p[4]), integer(p[5]), integer(p[6]), integer(p[7]), integer(p[8]));
        Player player =
                new Player(new EntityId(integer(p[1])), base, integer(p[12]), integer(p[14]));
        player.restoreProgress(integer(p[10]), integer(p[11]), integer(p[12]), integer(p[13]));
        List<Item> equipped = new ArrayList<>();
        while (in.peek("inv")) {
            String[] i = in.next("inv");
            Item item = item(integer(i[1]), i[2], integer(i[3]));
            Optional<KeyType> key = item.definition().key();
            if (key.isPresent()) {
                // Prima che diventassero un contatore, le chiavi stavano nell'inventario.
                player.addKeys(key.get(), item.count());
                continue;
            }
            if (!player.inventory().add(item)) {
                throw new IllegalArgumentException("Inventario salvato troppo pieno");
            }
            if (integer(i[4]) == 1) {
                equipped.add(item);
            }
        }
        for (Item item : equipped) {
            player.equip(item);
        }
        player.restoreSavedHealth(integer(p[9]));
        // Le chiavi in fondo alla riga sono arrivate dopo il formato 1.
        if (p.length > 16) {
            player.addKeys(KeyType.COMMON, integer(p[15]));
            player.addKeys(KeyType.BOSS, integer(p[16]));
        }
        FloorBuilder builder =
                new FloorBuilder()
                        .terrain(terrain)
                        .player(player, new Position(integer(p[2]), integer(p[3])));
        while (in.peek("enemy")) {
            String[] e = in.next("enemy");
            // Il livello è arrivato dopo il formato 1: un salvataggio di prima ha nemici di livello
            // 1.
            int level = e.length > 11 ? integer(e[11]) : 1;
            Enemy enemy = new Enemy(new EntityId(integer(e[1])), enemy(e[2]), level);
            enemy.restoreSavedHealth(integer(e[5]));
            Optional<Position> jump =
                    e[6].equals("-")
                            ? Optional.empty()
                            : Optional.of(new Position(integer(e[6]), integer(e[7])));
            // La fase è arrivata dopo il formato 1. Prima un salto annunciato lasciava il nemico a
            // terra fino all'atterraggio: oggi è in aria, sopra la sua ombra.
            JumpPhase phase =
                    e.length > 12
                            ? JumpPhase.valueOf(e[12])
                            : jump.isPresent() ? JumpPhase.AIRBORNE : JumpPhase.RESTING;
            enemy.restoreState(phase, jump, integer(e[9]) == 1);
            // Il campo è arrivato dopo il formato 1: un salvataggio di prima non ce l'ha.
            enemy.retreated(e.length > 10 && integer(e[10]) == 1);
            Position where = new Position(integer(e[3]), integer(e[4]));
            builder.enemy(enemy, e.length > 12 ? where : jump.orElse(where));
        }
        while (in.peek("ground")) {
            String[] g = in.next("ground");
            builder.item(
                    item(integer(g[3]), g[4], integer(g[5])),
                    new Position(integer(g[1]), integer(g[2])));
        }
        while (in.peek("fixture")) {
            String[] f = in.next("fixture");
            Position at = new Position(integer(f[1]), integer(f[2]));
            Optional<Item> contents =
                    f[5].equals("-")
                            ? Optional.empty()
                            : Optional.of(item(integer(f[5]), f[6], integer(f[7])));
            List<Position> linked = new ArrayList<>();
            int count = integer(f[8]);
            for (int k = 0; k < count; k++) {
                linked.add(new Position(integer(f[9 + 2 * k]), integer(f[10 + 2 * k])));
            }
            Fixture fixture = fixture(FixtureType.valueOf(f[3]), linked, contents);
            // Si apre prima di metterlo sul piano: un attore può stare in una porta aperta.
            if (integer(f[4]) == 1) {
                fixture.restoreOpen();
            }
            builder.fixture(fixture, at);
        }
        CellSet explored = cells(in.next("explored"), terrain);
        CellSet visible = cells(in.next("visible"), terrain);
        Optional<EntityId> defeated =
                in.peek("defeated")
                        ? Optional.of(new EntityId(integer(in.next("defeated")[1])))
                        : Optional.empty();
        // La stanza del boss è arrivata dopo il formato 1: senza, il boss salta dove può.
        if (in.peek("room")) {
            String[] r = in.next("room");
            List<Position> room = new ArrayList<>();
            for (int k = 1; k + 1 < r.length; k += 2) {
                room.add(new Position(integer(r[k]), integer(r[k + 1])));
            }
            builder.bossRoom(room);
        }
        Optional<Shop> shop = Optional.empty();
        List<EntityId> unpaid = new ArrayList<>();
        if (in.peek("shop")) {
            EntityId merchant = new EntityId(integer(in.next("shop")[1]));
            Set<Position> zone = new LinkedHashSet<>();
            while (in.peek("zone")) {
                String[] z = in.next("zone");
                zone.add(new Position(integer(z[1]), integer(z[2])));
            }
            Map<EntityId, Integer> prices = new LinkedHashMap<>();
            while (in.peek("price")) {
                String[] c = in.next("price");
                prices.put(new EntityId(integer(c[1])), integer(c[2]));
            }
            while (in.peek("unpaid")) {
                unpaid.add(new EntityId(integer(in.next("unpaid")[1])));
            }
            shop = Optional.of(new Shop(merchant, zone, prices));
        }
        in.next("end");
        Floor floor = builder.build();
        floor.restoreMemory(visible, explored, defeated);
        if (shop.isPresent()) {
            for (EntityId item : unpaid) {
                shop.get().take(item);
            }
            floor.openShop(shop.get());
        }
        return floor;
    }

    /** Quante righe del piano ci sono all'inizio di {@code lines}, fino a {@code end} compreso. */
    public static int length(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals("end")) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException("Piano senza la riga end");
    }

    private Item item(int id, String kind, int count) {
        Item item = new Item(new EntityId(id), itemDefinition(Kind.of(kind)));
        item.restoreCount(count);
        return item;
    }

    private ItemDefinition itemDefinition(Kind kind) {
        for (ItemDefinition definition : items.all()) {
            if (definition.kind().equals(kind)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("Oggetto sconosciuto: " + kind.value());
    }

    private EnemyDefinition enemy(String kind) {
        for (EnemyDefinition definition : enemies.all()) {
            if (definition.kind().value().equals(kind)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("Nemico sconosciuto: " + kind);
    }

    private static Fixture fixture(
            FixtureType type, List<Position> linked, Optional<Item> contents) {
        return switch (type) {
            case DOOR -> Fixture.door();
            case LOCKED_DOOR -> Fixture.lockedDoor();
            case BOSS_DOOR -> Fixture.bossDoor();
            case GATE -> Fixture.gate();
            case LEVER -> Fixture.lever(linked);
            case CHEST -> Fixture.chest(contents);
            case STAIRS -> Fixture.stairs();
        };
    }

    private static String cells(CellSet set, Grid<TileType> terrain) {
        StringBuilder bits = new StringBuilder();
        for (int y = 0; y < terrain.height(); y++) {
            for (int x = 0; x < terrain.width(); x++) {
                bits.append(set.contains(new Position(x, y)) ? '1' : '0');
            }
        }
        return bits.toString();
    }

    private static CellSet cells(String[] line, Grid<TileType> terrain) {
        String bits = line[1];
        if (bits.length() != terrain.width() * terrain.height()) {
            throw new IllegalArgumentException("Celle salvate di lunghezza sbagliata");
        }
        List<Position> cells = new ArrayList<>();
        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) == '1') {
                cells.add(new Position(i % terrain.width(), i / terrain.width()));
            }
        }
        return CellSet.of(terrain.width(), terrain.height(), cells);
    }

    private static int integer(String value) {
        return Integer.parseInt(value);
    }

    private static String join(Object... parts) {
        StringBuilder line = new StringBuilder();
        for (Object part : parts) {
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(part);
        }
        return line.toString();
    }

    /** Legge le righe una alla volta, controllando che abbiano il tipo atteso. */
    private static final class Reader {
        private final List<String> lines;
        private int index;

        Reader(List<String> lines) {
            this.lines = lines;
        }

        boolean peek(String tag) {
            return index < lines.size() && lines.get(index).split(" ", -1)[0].equals(tag);
        }

        String[] next(String tag) {
            if (!peek(tag)) {
                throw new IllegalArgumentException(
                        "Atteso " + tag + " alla riga " + index + " del piano salvato");
            }
            return lines.get(index++).split(" ", -1);
        }
    }
}
