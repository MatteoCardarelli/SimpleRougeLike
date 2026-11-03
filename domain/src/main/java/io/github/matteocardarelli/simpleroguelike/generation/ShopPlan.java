package io.github.matteocardarelli.simpleroguelike.generation;

import io.github.matteocardarelli.simpleroguelike.dungeon.ItemDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Dove sta un negozio: una stanza di {@value #WIDTH}×{@value #HEIGHT} celle libere, con il mercante
 * sul lato sinistro e quattro merci a terra. La zona è la stanza più il suo bordo: finché il player
 * resta lì può tenere la merce senza pagarla.
 *
 * @param room le celle del negozio, dove non nasce niente altro
 * @param zone la stanza e il suo bordo
 * @param merchant la cella del mercante
 * @param wares le celle delle merci
 */
record ShopPlan(Set<Position> room, Set<Position> zone, Position merchant, List<Position> wares) {

    static final int WIDTH = 5;
    static final int HEIGHT = 3;

    ShopPlan {
        room = Collections.unmodifiableSet(new LinkedHashSet<>(room));
        zone = Collections.unmodifiableSet(new LinkedHashSet<>(zone));
        wares = List.copyOf(wares);
    }

    /** Il prezzo di una merce: più è rara, più costa. */
    static int price(ItemDefinition type) {
        return Math.max(5, 120 / Math.max(1, type.spawnWeight()));
    }

    /**
     * Una stanza libera a caso, fuori dalle zone chiuse.
     *
     * @return vuoto se il piano non ha uno spazio abbastanza grande
     */
    static Optional<ShopPlan> find(
            Grid<TileType> terrain,
            Collection<Position> free,
            Set<Position> closed,
            RandomSource random) {
        Set<Position> open = new LinkedHashSet<>(free);
        open.removeAll(closed);
        List<Position> corners = new ArrayList<>();
        for (Position corner : terrain.positions()) {
            if (fits(open, corner)) {
                corners.add(corner);
            }
        }
        if (corners.isEmpty()) {
            return Optional.empty();
        }
        Position corner = corners.get(random.nextInt(corners.size()));
        Set<Position> room = new LinkedHashSet<>();
        Set<Position> zone = new LinkedHashSet<>();
        for (int y = -1; y <= HEIGHT; y++) {
            for (int x = -1; x <= WIDTH; x++) {
                Position cell = new Position(corner.x() + x, corner.y() + y);
                zone.add(cell);
                if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
                    room.add(cell);
                }
            }
        }
        Position merchant = new Position(corner.x(), corner.y() + 1);
        List<Position> wares =
                List.of(
                        new Position(corner.x() + 2, corner.y()),
                        new Position(corner.x() + 4, corner.y()),
                        new Position(corner.x() + 2, corner.y() + 2),
                        new Position(corner.x() + 4, corner.y() + 2));
        return Optional.of(new ShopPlan(room, zone, merchant, wares));
    }

    private static boolean fits(Set<Position> open, Position corner) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (!open.contains(new Position(corner.x() + x, corner.y() + y))) {
                    return false;
                }
            }
        }
        return true;
    }
}
