package io.github.matteocardarelli.simpleroguelike.ai;

import static io.github.matteocardarelli.simpleroguelike.ai.Behaviors.enemy;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.port.StraightLinePathfinder;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import org.junit.jupiter.api.Test;

class SurroundBehaviorTest {

    private final SurroundBehavior surround = new SurroundBehavior(new StraightLinePathfinder());

    @Test
    void decide_nextToThePlayer_attacks() {
        Floor floor = FloorFixtures.parse("@w");

        assertThat(decide(floor, 2)).isEqualTo(new StepIntent(Direction.LEFT));
    }

    @Test
    void decide_twoWolvesOnTheSameSide_splitTheCellsAroundThePlayer() {
        // Il player è al centro; i due lupi arrivano da destra. Il primo prende la cella a destra
        // del player, il secondo deve aggirarlo verso un'altra cella libera.
        Floor floor = FloorFixtures.parse(".....", "..@.w", "....w", ".....");

        assertThat(decide(floor, 2)).isEqualTo(new StepIntent(Direction.LEFT));
        assertThat(decide(floor, 3)).isNotEqualTo(new StepIntent(Direction.UP));
    }

    @Test
    void decide_isTheSameEveryTime() {
        Floor floor = FloorFixtures.parse(".....", "..@.w", "....w", ".....");

        assertThat(decide(floor, 3)).isEqualTo(decide(floor, 3));
    }

    @Test
    void decide_playerDead_waits() {
        Floor floor = FloorFixtures.parse("@..w");
        floor.damage(new EntityId(1), 1000);

        assertThat(decide(floor, 2)).isEqualTo(new WaitIntent());
    }

    private EnemyIntent decide(Floor floor, int enemyId) {
        return surround.decide(enemy(floor, enemyId), floor, new SeededRandomSource(1));
    }
}
