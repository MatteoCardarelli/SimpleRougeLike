package io.github.matteocardarelli.simpleroguelike.run;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardPlayer;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.generation.FloorFactory;
import io.github.matteocardarelli.simpleroguelike.generation.FloorPopulator;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.port.FixedTerrainGenerator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunTest {

    private static final FloorSpec SPEC = new FloorSpec(15, 10, 3, 2, 3, 1);
    private static final FloorFactory FLOORS =
            new FloorFactory(
                    new FixedTerrainGenerator(),
                    new FloorPopulator(
                            new StandardItemCatalog(),
                            new StandardEnemyCatalog(),
                            StandardPlayer.DEFINITION));

    @Test
    void start_sameSeed_givesSameRun() {
        Run first = Run.start(7, SPEC, FLOORS);
        Run second = Run.start(7, SPEC, FLOORS);

        assertThat(describe(first)).isEqualTo(describe(second));
        assertThat(first.gameplay().nextLong()).isEqualTo(second.gameplay().nextLong());
    }

    @Test
    void start_differentSeeds_giveDifferentFloors() {
        assertThat(describe(Run.start(7, SPEC, FLOORS)))
                .isNotEqualTo(describe(Run.start(8, SPEC, FLOORS)));
    }

    @Test
    void start_idsContinueAfterTheFloor() {
        Run run = Run.start(7, SPEC, FLOORS);

        assertThat(run.ids().next()).isEqualTo(new EntityId(7));
    }

    @Test
    void streams_areIndependent() {
        Run quiet = Run.start(7, SPEC, FLOORS);
        Run busy = Run.start(7, SPEC, FLOORS);

        for (int i = 0; i < 50; i++) {
            busy.gameplay().nextLong();
        }

        assertThat(busy.generation().nextLong()).isEqualTo(quiet.generation().nextLong());
        assertThat(quiet.generation().nextLong()).isNotEqualTo(quiet.gameplay().nextLong());
    }

    @Test
    void turn_startsAtOne_andGrowsAtEveryEnd() {
        Run run = Run.start(7, SPEC, FLOORS);

        assertThat(run.turn()).isEqualTo(1);
        run.endTurn();
        run.endTurn();
        assertThat(run.turn()).isEqualTo(3);
    }

    @Test
    void view_isTheSameFloor() {
        Run run = Run.start(7, SPEC, FLOORS);

        assertThat(run.view()).isSameAs(run.floor());
    }

    private static List<String> describe(Run run) {
        List<String> lines = new ArrayList<>();
        lines.add("player " + run.view().positionOf(run.view().playerId()));
        for (Enemy enemy : run.view().enemies()) {
            lines.add(enemy.id() + " " + run.view().positionOf(enemy.id()));
        }
        run.view().itemsOnGround().forEach((at, item) -> lines.add(item.id() + " " + at));
        return lines;
    }
}
