package io.github.matteocardarelli.simpleroguelike.command;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.event.Healed;
import io.github.matteocardarelli.simpleroguelike.event.ItemUsed;
import io.github.matteocardarelli.simpleroguelike.event.PoisonCured;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import org.junit.jupiter.api.Test;

class UseItemHandlerTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final MoveHandler MOVE =
            new MoveHandler(
                    new ActionRules(
                            new CombatRules(
                                    new DamageRules(new LevelingRules(new Stats(20, 2), 150)))));
    private static final UseItemHandler USE = new UseItemHandler();

    @Test
    void useItem_inInventory_usesItAndConsumesTheTurn() {
        Run run = run("@!g");
        MOVE.handle(new MoveCommand(Direction.RIGHT), run);
        run.floor().damage(PLAYER, 30);

        CommandOutcome outcome = USE.handle(new UseItemCommand(new EntityId(2)), run);

        assertThat(outcome.turnConsumed()).isTrue();
        assertThat(outcome.events()).hasSize(2).first().isInstanceOf(ItemUsed.class);
        assertThat(outcome.events().get(1)).isInstanceOf(Healed.class);
    }

    @Test
    void useItem_notInInventory_doesNothing() {
        Run run = run("@!g");

        assertThat(USE.handle(new UseItemCommand(new EntityId(2)), run))
                .isSameAs(CommandOutcome.none());
        assertThat(run.floor().itemAt(new Position(1, 0))).isPresent();
    }

    @Test
    void useItem_anAntidoteWithoutPoison_doesNothing_andKeepsTheTurnAndTheItem() {
        Run run = run("@.g");
        Item antidote =
                new Item(new EntityId(50), FloorFixtures.item(StandardItemCatalog.ANTIDOTE));
        give(run.floor(), antidote);

        CommandOutcome outcome = USE.handle(new UseItemCommand(antidote.id()), run);

        assertThat(outcome.turnConsumed()).isFalse();
        assertThat(run.floor().player().inventory().find(antidote.id())).isPresent();
    }

    @Test
    void useItem_anAntidoteWhilePoisoned_curesAndConsumesTheTurn() {
        Run run = run("@.g");
        Item antidote =
                new Item(new EntityId(50), FloorFixtures.item(StandardItemCatalog.ANTIDOTE));
        give(run.floor(), antidote);
        run.floor().poison(PLAYER);

        CommandOutcome outcome = USE.handle(new UseItemCommand(antidote.id()), run);

        assertThat(outcome.turnConsumed()).isTrue();
        assertThat(outcome.events()).last().isInstanceOf(PoisonCured.class);
        assertThat(run.floor().player().isPoisoned()).isFalse();
    }

    private static void give(Floor floor, Item item) {
        floor.dropItem(floor.positionOf(floor.playerId()), item);
        floor.pickUp(floor.playerId());
    }

    private static Run run(String... rows) {
        return Run.start(1, new FloorSpec(3, 3, 0, 0, 0, 1), new FixedFloorSource(rows));
    }
}
