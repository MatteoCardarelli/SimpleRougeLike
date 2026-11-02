package io.github.matteocardarelli.simpleroguelike.ai;

import static io.github.matteocardarelli.simpleroguelike.ai.Behaviors.enemy;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import org.junit.jupiter.api.Test;

class KiteBehaviorTest {

    private final Behaviors.Fixed approach = new Behaviors.Fixed(new WaitIntent());
    private final KiteBehavior kite = new KiteBehavior(approach);

    @Test
    void decide_playerInLineAndInRange_shoots() {
        Floor floor = FloorFixtures.parse("@...a");

        assertThat(decide(floor, 2)).isEqualTo(new RangedAttackIntent(Direction.LEFT));
        assertThat(approach.calls).isZero();
    }

    @Test
    void decide_playerInAColumn_shootsAlongIt() {
        Floor floor = FloorFixtures.parse("a", ".", "@");

        assertThat(decide(floor, 1)).isEqualTo(new RangedAttackIntent(Direction.DOWN));
    }

    @Test
    void decide_somethingInTheWay_approachesInstead() {
        Floor wall = FloorFixtures.parse("@.#.a");
        Floor goblin = FloorFixtures.parse("@.g.a");

        assertThat(decide(wall, 2)).isEqualTo(new WaitIntent());
        assertThat(decide(goblin, 3)).isEqualTo(new WaitIntent());
        assertThat(approach.calls).isEqualTo(2);
    }

    @Test
    void decide_outOfRangeOrNotInLine_approaches() {
        Floor far = FloorFixtures.parse("@......a");
        Floor diagonal = FloorFixtures.parse("@..", "..a");

        decide(far, 2);
        decide(diagonal, 2);

        assertThat(approach.calls).isEqualTo(2);
    }

    @Test
    void decide_playerNextToIt_stepsAway() {
        Floor floor = FloorFixtures.parse("@a.");

        assertThat(decide(floor, 2)).isEqualTo(new StepIntent(Direction.RIGHT));
    }

    @Test
    void decide_playerNextToIt_andNoWayOut_attacksUpClose() {
        Floor floor = FloorFixtures.parse("#@a#", "####");

        assertThat(decide(floor, 2)).isEqualTo(new StepIntent(Direction.LEFT));
    }

    @Test
    void decide_playerDead_waits() {
        Floor floor = FloorFixtures.parse("@..a");
        floor.damage(new EntityId(1), 1000);

        assertThat(decide(floor, 2)).isEqualTo(new WaitIntent());
    }

    @Test
    void aim_checksLineRangeAndTheCellsInBetween() {
        Floor floor = FloorFixtures.parse("@...");
        Position from = new Position(0, 0);

        assertThat(KiteBehavior.aim(floor, from, new Position(3, 0), 3)).contains(Direction.RIGHT);
        assertThat(KiteBehavior.aim(floor, from, new Position(3, 0), 2)).isEmpty();
        assertThat(KiteBehavior.aim(floor, from, from, 5)).isEmpty();
    }

    private EnemyIntent decide(Floor floor, int enemyId) {
        return kite.decide(enemy(floor, enemyId), floor, new SeededRandomSource(1));
    }
}
