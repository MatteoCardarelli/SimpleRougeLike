package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.content.StandardPlayer;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import org.junit.jupiter.api.Test;

class PlayerDefinitionTest {

    @Test
    void constructor_invalidValues_areRejected() {
        assertThatThrownBy(() -> new PlayerDefinition(new Stats(0, 1), 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlayerDefinition(new Stats(1, 1), 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlayerDefinition(new Stats(1, 1), 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void player_fromDefinition_takesItsValues() {
        Player player = new Player(new EntityId(1), StandardPlayer.DEFINITION);

        assertThat(player.stats()).isEqualTo(new Stats(100, 10, 2, 10, 5));
        assertThat(player.experienceToNextLevel()).isEqualTo(100);
        assertThat(player.inventory().capacity()).isEqualTo(10);
    }
}
