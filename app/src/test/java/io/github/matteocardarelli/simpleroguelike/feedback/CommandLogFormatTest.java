package io.github.matteocardarelli.simpleroguelike.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.DropItemCommand;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.command.UseItemCommand;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandLogFormatTest {

    private final CommandLogFormat format = new CommandLogFormat();

    @Test
    void line_writesEachCommandByHand() {
        assertThat(format.line(new MoveCommand(Direction.UP))).isEqualTo("move up");
        assertThat(format.line(new MoveCommand(Direction.DOWN))).isEqualTo("move down");
        assertThat(format.line(new MoveCommand(Direction.LEFT))).isEqualTo("move left");
        assertThat(format.line(new MoveCommand(Direction.RIGHT))).isEqualTo("move right");
        assertThat(format.line(new UseItemCommand(new EntityId(12)))).isEqualTo("use item=12");
        assertThat(format.line(new DropItemCommand(new EntityId(5)))).isEqualTo("drop item=5");
    }

    @Test
    void parse_isTheInverseOfLine() {
        List<Command> commands = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            commands.add(new MoveCommand(direction));
        }
        commands.add(new UseItemCommand(new EntityId(7)));
        commands.add(new DropItemCommand(new EntityId(7)));

        for (Command command : commands) {
            assertThat(format.parse(format.line(command))).isEqualTo(command);
        }
    }

    @Test
    void parse_unknownCommand_throws() {
        assertThatThrownBy(() -> format.parse("jump high"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> format.parse("move north"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Un comando nuovo senza formato non si potrebbe rigiocare da una nota di collaudo. */
    @Test
    void everyCommand_hasAFormat() {
        List<String> commands = new ArrayList<>();
        for (JavaClass type :
                new ClassFileImporter().importPackages(Command.class.getPackageName())) {
            if (type.isAssignableTo(Command.class) && !type.isInterface()) {
                commands.add(type.getName());
            }
        }
        List<String> formatted = new ArrayList<>();
        for (Class<? extends Command> type : format.formattedTypes()) {
            formatted.add(type.getName());
        }

        assertThat(commands).isNotEmpty();
        assertThat(formatted).containsExactlyInAnyOrderElementsOf(commands);
    }
}
