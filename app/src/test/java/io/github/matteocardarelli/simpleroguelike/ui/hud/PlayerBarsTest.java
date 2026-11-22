package io.github.matteocardarelli.simpleroguelike.ui.hud;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import org.junit.jupiter.api.Test;

class PlayerBarsTest {

    private static PlayerView player(int poisonTurns) {
        return new PlayerView(
                new EntityId(1),
                new Position(1, 1),
                80,
                100,
                1,
                0,
                100,
                10,
                0,
                0,
                0,
                0,
                0,
                0,
                poisonTurns);
    }

    @Test
    void isPoisoned_showsPoisonOnlyWhileTurnsRemain() {
        assertThat(PlayerBars.isPoisoned(player(0))).isFalse();
        assertThat(PlayerBars.isPoisoned(player(3))).isTrue();
    }
}
