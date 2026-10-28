package io.github.matteocardarelli.simpleroguelike.port;

import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.Optional;

/**
 * Un {@link TerrainGenerator} finto che non usa il caso. Senza argomenti genera una stanza: muri
 * sul bordo e pavimento dentro, delle dimensioni chieste. Con un disegno restituisce sempre quello.
 * Passa lo stesso contratto dell'adapter vero.
 */
public final class FixedTerrainGenerator implements TerrainGenerator {

    private final Optional<Grid<TileType>> fixed;

    /** Una stanza vuota delle dimensioni chieste. */
    public FixedTerrainGenerator() {
        this.fixed = Optional.empty();
    }

    /** Sempre lo stesso terreno, disegnato come in {@link FloorFixtures#terrain}. */
    public FixedTerrainGenerator(String... rows) {
        this.fixed = Optional.of(FloorFixtures.terrain(rows));
    }

    /**
     * Genera il terreno.
     *
     * @throws IllegalArgumentException se il terreno disegnato non ha le dimensioni chieste
     */
    @Override
    public Grid<TileType> generate(int width, int height, RandomSource random) {
        if (fixed.isPresent()) {
            Grid<TileType> terrain = fixed.get();
            if (terrain.width() != width || terrain.height() != height) {
                throw new IllegalArgumentException(
                        "Il terreno disegnato è "
                                + terrain.width()
                                + "x"
                                + terrain.height()
                                + ", non "
                                + width
                                + "x"
                                + height);
            }
            return terrain;
        }
        return Grid.of(
                width,
                height,
                cell -> {
                    boolean border =
                            cell.x() == 0
                                    || cell.y() == 0
                                    || cell.x() == width - 1
                                    || cell.y() == height - 1;
                    return border ? TileType.WALL : TileType.FLOOR;
                });
    }
}
