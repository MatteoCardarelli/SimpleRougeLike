package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TileTypeTest {

    @Test
    void tileType_wallBlocksMovementAndSight() {
        assertThat(TileType.FLOOR.isWalkable()).isTrue();
        assertThat(TileType.FLOOR.isTransparent()).isTrue();
        assertThat(TileType.WALL.isWalkable()).isFalse();
        assertThat(TileType.WALL.isTransparent()).isFalse();
    }
}
