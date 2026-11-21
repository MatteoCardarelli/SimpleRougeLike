package io.github.matteocardarelli.simpleroguelike.ui.render;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.Direction;
import org.junit.jupiter.api.Test;

class GameViewTest {

    @Test
    void projectileRotation_turnsTheNorthEastArtTowardTheShot() {
        assertThat(GameView.projectileRotation(Direction.RIGHT)).isEqualTo(45f);
        assertThat(GameView.projectileRotation(Direction.DOWN)).isEqualTo(135f);
        assertThat(GameView.projectileRotation(Direction.LEFT)).isEqualTo(-135f);
        assertThat(GameView.projectileRotation(Direction.UP)).isEqualTo(-45f);
    }
}
