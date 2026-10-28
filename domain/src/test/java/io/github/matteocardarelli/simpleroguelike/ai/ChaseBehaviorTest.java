package io.github.matteocardarelli.simpleroguelike.ai;

import static io.github.matteocardarelli.simpleroguelike.ai.Behaviors.enemy;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.port.Pathfinder;
import io.github.matteocardarelli.simpleroguelike.port.StraightLinePathfinder;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChaseBehaviorTest {

    private final ChaseBehavior chase = new ChaseBehavior(new StraightLinePathfinder());

    @Test
    void decide_playerInSight_stepsTowardIt() {
        Floor floor = FloorFixtures.parse("@...g");

        assertThat(decide(floor, 2)).isEqualTo(new StepIntent(Direction.LEFT));
    }

    @Test
    void decide_nextToThePlayer_stepsIntoIt() {
        Floor floor = FloorFixtures.parse(".", "@", "g");

        assertThat(decide(floor, 2)).isEqualTo(new StepIntent(Direction.UP));
    }

    @Test
    void decide_otherEnemyInTheWay_neverStepsOntoIt() {
        Floor floor = FloorFixtures.parse("@.gg", "....");

        assertThat(decide(floor, 3)).isEqualTo(new WaitIntent());
    }

    @Test
    void decide_noStep_waits() {
        Floor floor = FloorFixtures.parse("@#g");

        assertThat(decide(floor, 2)).isEqualTo(new WaitIntent());
    }

    @Test
    void decide_deadPlayer_waits() {
        Floor floor = FloorFixtures.parse("@.g");
        floor.damage(new EntityId(1), 1000);

        assertThat(decide(floor, 2)).isEqualTo(new WaitIntent());
    }

    @Test
    void decide_blocksOnlyTheOtherLiveEnemies() {
        Floor floor = FloorFixtures.parse("@.gg", ".g..");
        floor.damage(new EntityId(3), 1000);
        CellSet[] seen = new CellSet[1];
        Pathfinder spy =
                (Grid<TileType> terrain, CellSet blocked, Position from, Position to) -> {
                    seen[0] = blocked;
                    return Optional.empty();
                };

        new ChaseBehavior(spy).decide(enemy(floor, 2), floor, new SeededRandomSource(1));

        assertThat(seen[0].positions()).containsExactly(new Position(1, 1));
    }

    private EnemyIntent decide(Floor floor, int enemyId) {
        return chase.decide(enemy(floor, enemyId), floor, new SeededRandomSource(1));
    }
}
