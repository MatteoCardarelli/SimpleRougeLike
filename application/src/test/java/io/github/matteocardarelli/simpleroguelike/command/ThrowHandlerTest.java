package io.github.matteocardarelli.simpleroguelike.command;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.Fired;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import io.github.matteocardarelli.simpleroguelike.rules.RangedRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import org.junit.jupiter.api.Test;

class ThrowHandlerTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId KNIVES = new EntityId(2);
    private static final ThrowHandler THROW =
            new ThrowHandler(
                    new RangedRules(new DamageRules(new LevelingRules(new Stats(20, 2), 150))));

    @Test
    void throw_spendsOneKnife_andConsumesTheTurn() {
        Run run = run("@--...g");
        run.floor().move(PLAYER, Direction.RIGHT);
        run.floor().pickUp(PLAYER);
        run.floor().move(PLAYER, Direction.RIGHT);
        run.floor().pickUp(PLAYER);

        CommandOutcome outcome = THROW.handle(new ThrowCommand(KNIVES, Direction.RIGHT), run);

        assertThat(outcome.turnConsumed()).isTrue();
        assertThat(outcome.events().get(0)).isInstanceOf(Fired.class);
        assertThat(run.floor().player().inventory().find(KNIVES).orElseThrow().count())
                .isEqualTo(1);
    }

    @Test
    void throw_withoutTheItem_orAnItemThatDoesNotFly_doesNothing() {
        Run empty = run("@-....g");
        Run potion = run("@!....g");
        potion.floor().move(PLAYER, Direction.RIGHT);
        potion.floor().pickUp(PLAYER);

        assertThat(THROW.handle(new ThrowCommand(KNIVES, Direction.RIGHT), empty))
                .isSameAs(CommandOutcome.none());
        assertThat(THROW.handle(new ThrowCommand(new EntityId(2), Direction.RIGHT), potion))
                .isSameAs(CommandOutcome.none());
    }

    private static Run run(String... rows) {
        return Run.start(1, new FloorSpec(7, 3, 0, 0, 0, 1), new FixedFloorSource(rows));
    }
}
