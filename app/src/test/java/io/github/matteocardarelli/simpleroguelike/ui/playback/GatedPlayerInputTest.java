package io.github.matteocardarelli.simpleroguelike.ui.playback;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.TestGraph;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.ui.input.PlayerInput;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GatedPlayerInputTest {

    /** Registra le azioni che gli arrivano, come le riceverebbe il ViewModel. */
    private static final class RecordingInput implements PlayerInput {
        final List<String> actions = new ArrayList<>();

        @Override
        public void submit(Command command) {
            actions.add("submit " + ((MoveCommand) command).direction());
        }

        @Override
        public void toggleInventory() {
            actions.add("toggle");
        }

        @Override
        public void chooseItem(EntityId item) {
            actions.add("choose " + item.value());
        }
    }

    private final RecordingInput target = new RecordingInput();
    private final ControlledEventPlayer player = new ControlledEventPlayer();
    private final TurnPlayback playback = new TurnPlayback(player, new Scene(SceneTest.PLAYER));
    private final GatedPlayerInput input = new GatedPlayerInput(target, playback);

    @Test
    void whenIdle_forwardsAtOnce() {
        input.submit(new MoveCommand(Direction.UP));
        input.toggleInventory();
        input.chooseItem(new EntityId(3));

        assertThat(target.actions).containsExactly("submit UP", "toggle", "choose 3");
    }

    @Test
    void whilePlaying_keepsOnlyTheLastAction_untilFlush() {
        playback.setOnIdle(input::flush);
        playback.show(TestGraph.session(GameConfig.standard()).newGame(1), true);

        input.submit(new MoveCommand(Direction.UP));
        input.toggleInventory();
        input.submit(new MoveCommand(Direction.LEFT));
        List<String> beforeTheEnd = List.copyOf(target.actions);
        player.complete();

        assertThat(beforeTheEnd).isEmpty();
        assertThat(target.actions).containsExactly("submit LEFT");
    }
}
