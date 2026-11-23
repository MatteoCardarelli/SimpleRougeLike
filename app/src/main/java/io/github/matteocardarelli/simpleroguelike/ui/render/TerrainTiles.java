package io.github.matteocardarelli.simpleroguelike.ui.render;

import androidx.annotation.DrawableRes;
import io.github.matteocardarelli.simpleroguelike.R;

/**
 * Quale immagine mostra una cella. Sono funzioni pure della posizione e dei vicini, così una cella
 * ha sempre lo stesso aspetto.
 */
final class TerrainTiles {

    private static final int[] FLOORS = {
        R.drawable.tile_floor_0, R.drawable.tile_floor_1, R.drawable.tile_floor_2
    };

    private TerrainTiles() {}

    /** Quale delle varianti del pavimento, da 0 a 2, mostra la cella in (x, y). */
    static int floorVariant(int x, int y) {
        int hash = x * 73856093 ^ y * 19349663;
        hash ^= hash >>> 13;
        hash *= 0x5bd1e995;
        hash ^= hash >>> 15;
        return Math.floorMod(hash, FLOORS.length);
    }

    @DrawableRes
    static int floor(int x, int y) {
        return FLOORS[floorVariant(x, y)];
    }

    /**
     * Il muro mostra la faccia se la cella sotto è dentro la mappa e non è un muro, altrimenti la
     * parte alta.
     */
    static boolean isWallFace(boolean belowInGrid, boolean belowIsWall) {
        return belowInGrid && !belowIsWall;
    }

    @DrawableRes
    static int wall(boolean belowInGrid, boolean belowIsWall) {
        return isWallFace(belowInGrid, belowIsWall)
                ? R.drawable.tile_wall_face
                : R.drawable.tile_wall_top;
    }
}
