package io.github.matteocardarelli.simpleroguelike.rules;

import static io.github.matteocardarelli.simpleroguelike.rules.Rules.DAMAGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.ExperienceGained;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DamageRulesTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId GOBLIN = new EntityId(2);

    @Test
    void apply_killByPlayer_grantsTheEnemyReward() {
        Floor floor = FloorFixtures.parse("@g");

        var events = DAMAGE.apply(floor, Optional.of(PLAYER), GOBLIN, 100);

        assertThat(events)
                .containsExactly(
                        new Damaged(GOBLIN, new Position(1, 0), 30, 0),
                        new Died(GOBLIN, Kind.of("goblin"), new Position(1, 0)),
                        new ExperienceGained(new Position(0, 0), 25));
        assertThat(floor.player().experience()).isEqualTo(25);
    }

    @Test
    void apply_killWithoutSource_grantsNothing() {
        Floor floor = FloorFixtures.parse("@g");

        var events = DAMAGE.apply(floor, Optional.empty(), GOBLIN, 100);

        assertThat(events).hasSize(2);
        assertThat(floor.player().experience()).isZero();
    }

    @Test
    void apply_killByAnotherEnemy_grantsNothing() {
        Floor floor = FloorFixtures.parse("@gg");

        DAMAGE.apply(floor, Optional.of(new EntityId(3)), GOBLIN, 100);

        assertThat(floor.player().experience()).isZero();
    }

    @Test
    void apply_notLethal_grantsNothing() {
        Floor floor = FloorFixtures.parse("@g");

        var events = DAMAGE.apply(floor, Optional.of(PLAYER), GOBLIN, 5);

        assertThat(events).containsExactly(new Damaged(GOBLIN, new Position(1, 0), 5, 25));
        assertThat(floor.player().experience()).isZero();
    }

    @Test
    void apply_playerKilled_grantsNothing() {
        Floor floor = FloorFixtures.parse("@g");

        var events = DAMAGE.apply(floor, Optional.of(GOBLIN), PLAYER, 1000);

        assertThat(events).last().isInstanceOf(Died.class);
        assertThat(floor.player().isAlive()).isFalse();
    }
}
