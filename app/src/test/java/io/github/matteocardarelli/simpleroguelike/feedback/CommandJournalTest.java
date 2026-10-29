package io.github.matteocardarelli.simpleroguelike.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandJournalTest {

    @Test
    void record_keepsTheCommandsInOrder() {
        CommandJournal journal = new CommandJournal();
        journal.start(42);

        journal.record(new MoveCommand(Direction.UP));
        journal.record(new MoveCommand(Direction.LEFT));

        assertThat(journal.seed()).isEqualTo(42);
        assertThat(journal.commands())
                .containsExactly(new MoveCommand(Direction.UP), new MoveCommand(Direction.LEFT));
    }

    @Test
    void start_forgetsThePreviousGame() {
        CommandJournal journal = new CommandJournal();
        journal.start(1);
        journal.record(new MoveCommand(Direction.UP));

        journal.start(2);

        assertThat(journal.seed()).isEqualTo(2);
        assertThat(journal.commands()).isEmpty();
    }

    @Test
    void commands_isACopyThatCannotBeModified() {
        CommandJournal journal = new CommandJournal();
        journal.start(1);
        List<Command> before = journal.commands();

        journal.record(new MoveCommand(Direction.UP));

        assertThat(before).isEmpty();
        assertThatThrownBy(() -> journal.commands().add(new MoveCommand(Direction.DOWN)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
