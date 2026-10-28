package io.github.matteocardarelli.simpleroguelike.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.dungeon.EffectContext;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.event.Healed;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import org.junit.jupiter.api.Test;

class HealEffectTest {

    private static final EntityId PLAYER = new EntityId(1);

    @Test
    void apply_healsTheUser() {
        Floor floor = FloorFixtures.parse("@");
        floor.damage(PLAYER, 50);

        var events = new HealEffect(25).apply(context(floor));

        assertThat(events).containsExactly(new Healed(PLAYER, new Position(0, 0), 25));
        assertThat(floor.player().health()).isEqualTo(75);
    }

    @Test
    void apply_fullHealth_emitsNothing() {
        Floor floor = FloorFixtures.parse("@");

        assertThat(new HealEffect(25).apply(context(floor))).isEmpty();
    }

    @Test
    void apply_deadUser_staysDead() {
        Floor floor = FloorFixtures.parse("@");
        floor.damage(PLAYER, 1000);

        assertThat(new HealEffect(25).apply(context(floor))).isEmpty();
        assertThat(floor.player().isAlive()).isFalse();
    }

    @Test
    void constructor_nonPositiveAmount_isRejected() {
        assertThatThrownBy(() -> new HealEffect(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static EffectContext context(Floor floor) {
        return new EffectContext(PLAYER, floor, new SeededRandomSource(1));
    }
}
