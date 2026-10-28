package io.github.matteocardarelli.simpleroguelike.squidlib;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.port.TerrainGenerator;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidmath.GWTRNG;

/**
 * Stanze e corridoi generati da SquidLib.
 *
 * <p>Il generatore di SquidLib riceve un {@link GWTRNG} con il seed preso da {@code random}: i
 * costruttori senza generatore ne creerebbero uno da {@code Math.random()}, e la stessa partita
 * darebbe mappe diverse.
 */
public final class SquidTerrainGenerator implements TerrainGenerator {

    @Override
    public Grid<TileType> generate(int width, int height, RandomSource random) {
        char[][] map =
                new DungeonGenerator(width, height, new GWTRNG(random.nextLong())).generate();
        return SquidGrids.toTerrain(map);
    }
}
