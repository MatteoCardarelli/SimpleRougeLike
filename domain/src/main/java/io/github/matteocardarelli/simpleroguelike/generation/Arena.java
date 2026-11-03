package io.github.matteocardarelli.simpleroguelike.generation;

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

/**
 * L'arena del boss, ricavata nel terreno generato: una stanza di {@value #INNER_WIDTH}×{@value
 * #INNER_HEIGHT} celle chiusa da muri, con una sola porta sul lato che guarda il centro del piano e
 * un corridoio scavato da lì fino al primo pavimento del resto del piano. Il boss sta al centro.
 *
 * @param terrain il terreno con l'arena e il corridoio
 * @param interior le celle dentro l'arena, dove non nasce niente altro
 * @param door la cella della porta del boss
 * @param bossAnchor la cella di riferimento del boss
 * @param outside le celle del resto del piano collegate al corridoio: il player nasce qui
 */
record Arena(
        Grid<TileType> terrain,
        Set<Position> interior,
        Position door,
        Position bossAnchor,
        Set<Position> outside) {

    static final int INNER_WIDTH = 10;
    static final int INNER_HEIGHT = 8;
    private static final int ATTEMPTS = 40;

    Arena {
        interior = Collections.unmodifiableSet(new LinkedHashSet<>(interior));
        outside = Collections.unmodifiableSet(new LinkedHashSet<>(outside));
    }

    /**
     * Ricava l'arena in una posizione a caso, provando più posizioni.
     *
     * @param bossSize il lato del quadrato che contiene il boss
     * @throws FloorGenerationException se il terreno è troppo piccolo o nessuna posizione va bene
     */
    static Arena carve(Grid<TileType> terrain, int bossSize, RandomSource random) {
        int outerWidth = INNER_WIDTH + 2;
        int outerHeight = INNER_HEIGHT + 2;
        if (terrain.width() < outerWidth + 4 || terrain.height() < outerHeight + 4) {
            throw new FloorGenerationException("Piano troppo piccolo per l'arena del boss");
        }
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x0 = 1 + random.nextInt(terrain.width() - outerWidth - 1);
            int y0 = 1 + random.nextInt(terrain.height() - outerHeight - 1);
            Optional<Arena> arena = tryAt(terrain, x0, y0, bossSize);
            if (arena.isPresent()) {
                return arena.get();
            }
        }
        throw new FloorGenerationException("Nessuna posizione per l'arena del boss");
    }

    private static Optional<Arena> tryAt(Grid<TileType> terrain, int x0, int y0, int bossSize) {
        int outerWidth = INNER_WIDTH + 2;
        int outerHeight = INNER_HEIGHT + 2;
        List<TileType> cells = new ArrayList<>(terrain.cells());
        Set<Position> interior = new LinkedHashSet<>();
        Set<Position> rect = new LinkedHashSet<>();
        for (int y = y0; y < y0 + outerHeight; y++) {
            for (int x = x0; x < x0 + outerWidth; x++) {
                Position cell = new Position(x, y);
                rect.add(cell);
                boolean border =
                        x == x0 || y == y0 || x == x0 + outerWidth - 1 || y == y0 + outerHeight - 1;
                cells.set(index(terrain, cell), border ? TileType.WALL : TileType.FLOOR);
                if (!border) {
                    interior.add(cell);
                }
            }
        }
        // La porta sta sul lato che guarda il centro del piano.
        int dx = (x0 + outerWidth / 2) - terrain.width() / 2;
        int dy = (y0 + outerHeight / 2) - terrain.height() / 2;
        Direction out;
        Position door;
        if (Math.abs(dx) >= Math.abs(dy)) {
            out = dx > 0 ? Direction.LEFT : Direction.RIGHT;
            int x = dx > 0 ? x0 : x0 + outerWidth - 1;
            door = new Position(x, y0 + outerHeight / 2);
        } else {
            out = dy > 0 ? Direction.UP : Direction.DOWN;
            int y = dy > 0 ? y0 : y0 + outerHeight - 1;
            door = new Position(x0 + outerWidth / 2, y);
        }
        cells.set(index(terrain, door), TileType.FLOOR);
        Position cell = door.translate(out);
        Optional<Position> reached = Optional.empty();
        while (terrain.isInside(cell)) {
            if (!rect.contains(cell) && terrain.get(cell).isWalkable()) {
                reached = Optional.of(cell);
                break;
            }
            cells.set(index(terrain, cell), TileType.FLOOR);
            cell = cell.translate(out);
        }
        if (!reached.isPresent()) {
            return Optional.empty();
        }
        Grid<TileType> carved = new Grid<>(terrain.width(), terrain.height(), cells);
        Set<Position> outside = reachable(carved, reached.get(), door, rect);
        if (outside.size() < INNER_WIDTH * INNER_HEIGHT) {
            // Il resto del piano collegato all'arena è troppo piccolo per una partita.
            return Optional.empty();
        }
        Position anchor =
                new Position(
                        x0 + 1 + (INNER_WIDTH - bossSize) / 2,
                        y0 + 1 + (INNER_HEIGHT - bossSize) / 2);
        return Optional.of(new Arena(carved, interior, door, anchor, outside));
    }

    private static int index(Grid<TileType> terrain, Position cell) {
        return cell.y() * terrain.width() + cell.x();
    }

    /** Le celle attraversabili raggiungibili da {@code start} senza entrare nell'arena. */
    private static Set<Position> reachable(
            Grid<TileType> terrain, Position start, Position door, Set<Position> arena) {
        Set<Position> seen = new LinkedHashSet<>();
        Deque<Position> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Position cell = queue.poll();
            for (Direction direction : Direction.values()) {
                Position next = cell.translate(direction);
                if (terrain.isInside(next)
                        && terrain.get(next).isWalkable()
                        && !next.equals(door)
                        && !arena.contains(next)
                        && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return seen;
    }
}
