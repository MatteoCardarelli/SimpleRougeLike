package io.github.matteocardarelli.simpleroguelike.session;

import static io.github.matteocardarelli.simpleroguelike.session.Sessions.ACTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.ai.BehaviorBinding;
import io.github.matteocardarelli.simpleroguelike.ai.EnemyBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.StepIntent;
import io.github.matteocardarelli.simpleroguelike.ai.WaitIntent;
import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnemyTurnRunnerTest {

    private static final BehaviorKey CHASE_OR_WANDER = StandardEnemyCatalog.CHASE_OR_WANDER;

    @Test
    void run_eachEnemyActsInIdOrder() {
        List<EntityId> order = new ArrayList<>();
        EnemyBehavior recorder =
                (enemy, floor, random) -> {
                    order.add(enemy.id());
                    return new WaitIntent();
                };
        Run run = run("g.@.g", ".g...");

        runner(recorder).run(run);

        assertThat(order).containsExactly(new EntityId(1), new EntityId(3), new EntityId(4));
    }

    @Test
    void run_stepIntent_usesTheSameRuleAsThePlayer() {
        Run run = run("@.g");

        List<GameEvent> events = runner((e, f, r) -> new StepIntent(Direction.LEFT)).run(run);

        assertThat(events).singleElement().isInstanceOf(Moved.class);
        assertThat(run.floor().positionOf(new EntityId(2))).isEqualTo(new Position(1, 0));
    }

    @Test
    void run_stepIntoThePlayer_isAnAttack() {
        Run run = run("@g");

        List<GameEvent> events = runner((e, f, r) -> new StepIntent(Direction.LEFT)).run(run);

        assertThat(events).first().isInstanceOf(Attacked.class);
    }

    @Test
    void run_waitIntent_changesNothing() {
        Run run = run("@.g");

        assertThat(runner((e, f, r) -> new WaitIntent()).run(run)).isEmpty();
        assertThat(run.floor().positionOf(new EntityId(2))).isEqualTo(new Position(2, 0));
    }

    @Test
    void run_stopsAsSoonAsThePlayerDies() {
        Run run = run("g@g");
        run.floor().damage(new EntityId(2), 98);
        List<EntityId> acted = new ArrayList<>();
        EnemyBehavior attack =
                (enemy, floor, random) -> {
                    acted.add(enemy.id());
                    return new StepIntent(
                            floor.positionOf(enemy.id()).x() == 0
                                    ? Direction.RIGHT
                                    : Direction.LEFT);
                };

        runner(attack).run(run);

        assertThat(acted).containsExactly(new EntityId(1));
        assertThat(run.floor().player().isAlive()).isFalse();
    }

    @Test
    void run_deadPlayerAtTheStart_nobodyActs() {
        Run run = run("@g");
        run.floor().damage(new EntityId(1), 1000);

        assertThat(runner((e, f, r) -> new StepIntent(Direction.LEFT)).run(run)).isEmpty();
    }

    @Test
    void run_enemyWithAnUnknownBehavior_isAnError() {
        Run run = run("@g");
        EnemyTurnRunner runner =
                new EnemyTurnRunner(
                        List.of(
                                new BehaviorBinding(
                                        CHASE_OR_WANDER, (e, f, r) -> new WaitIntent())),
                        List::of,
                        ACTIONS);
        Enemy goblin = run.floor().enemies().get(0);

        assertThat(goblin.behavior()).isEqualTo(CHASE_OR_WANDER);
        assertThat(runner.run(run)).isEmpty();
        EnemyTurnRunner empty = new EnemyTurnRunner(List.of(), List::of, ACTIONS);
        assertThatThrownBy(() -> empty.run(run)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_catalogKeyWithoutBehavior_isRejected() {
        assertThatThrownBy(
                        () -> new EnemyTurnRunner(List.of(), new StandardEnemyCatalog(), ACTIONS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chase_or_wander");
    }

    @Test
    void constructor_sameKeyTwice_isRejected() {
        BehaviorBinding one = new BehaviorBinding(CHASE_OR_WANDER, (e, f, r) -> new WaitIntent());
        BehaviorBinding two = new BehaviorBinding(CHASE_OR_WANDER, (e, f, r) -> new WaitIntent());

        assertThatThrownBy(
                        () ->
                                new EnemyTurnRunner(
                                        List.of(one, two), new StandardEnemyCatalog(), ACTIONS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static EnemyTurnRunner runner(EnemyBehavior behavior) {
        return new EnemyTurnRunner(
                List.of(new BehaviorBinding(CHASE_OR_WANDER, behavior)),
                new StandardEnemyCatalog(),
                ACTIONS);
    }

    private static Run run(String... rows) {
        return Run.start(1, new FloorSpec(3, 3, 0, 0, 0, 1), new FixedFloorSource(rows));
    }
}
