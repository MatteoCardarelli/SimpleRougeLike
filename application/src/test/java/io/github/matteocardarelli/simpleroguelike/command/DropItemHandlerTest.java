package io.github.matteocardarelli.simpleroguelike.command;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.ItemPutDown;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import org.junit.jupiter.api.Test;

class DropItemHandlerTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId SWORD = new EntityId(2);
    private static final DropItemHandler DROP = new DropItemHandler();

    @Test
    void drop_inInventory_putsItDownWithoutConsumingTheTurn() {
        Run run = run("@/.");
        run.floor().move(PLAYER, Direction.RIGHT);
        run.floor().pickUp(PLAYER);

        CommandOutcome outcome = DROP.handle(new DropItemCommand(SWORD), run);

        assertThat(outcome.turnConsumed()).isFalse();
        assertThat(outcome.events()).singleElement().isInstanceOf(ItemPutDown.class);
        assertThat(run.floor().player().inventory().find(SWORD)).isEmpty();
        assertThat(run.floor().itemAt(new Position(1, 0))).isPresent();
    }

    @Test
    void drop_notInInventory_doesNothing() {
        Run run = run("@/.");

        assertThat(DROP.handle(new DropItemCommand(SWORD), run)).isSameAs(CommandOutcome.none());
    }

    private static Run run(String... rows) {
        return Run.start(1, new FloorSpec(3, 3, 0, 0, 0, 1), new FixedFloorSource(rows));
    }
}
