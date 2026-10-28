package io.github.matteocardarelli.simpleroguelike.ai;

import static io.github.matteocardarelli.simpleroguelike.ai.Behaviors.enemy;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WanderBehaviorTest {

    private final WanderBehavior wander = new WanderBehavior();

    @Test
    void decide_onlyStepsIntoFreeCells() {
        Floor floor = FloorFixtures.parse("###", "@g.", "###");
        SeededRandomSource random = new SeededRandomSource(1);

        for (int i = 0; i < 50; i++) {
            assertThat(wander.decide(enemy(floor, 2), floor, random))
                    .isEqualTo(new StepIntent(Direction.RIGHT));
        }
    }

    @Test
    void decide_usesEveryFreeDirection() {
        Floor floor = FloorFixtures.parse("@....", ".....", "..g..", ".....");
        SeededRandomSource random = new SeededRandomSource(1);
        List<EnemyIntent> seen = new ArrayList<>();

        for (int i = 0; i < 200; i++) {
            seen.add(wander.decide(enemy(floor, 2), floor, random));
        }

        assertThat(seen)
                .contains(
                        new StepIntent(Direction.UP),
                        new StepIntent(Direction.DOWN),
                        new StepIntent(Direction.LEFT),
                        new StepIntent(Direction.RIGHT))
                .doesNotContain(new WaitIntent());
    }

    @Test
    void decide_surrounded_waits() {
        Floor floor = FloorFixtures.parse("###", "@g#", "###");

        assertThat(wander.decide(enemy(floor, 2), floor, new SeededRandomSource(1)))
                .isEqualTo(new WaitIntent());
    }

    @Test
    void decide_sameRandom_sameChoices() {
        Floor floor = FloorFixtures.parse("@....", ".....", "..g..", ".....");
        SeededRandomSource first = new SeededRandomSource(3);
        SeededRandomSource second = new SeededRandomSource(3);

        for (int i = 0; i < 20; i++) {
            assertThat(wander.decide(enemy(floor, 2), floor, first))
                    .isEqualTo(wander.decide(enemy(floor, 2), floor, second));
        }
    }
}
