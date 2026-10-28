package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import org.junit.jupiter.api.Test;

class ActorTest {

    @Test
    void actor_takeDamage_neverGoesBelowZero() {
        Enemy goblin = new Enemy(new EntityId(2), FloorFixtures.goblin());

        assertThat(goblin.takeDamage(12)).isEqualTo(12);
        assertThat(goblin.takeDamage(100)).isEqualTo(18);
        assertThat(goblin.health()).isZero();
        assertThat(goblin.isAlive()).isFalse();
    }

    @Test
    void actor_restoreHealth_onDead_returnsZero() {
        Enemy goblin = new Enemy(new EntityId(2), FloorFixtures.goblin());
        goblin.takeDamage(100);

        assertThat(goblin.restoreHealth(10)).isZero();
        assertThat(goblin.health()).isZero();
    }
}
