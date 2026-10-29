package io.github.matteocardarelli.simpleroguelike.command;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoveHandlerTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final MoveHandler MOVE =
            new MoveHandler(
                    new ActionRules(
                            new CombatRules(new DamageRules(new LevelingRules(20, 2, 150)))));
    private static final UseItemHandler USE = new UseItemHandler();

    @Test
    void move_freeCell_movesAndConsumesTheTurn() {
        Run run = run("@.g");

        CommandOutcome outcome = MOVE.handle(new MoveCommand(Direction.RIGHT), run);

        assertThat(outcome.turnConsumed()).isTrue();
        assertThat(outcome.events()).singleElement().isInstanceOf(Moved.class);
        assertThat(run.floor().positionOf(PLAYER)).isEqualTo(new Position(1, 0));
    }

    @Test
    void move_intoAWall_staysStillAndConsumesTheTurn() {
        Run run = run("#@g");

        CommandOutcome outcome = MOVE.handle(new MoveCommand(Direction.LEFT), run);

        assertThat(outcome).isEqualTo(new CommandOutcome(List.of(), true));
        assertThat(run.floor().positionOf(PLAYER)).isEqualTo(new Position(1, 0));
    }

    @Test
    void handlers_declareTheirCommandType() {
        assertThat(MOVE.commandType()).isEqualTo(MoveCommand.class);
        assertThat(USE.commandType()).isEqualTo(UseItemCommand.class);
    }

    private static Run run(String... rows) {
        return Run.start(1, new FloorSpec(3, 3, 0, 0, 0, 1), new FixedFloorSource(rows));
    }
}
