package io.github.matteocardarelli.simpleroguelike.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandDispatcherTest {

    private static final CommandOutcome REST = new CommandOutcome(List.of(), true);

    record RestCommand() implements Command {}

    record OtherCommand() implements Command {}

    @Test
    void dispatch_usesTheHandlerOfTheCommandClass() {
        CommandDispatcher dispatcher = new CommandDispatcher(List.of(new RestHandler()));

        assertThat(dispatcher.dispatch(new RestCommand(), run())).isSameAs(REST);
    }

    @Test
    void dispatch_noHandler_isRejected() {
        CommandDispatcher dispatcher = new CommandDispatcher(List.of(new RestHandler()));

        assertThatThrownBy(() -> dispatcher.dispatch(new OtherCommand(), run()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OtherCommand");
    }

    @Test
    void constructor_twoHandlersForTheSameCommand_isRejected() {
        assertThatThrownBy(
                        () -> new CommandDispatcher(List.of(new RestHandler(), new RestHandler())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dispatch_severalHandlers_eachGetsItsOwnCommand() {
        CommandDispatcher dispatcher =
                new CommandDispatcher(List.of(new RestHandler(), new UseItemHandler()));

        assertThat(dispatcher.dispatch(new RestCommand(), run()).turnConsumed()).isTrue();
        assertThat(dispatcher.dispatch(new UseItemCommand(new EntityId(99)), run()).turnConsumed())
                .isFalse();
    }

    @Test
    void commandOutcome_copiesEvents_andNoneIsEmpty() {
        assertThat(CommandOutcome.none().events()).isEmpty();
        assertThat(CommandOutcome.none().turnConsumed()).isFalse();
        assertThatThrownBy(() -> REST.events().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void moveCommand_isAValue() {
        assertThat(new MoveCommand(Direction.UP)).isEqualTo(new MoveCommand(Direction.UP));
    }

    private static Run run() {
        return Run.start(1, new FloorSpec(3, 3, 0, 0, 0, 1), new FixedFloorSource("@g"));
    }

    private static final class RestHandler implements CommandHandler<RestCommand> {
        @Override
        public Class<RestCommand> commandType() {
            return RestCommand.class;
        }

        @Override
        public CommandOutcome handle(RestCommand command, Run run) {
            return REST;
        }
    }
}
