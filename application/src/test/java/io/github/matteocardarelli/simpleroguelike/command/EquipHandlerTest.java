package io.github.matteocardarelli.simpleroguelike.command;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.ItemEquipped;
import io.github.matteocardarelli.simpleroguelike.event.ItemUnequipped;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import org.junit.jupiter.api.Test;

class EquipHandlerTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId SWORD = new EntityId(2);
    private static final EquipHandler EQUIP = new EquipHandler();
    private static final UnequipHandler UNEQUIP = new UnequipHandler();

    @Test
    void equip_inInventory_equipsWithoutConsumingTheTurn() {
        Run run = runWithTheSwordInTheInventory();

        CommandOutcome outcome = EQUIP.handle(new EquipCommand(SWORD), run);

        assertThat(outcome.turnConsumed()).isFalse();
        assertThat(outcome.events()).singleElement().isInstanceOf(ItemEquipped.class);
        assertThat(run.floor().player().isEquipped(SWORD)).isTrue();
    }

    @Test
    void unequip_equipped_unequipsWithoutConsumingTheTurn() {
        Run run = runWithTheSwordInTheInventory();
        EQUIP.handle(new EquipCommand(SWORD), run);

        CommandOutcome outcome = UNEQUIP.handle(new UnequipCommand(SWORD), run);

        assertThat(outcome.turnConsumed()).isFalse();
        assertThat(outcome.events()).singleElement().isInstanceOf(ItemUnequipped.class);
        assertThat(run.floor().player().isEquipped(SWORD)).isFalse();
    }

    @Test
    void equipAndUnequip_notInInventory_doNothing() {
        Run run = run("@/");

        assertThat(EQUIP.handle(new EquipCommand(SWORD), run)).isSameAs(CommandOutcome.none());
        assertThat(UNEQUIP.handle(new UnequipCommand(SWORD), run)).isSameAs(CommandOutcome.none());
    }

    @Test
    void equip_aPotion_doesNothing() {
        Run run = run("@!");
        run.floor().move(PLAYER, Direction.RIGHT);
        run.floor().pickUp(PLAYER);

        assertThat(EQUIP.handle(new EquipCommand(new EntityId(2)), run))
                .isSameAs(CommandOutcome.none());
    }

    @Test
    void useItem_onEquipment_doesNothing() {
        Run run = runWithTheSwordInTheInventory();

        assertThat(new UseItemHandler().handle(new UseItemCommand(SWORD), run))
                .isSameAs(CommandOutcome.none());
        assertThat(run.floor().player().inventory().find(SWORD)).isPresent();
    }

    private static Run runWithTheSwordInTheInventory() {
        Run run = run("@/");
        run.floor().move(PLAYER, Direction.RIGHT);
        run.floor().pickUp(PLAYER);
        return run;
    }

    private static Run run(String... rows) {
        return Run.start(1, new FloorSpec(3, 3, 0, 0, 0, 1), new FixedFloorSource(rows));
    }
}
