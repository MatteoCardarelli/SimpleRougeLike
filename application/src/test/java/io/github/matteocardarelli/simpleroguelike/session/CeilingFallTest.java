package io.github.matteocardarelli.simpleroguelike.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.event.EnemyFell;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Landed;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** I goblin che cadono dal soffitto quando lo slime gigante atterra. */
class CeilingFallTest {

    private static final int LEVEL = 3;
    private static final int SEEDS = 150;

    @Test
    void landing_withNoGoblins_dropsTwoOnValidCells_whenTheRollSucceeds() {
        int falls = 0;
        int misses = 0;
        for (long seed = 1; seed <= SEEDS; seed++) {
            Run run = bossFight(seed, 0);
            Floor floor = run.floor();
            List<GameEvent> events = Sessions.enemyTurnRunner().run(run, LEVEL);
            List<EnemyFell> fell = fell(events);

            if (!rollSucceeds(seed)) {
                assertThat(fell).as("seed %d", seed).isEmpty();
                misses++;
                continue;
            }
            falls++;
            assertThat(fell).hasSize(2);
            int landed = indexOfLanded(events);
            assertThat(landed).isGreaterThanOrEqualTo(0);
            assertThat(events.indexOf(fell.get(0))).isGreaterThan(landed);
            Position playerAt = floor.positionOf(floor.playerId());
            List<Position> cells = new ArrayList<>();
            for (EnemyFell event : fell) {
                Enemy goblin = floor.enemy(event.enemy()).orElseThrow();
                assertThat(goblin.kind()).isEqualTo(StandardEnemyCatalog.GOBLIN);
                assertThat(goblin.level()).isEqualTo(LEVEL);
                assertThat(floor.positionOf(event.enemy())).isEqualTo(event.at());
                assertThat(floor.bossRoom().contains(event.at())).isTrue();
                assertThat(event.at()).isNotEqualTo(playerAt);
                assertThat(event.at().isAdjacentTo(playerAt)).isFalse();
                assertThat(floor.cellsOf(floor.bossId().orElseThrow())).doesNotContain(event.at());
                cells.add(event.at());
            }
            assertThat(cells).doesNotHaveDuplicates();
        }
        assertThat(falls).isGreaterThan(0);
        assertThat(misses).isGreaterThan(falls);
    }

    @Test
    void landing_dropsOnlyTheGoblinsThatFitUnderFourAlive() {
        int[] alive = {0, 1, 2, 3, 4, 5};
        int[] expected = {2, 2, 2, 1, 0, 0};
        for (int i = 0; i < alive.length; i++) {
            for (long seed = 1; seed <= SEEDS; seed++) {
                Run run = bossFight(seed, alive[i]);

                List<EnemyFell> fell = fell(Sessions.enemyTurnRunner().run(run, LEVEL));

                assertThat(fell)
                        .as("%d vivi, seed %d", alive[i], seed)
                        .hasSize(rollSucceeds(seed) ? expected[i] : 0);
            }
        }
    }

    @Test
    void landing_freshGoblins_takeTheNextIdsOfTheRun() {
        long seed = 1;
        while (!rollSucceeds(seed)) {
            seed++;
        }
        Run run = bossFight(seed, 0);
        int nextId = run.ids().peek();

        List<EnemyFell> fell = fell(Sessions.enemyTurnRunner().run(run, LEVEL));

        assertThat(fell)
                .extracting(EnemyFell::enemy)
                .containsExactly(new EntityId(nextId), new EntityId(nextId + 1));
    }

    /** Il tiro del gioco è il primo del flusso di gameplay: lo si rifà dallo stesso seed. */
    private static boolean rollSucceeds(long seed) {
        SeededRandomSource root = new SeededRandomSource(seed);
        root.split();
        RandomSource gameplay = root.split();
        return gameplay.nextInt(100) < EnemyTurnRunner.FALL_CHANCE;
    }

    private static int indexOfLanded(List<GameEvent> events) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) instanceof Landed) {
                return i;
            }
        }
        return -1;
    }

    private static List<EnemyFell> fell(List<GameEvent> events) {
        return events.stream()
                .filter(EnemyFell.class::isInstance)
                .map(EnemyFell.class::cast)
                .toList();
    }

    /**
     * Una stanza del boss con lo slime in aria sopra la sua ombra e {@code goblins} goblin vivi.
     */
    private static Run bossFight(long seed, int goblins) {
        StringBuilder last = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            last.append(i < goblins ? 'g' : '.');
        }
        String[] rows = {
            "@.....S.......",
            "..............",
            "..............",
            "..............",
            "..............",
            "..............",
            "..............",
            last.toString(),
            "..............",
        };
        FloorSource source =
                new FloorSource() {
                    @Override
                    public Floor create(FloorSpec spec, RandomSource random, EntityIdSequence ids) {
                        Floor floor = FloorFixtures.parseBossRoom(ids, rows);
                        EntityId slime = floor.bossId().orElseThrow();
                        floor.chargeJump(slime);
                        floor.announceJump(slime, new Position(6, 0));
                        return floor;
                    }

                    @Override
                    public Floor createNext(
                            FloorSpec spec,
                            RandomSource random,
                            EntityIdSequence ids,
                            Player player) {
                        throw new UnsupportedOperationException();
                    }
                };
        return Run.start(seed, new FloorSpec(14, 9, 0, 0, 0, 1), source);
    }
}
