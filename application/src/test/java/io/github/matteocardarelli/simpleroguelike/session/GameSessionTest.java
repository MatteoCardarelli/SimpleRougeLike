package io.github.matteocardarelli.simpleroguelike.session;

import static io.github.matteocardarelli.simpleroguelike.session.Sessions.DAMAGE;
import static io.github.matteocardarelli.simpleroguelike.session.Sessions.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.CommandHandler;
import io.github.matteocardarelli.simpleroguelike.command.CommandOutcome;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.command.UseItemCommand;
import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.ExperienceGained;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.snapshot.Visibility;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId GOBLIN = new EntityId(2);

    @Test
    void newGame_startsPlayingAtTurnOne() {
        GameSession session = session(new FixedFloorSource("#####", "#@.g#", "#####"));

        TurnResult result = session.newGame(42);

        assertThat(result.revision()).isEqualTo(1);
        assertThat(result.accepted()).isTrue();
        assertThat(result.events()).isEmpty();
        assertThat(result.snapshot().seed()).isEqualTo(42);
        assertThat(result.snapshot().turn()).isEqualTo(1);
        assertThat(result.snapshot().phase()).isEqualTo(Phase.PLAYING);
        assertThat(result.snapshot().player().position()).isEqualTo(new Position(1, 1));
    }

    @Test
    void current_beforeNewGame_throws() {
        GameSession session = session(new FixedFloorSource("@g"));

        assertThatThrownBy(session::current).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> session.handle(new MoveCommand(Direction.UP)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void current_returnsTheLastResultWithoutANewRevision() {
        GameSession session = session(new FixedFloorSource("#####", "#@.g#", "#####"));
        session.newGame(1);
        TurnResult moved = session.handle(new MoveCommand(Direction.RIGHT));

        assertThat(session.current()).isSameAs(moved);
        assertThat(session.current()).isSameAs(moved);
    }

    /** Un passo contro un muro è il modo di aspettare un turno: il player resta fermo. */
    @Test
    void handle_intoAWall_playerStaysStill_andEnemiesAct() {
        GameSession session = session(new FixedFloorSource("#####", "#@..#", "#..g#", "#####"));
        session.newGame(1);

        TurnResult result = session.handle(new MoveCommand(Direction.LEFT));

        assertThat(result.accepted()).isTrue();
        assertThat(result.revision()).isEqualTo(2);
        assertThat(result.snapshot().turn()).isEqualTo(2);
        assertThat(result.snapshot().player().position()).isEqualTo(new Position(1, 1));
        assertThat(result.events()).singleElement().isInstanceOf(Moved.class);
        assertThat(result.snapshot().visibleEnemies())
                .extracting(ActorView::position)
                .doesNotContain(new Position(3, 2));
    }

    @Test
    void handle_move_thenEnemiesAct_andTheTurnAdvances() {
        GameSession session =
                session(new FixedFloorSource("#########", "#@......#", "#......g#", "#########"));
        session.newGame(1);

        TurnResult result = session.handle(new MoveCommand(Direction.RIGHT));

        assertThat(result.events())
                .extracting(GameEvent::getClass)
                .containsExactly(Moved.class, Moved.class);
        assertThat(((Moved) result.events().get(0)).actor()).isEqualTo(PLAYER);
        assertThat(result.snapshot().turn()).isEqualTo(2);
    }

    /** Bug #1: un nemico abbastanza vicino non si avvicinava al player. */
    @Test
    void handle_enemyWithinAggroRange_stepsCloserToThePlayer() {
        GameSession session =
                session(new FixedFloorSource("#########", "#@......#", "#......g#", "#########"));
        session.newGame(1);

        GameSnapshot after = session.handle(new MoveCommand(Direction.DOWN)).snapshot();

        Position player = after.player().position();
        Position goblin = after.visibleEnemies().get(0).position();
        assertThat(player).isEqualTo(new Position(1, 2));
        assertThat(goblin).isEqualTo(new Position(6, 2));
        assertThat(goblin.distanceTo(player)).isLessThan(new Position(7, 2).distanceTo(player));
    }

    @Test
    void handle_lastEnemyKilled_isVictory_andCommandsAreRejected() {
        GameSession session = session(new FixedFloorSource("####", "#@g#", "####"));
        session.newGame(1);

        // Un colpo può mancare: si attacca finché il goblin non muore, entro un limite.
        TurnResult kill = session.handle(new MoveCommand(Direction.RIGHT));
        for (int attempt = 0; attempt < 30 && kill.snapshot().phase() == Phase.PLAYING; attempt++) {
            kill = session.handle(new MoveCommand(Direction.RIGHT));
        }
        TurnResult after = session.handle(new MoveCommand(Direction.RIGHT));

        assertThat(kill.events()).hasAtLeastOneElementOfType(Died.class);
        assertThat(kill.events()).hasAtLeastOneElementOfType(ExperienceGained.class);
        assertThat(kill.snapshot().phase()).isEqualTo(Phase.VICTORY);
        assertThat(kill.snapshot().visibleEnemies()).isEmpty();
        assertThat(after.accepted()).isFalse();
        assertThat(after.events()).isEmpty();
        assertThat(after.revision()).isEqualTo(kill.revision() + 1);
    }

    /** Bug #2: da morti si poteva continuare a giocare, e una pozione riportava in vita. */
    @Test
    void handle_afterDeath_rejectsEveryCommand_andTheGameOverStays() {
        TurnHook killer = hook(1, run -> DAMAGE.apply(run.floor(), Optional.empty(), PLAYER, 1000));
        GameSession session =
                session(
                        new FixedFloorSource("######", "#@!.g#", "######"),
                        20,
                        List.of(killer),
                        List.of());
        session.newGame(1);
        TurnResult death = session.handle(new MoveCommand(Direction.RIGHT));

        TurnResult move = session.handle(new MoveCommand(Direction.LEFT));
        TurnResult potion = session.handle(new UseItemCommand(new EntityId(2)));

        assertThat(death.snapshot().phase()).isEqualTo(Phase.GAME_OVER);
        assertThat(death.snapshot().inventory()).hasSize(1);
        for (TurnResult rejected : List.of(move, potion)) {
            assertThat(rejected.accepted()).isFalse();
            assertThat(rejected.events()).isEmpty();
            assertThat(rejected.snapshot().phase()).isEqualTo(Phase.GAME_OVER);
            assertThat(rejected.snapshot().player().health()).isZero();
            assertThat(rejected.snapshot().player().position()).isEqualTo(new Position(2, 1));
        }
    }

    @Test
    void handle_playerKilledByAnEnemy_closesTheTurn() {
        int[] hookCalls = new int[1];
        TurnHook weaken =
                hook(
                        1,
                        run -> {
                            hookCalls[0]++;
                            int damage = hookCalls[0] == 1 ? 98 : 0;
                            return DAMAGE.apply(run.floor(), Optional.empty(), PLAYER, damage);
                        });
        GameSession session =
                session(
                        new FixedFloorSource("#####", "#.@g#", "#.g.#", "#####"),
                        20,
                        List.of(weaken),
                        List.of());
        session.newGame(1);
        session.handle(new MoveCommand(Direction.LEFT));

        TurnResult result = session.handle(new MoveCommand(Direction.RIGHT));

        assertThat(result.snapshot().phase()).isEqualTo(Phase.GAME_OVER);
        assertThat(result.events())
                .filteredOn(e -> e instanceof Attacked attack && !attack.attacker().equals(PLAYER))
                .as("solo il primo goblin attacca")
                .hasSize(1);
        assertThat(hookCalls[0]).as("gli hook non girano dopo la morte").isEqualTo(1);
        assertThat(result.snapshot().turn()).isEqualTo(3);
    }

    @Test
    void handle_hooksRunInOrder_andAnEnemyKilledByAHookIsRemoved() {
        List<String> calls = new ArrayList<>();
        TurnHook second =
                hook(
                        20,
                        run -> {
                            calls.add("secondo");
                            return DAMAGE.apply(run.floor(), Optional.empty(), GOBLIN, 1000);
                        });
        TurnHook first =
                hook(
                        10,
                        run -> {
                            calls.add("primo");
                            return List.of();
                        });
        GameSession session =
                session(
                        new FixedFloorSource("#######", "#@...g#", "#######"),
                        20,
                        List.of(second, first),
                        List.of());
        session.newGame(1);

        TurnResult result = session.handle(new MoveCommand(Direction.RIGHT));

        assertThat(calls).containsExactly("primo", "secondo");
        assertThat(result.events()).last().isInstanceOf(Died.class);
        assertThat(result.snapshot().visibleEnemies()).isEmpty();
        assertThat(result.snapshot().phase()).isEqualTo(Phase.VICTORY);
    }

    @Test
    void constructor_twoHooksWithTheSameOrder_isRejected() {
        TurnHook a = hook(5, run -> List.of());
        TurnHook b = hook(5, run -> List.of());

        assertThatThrownBy(() -> session(new FixedFloorSource("@g"), 5, List.of(a, b), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_nonPositiveRadius_isRejected() {
        assertThatThrownBy(() -> session(new FixedFloorSource("@g"), 0, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handle_enemyMovingInTheDark_isNotReported() {
        GameSession session =
                session(
                        new FixedFloorSource(
                                "##############",
                                "#@...........#",
                                "#...........g#",
                                "##############"),
                        2,
                        List.of(),
                        List.of());
        session.newGame(1);

        TurnResult result = session.handle(new MoveCommand(Direction.DOWN));

        assertThat(result.events()).hasSize(1);
        assertThat(((Moved) result.events().get(0)).actor()).isEqualTo(PLAYER);
        assertThat(result.snapshot().visibleEnemies()).isEmpty();
        assertThat(result.snapshot().cells().get(new Position(12, 2)).visibility())
                .isEqualTo(Visibility.UNEXPLORED);
    }

    @Test
    void handle_usingAPotion_healsAndConsumesTheTurn() {
        TurnHook hurtOnce = new HurtOnce(40);
        GameSession session =
                session(
                        new FixedFloorSource("#######", "#@!..g#", "#######"),
                        20,
                        List.of(hurtOnce),
                        List.of());
        session.newGame(1);
        session.handle(new MoveCommand(Direction.RIGHT));

        TurnResult result = session.handle(new UseItemCommand(new EntityId(2)));

        assertThat(result.snapshot().player().health()).isEqualTo(85);
        assertThat(result.snapshot().inventory()).isEmpty();
        assertThat(result.snapshot().turn()).isEqualTo(3);
    }

    @Test
    void handle_unknownItem_doesNothing_andTheTurnDoesNotPass() {
        GameSession session = session(new FixedFloorSource("#####", "#@.g#", "#####"));
        session.newGame(1);

        TurnResult result = session.handle(new UseItemCommand(new EntityId(99)));

        assertThat(result.accepted()).isTrue();
        assertThat(result.events()).isEmpty();
        assertThat(result.snapshot().turn()).isEqualTo(1);
    }

    @Test
    void extension_newCommandAndHook_workWithoutChangingTheSession() {
        record RestCommand() implements Command {}
        CommandHandler<RestCommand> rest =
                new CommandHandler<>() {
                    @Override
                    public Class<RestCommand> commandType() {
                        return RestCommand.class;
                    }

                    @Override
                    public CommandOutcome handle(RestCommand command, Run run) {
                        return new CommandOutcome(List.of(), true);
                    }
                };
        int[] turnsSeen = new int[1];
        TurnHook counter =
                hook(
                        1,
                        run -> {
                            turnsSeen[0]++;
                            return List.of();
                        });
        GameSession session =
                session(
                        new FixedFloorSource("#########", "#@......#", "#......g#", "#########"),
                        20,
                        List.of(counter),
                        List.of(rest));
        session.newGame(1);

        session.handle(new RestCommand());
        TurnResult result = session.handle(new RestCommand());

        assertThat(turnsSeen[0]).isEqualTo(2);
        assertThat(result.snapshot().turn()).isEqualTo(3);
        assertThat(result.snapshot().visibleEnemies().get(0).position())
                .isEqualTo(new Position(5, 2));
    }

    @Test
    void newGame_sameSeedAndCommands_givesSameResults() {
        List<Command> commands =
                List.of(
                        new MoveCommand(Direction.RIGHT),
                        new MoveCommand(Direction.DOWN),
                        new MoveCommand(Direction.RIGHT),
                        new MoveCommand(Direction.UP));

        assertThat(play(7, commands)).isEqualTo(play(7, commands));
    }

    @Test
    void newGame_afterAGame_startsOverWithAGrowingRevision() {
        GameSession session = session(new FixedFloorSource("####", "#@g#", "####"));
        session.newGame(1);
        session.handle(new MoveCommand(Direction.RIGHT));

        TurnResult again = session.newGame(2);

        assertThat(again.revision()).isEqualTo(3);
        assertThat(again.snapshot().turn()).isEqualTo(1);
        assertThat(again.snapshot().visibleEnemies().get(0).health()).isEqualTo(30);
    }

    private static List<TurnResult> play(long seed, List<Command> commands) {
        GameSession session =
                session(
                        new FixedFloorSource(
                                "##########",
                                "#@.......#",
                                "#....g...#",
                                "#.g......#",
                                "##########"));
        List<TurnResult> results = new ArrayList<>();
        results.add(session.newGame(seed));
        for (Command command : commands) {
            results.add(session.handle(command));
        }
        return results;
    }

    private static TurnHook hook(
            int order, java.util.function.Function<Run, List<GameEvent>> body) {
        return new TurnHook() {
            @Override
            public int order() {
                return order;
            }

            @Override
            public List<GameEvent> afterTurn(Run run) {
                return body.apply(run);
            }
        };
    }

    /** Toglie salute al player una volta sola, alla fine del primo turno. */
    private static final class HurtOnce implements TurnHook {
        private final int amount;
        private boolean done;

        HurtOnce(int amount) {
            this.amount = amount;
        }

        @Override
        public int order() {
            return 1;
        }

        @Override
        public List<GameEvent> afterTurn(Run run) {
            if (done) {
                return List.of();
            }
            done = true;
            return DAMAGE.apply(run.floor(), Optional.empty(), PLAYER, amount);
        }
    }
}
