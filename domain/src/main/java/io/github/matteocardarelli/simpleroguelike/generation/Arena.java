package io.github.matteocardarelli.simpleroguelike.generation;

import io.github.matteocardarelli.simpleroguelike.dungeon.Footprint;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * La stanza del boss, costruita in una fascia lungo un lato del piano che prima diventa tutta muro:
 * così niente del resto del piano la tocca. La stanza ha la forma della definizione del boss; dal
 * suo lato verso il piano esce la porta del boss, poi un corridoio largo una cella e lungo {@value
 * #CORRIDOR}, con muro ai due lati, fino al bordo della fascia. Da lì, se serve, si scava un
 * corridoio normale fino al primo pavimento del resto del piano. Le celle del resto del piano che
 * non si raggiungono più dal corridoio diventano muro. Il boss sta al centro della stanza.
 *
 * @param terrain il terreno con la stanza e il corridoio
 * @param interior le celle della stanza, dove non nasce niente altro
 * @param door la cella della porta del boss
 * @param corridor le celle del corridoio, dalla porta verso il resto del piano
 * @param bossAnchor la cella di riferimento del boss
 * @param outside le celle del resto del piano collegate al corridoio: il player nasce qui
 */
record Arena(
        Grid<TileType> terrain,
        Set<Position> interior,
        Position door,
        List<Position> corridor,
        Position bossAnchor,
        Set<Position> outside) {

    /** Quante celle è lungo il corridoio tra la porta del boss e il resto del piano. */
    static final int CORRIDOR = 5;

    /** Quante celle deve avere almeno il resto del piano per una partita. */
    static final int MIN_OUTSIDE = 80;

    private static final int ATTEMPTS = 40;

    Arena {
        interior = Collections.unmodifiableSet(new LinkedHashSet<>(interior));
        corridor = List.copyOf(corridor);
        outside = Collections.unmodifiableSet(new LinkedHashSet<>(outside));
    }

    /** Il lato del piano dove sta la fascia della stanza. */
    private enum Side {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }

    /**
     * Costruisce la stanza su un lato a caso, provando più posizioni.
     *
     * @param room la forma della stanza
     * @param bossSize il lato del quadrato che contiene il boss
     * @throws FloorGenerationException se il terreno è troppo piccolo o nessuna posizione va bene
     */
    static Arena build(Grid<TileType> terrain, Footprint room, int bossSize, RandomSource random) {
        int side = room.size();
        int band = band(side);
        if (Math.min(terrain.width(), terrain.height()) < side + 4
                || Math.max(terrain.width(), terrain.height()) < band + 4) {
            throw new FloorGenerationException("Piano troppo piccolo per la stanza del boss");
        }
        Side[] sides = Side.values();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Side chosen = sides[random.nextInt(sides.length)];
            int length = along(terrain, chosen);
            int depth = across(terrain, chosen);
            if (length < side + 4 || depth < band + 4) {
                continue;
            }
            int offset = 1 + random.nextInt(length - side - 2);
            Optional<Arena> arena = tryAt(terrain, room, bossSize, chosen, offset);
            if (arena.isPresent()) {
                return arena.get();
            }
        }
        throw new FloorGenerationException("Nessuna posizione per la stanza del boss");
    }

    /** Quante righe occupa la fascia: muro, stanza, porta e corridoio. */
    private static int band(int side) {
        return 1 + side + 1 + CORRIDOR;
    }

    private static Optional<Arena> tryAt(
            Grid<TileType> terrain, Footprint room, int bossSize, Side side, int offset) {
        int size = room.size();
        int band = band(size);
        int length = along(terrain, side);
        int depth = across(terrain, side);
        List<TileType> cells = new ArrayList<>(terrain.cells());
        Set<Position> bandCells = new LinkedHashSet<>();
        for (int y = 0; y < band; y++) {
            for (int x = 0; x < length; x++) {
                Position cell = toMap(terrain, side, x, y);
                bandCells.add(cell);
                cells.set(index(terrain, cell), TileType.WALL);
            }
        }
        Set<Position> interior = new LinkedHashSet<>();
        for (Position local : room.at(new Position(offset, 1))) {
            Position cell = toMap(terrain, side, local.x(), local.y());
            interior.add(cell);
            cells.set(index(terrain, cell), TileType.FLOOR);
        }
        int doorX = offset + size / 2;
        Position door = toMap(terrain, side, doorX, size + 1);
        cells.set(index(terrain, door), TileType.FLOOR);
        List<Position> corridor = new ArrayList<>();
        for (int y = size + 2; y < band; y++) {
            Position cell = toMap(terrain, side, doorX, y);
            corridor.add(cell);
            cells.set(index(terrain, cell), TileType.FLOOR);
        }
        // Dal bordo della fascia si scava fino al primo pavimento del resto del piano.
        @Nullable Position start = null;
        for (int y = band; y < depth - 1; y++) {
            Position cell = toMap(terrain, side, doorX, y);
            boolean floor = cells.get(index(terrain, cell)).isWalkable();
            cells.set(index(terrain, cell), TileType.FLOOR);
            if (floor) {
                start = toMap(terrain, side, doorX, band);
                break;
            }
        }
        if (start == null) {
            return Optional.empty();
        }
        Grid<TileType> carved = new Grid<>(terrain.width(), terrain.height(), cells);
        Set<Position> outside = reachable(carved, start, bandCells);
        if (outside.size() < MIN_OUTSIDE) {
            return Optional.empty();
        }
        // Il resto del piano che non si raggiunge più dal corridoio diventa muro.
        for (Position cell : carved.positions()) {
            if (!bandCells.contains(cell)
                    && carved.get(cell).isWalkable()
                    && !outside.contains(cell)) {
                cells.set(index(terrain, cell), TileType.WALL);
            }
        }
        Grid<TileType> result = new Grid<>(terrain.width(), terrain.height(), cells);
        int inset = (size - bossSize) / 2;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (Position local : Footprint.square(size).at(new Position(offset, 1))) {
            Position cell = toMap(terrain, side, local.x(), local.y());
            minX = Math.min(minX, cell.x());
            minY = Math.min(minY, cell.y());
        }
        Position anchor = new Position(minX + inset, minY + inset);
        return Optional.of(new Arena(result, interior, door, corridor, anchor, outside));
    }

    /** Quante celle è lungo il lato. */
    private static int along(Grid<TileType> terrain, Side side) {
        return side == Side.TOP || side == Side.BOTTOM ? terrain.width() : terrain.height();
    }

    /** Quante celle va il piano dal lato verso quello opposto. */
    private static int across(Grid<TileType> terrain, Side side) {
        return side == Side.TOP || side == Side.BOTTOM ? terrain.height() : terrain.width();
    }

    /**
     * La cella del piano che corrisponde a ({@code x}, {@code y}) contati dal lato: {@code x} lungo
     * il lato, {@code y} verso l'interno.
     */
    private static Position toMap(Grid<TileType> terrain, Side side, int x, int y) {
        return switch (side) {
            case TOP -> new Position(x, y);
            case BOTTOM -> new Position(x, terrain.height() - 1 - y);
            case LEFT -> new Position(y, x);
            case RIGHT -> new Position(terrain.width() - 1 - y, x);
        };
    }

    private static int index(Grid<TileType> terrain, Position cell) {
        return cell.y() * terrain.width() + cell.x();
    }

    /** Le celle attraversabili raggiungibili da {@code start} senza entrare nella fascia. */
    private static Set<Position> reachable(
            Grid<TileType> terrain, Position start, Set<Position> band) {
        Set<Position> seen = new LinkedHashSet<>();
        Deque<Position> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Position cell = queue.poll();
            for (Direction direction : Direction.values()) {
                Position next = cell.translate(direction);
                if (terrain.isInside(next)
                        && !band.contains(next)
                        && terrain.get(next).isWalkable()
                        && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return seen;
    }
}
