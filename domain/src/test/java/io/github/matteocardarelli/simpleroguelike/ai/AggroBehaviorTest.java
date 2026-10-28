package io.github.matteocardarelli.simpleroguelike.ai;

import static io.github.matteocardarelli.simpleroguelike.ai.Behaviors.enemy;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import org.junit.jupiter.api.Test;

class AggroBehaviorTest {

    private final Behaviors.Fixed chase = new Behaviors.Fixed(new StepIntent(Direction.LEFT));
    private final Behaviors.Fixed wander = new Behaviors.Fixed(new WaitIntent());
    private final AggroBehavior aggro = new AggroBehavior(chase, wander);

    @Test
    void decide_playerWithinRange_chases() {
        Floor floor = FloorFixtures.parse("@.......g");

        EnemyIntent intent = aggro.decide(enemy(floor, 2), floor, new SeededRandomSource(1));

        assertThat(intent).isEqualTo(new StepIntent(Direction.LEFT));
        assertThat(wander.calls).isZero();
    }

    @Test
    void decide_playerBeyondRange_wanders() {
        Floor floor = FloorFixtures.parse("@........g");

        EnemyIntent intent = aggro.decide(enemy(floor, 2), floor, new SeededRandomSource(1));

        assertThat(intent).isEqualTo(new WaitIntent());
        assertThat(chase.calls).isZero();
    }

    @Test
    void decide_deadPlayer_wanders() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(new EntityId(1), 1000);

        aggro.decide(enemy(floor, 2), floor, new SeededRandomSource(1));

        assertThat(wander.calls).isEqualTo(1);
        assertThat(chase.calls).isZero();
    }
}
