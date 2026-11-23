package io.github.matteocardarelli.simpleroguelike.ui.render;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.R;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TerrainTilesTest {

    @Test
    void floorVariant_isDeterministicAndInRange() {
        Set<Integer> seen = new HashSet<>();
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 40; x++) {
                int variant = TerrainTiles.floorVariant(x, y);
                assertThat(variant).isBetween(0, 2);
                assertThat(TerrainTiles.floorVariant(x, y)).isEqualTo(variant);
                seen.add(variant);
            }
        }
        assertThat(seen).containsExactlyInAnyOrder(0, 1, 2);
    }

    @Test
    void floor_usesTheVariantImage() {
        assertThat(TerrainTiles.floor(3, 4))
                .isIn(R.drawable.tile_floor_0, R.drawable.tile_floor_1, R.drawable.tile_floor_2);
    }

    @Test
    void wall_showsItsFaceOnlyWhenTheCellBelowIsInTheGridAndNotAWall() {
        assertThat(TerrainTiles.wall(true, false)).isEqualTo(R.drawable.tile_wall_face);
        assertThat(TerrainTiles.wall(true, true)).isEqualTo(R.drawable.tile_wall_top);
        assertThat(TerrainTiles.wall(false, false)).isEqualTo(R.drawable.tile_wall_top);
    }
}
