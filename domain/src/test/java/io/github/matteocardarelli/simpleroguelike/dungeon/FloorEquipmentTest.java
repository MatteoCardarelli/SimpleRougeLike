package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.event.ItemEquipped;
import io.github.matteocardarelli.simpleroguelike.event.ItemUnequipped;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.EquipmentSlot;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FloorEquipmentTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final Stats BASE = FloorFixtures.PLAYER_STATS;

    /** Raccoglie gli oggetti a destra del player, uno per passo. */
    private static Floor pickUpAll(String row) {
        Floor floor = FloorFixtures.parse(row);
        for (int i = 1; i < row.length(); i++) {
            floor.move(PLAYER, Direction.RIGHT);
            floor.pickUp(PLAYER);
        }
        return floor;
    }

    @Test
    void equip_addsTheBonusToTheStats_andTheItemStaysInTheInventory() {
        Floor floor = pickUpAll("@/");
        EntityId sword = new EntityId(2);

        var events = floor.equip(PLAYER, sword);

        assertThat(events)
                .containsExactly(
                        new ItemEquipped(sword, StandardItemCatalog.SWORD, new Position(1, 0)));
        assertThat(floor.player().stats().attack()).isEqualTo(BASE.attack() + 4);
        assertThat(floor.player().baseStats()).isEqualTo(BASE);
        assertThat(floor.player().inventory().find(sword)).isPresent();
        assertThat(floor.player().isEquipped(sword)).isTrue();
    }

    @Test
    void equip_inAnOccupiedSlot_replacesTheOldItem() {
        Floor floor = pickUpAll("@//");
        EntityId first = new EntityId(2);
        EntityId second = new EntityId(3);
        floor.equip(PLAYER, first);

        var events = floor.equip(PLAYER, second);

        Position at = new Position(2, 0);
        assertThat(events)
                .containsExactly(
                        new ItemUnequipped(first, StandardItemCatalog.SWORD, at),
                        new ItemEquipped(second, StandardItemCatalog.SWORD, at));
        assertThat(floor.player().stats().attack()).isEqualTo(BASE.attack() + 4);
        assertThat(floor.player().equipped()).containsOnlyKeys(EquipmentSlot.WEAPON);
    }

    @Test
    void equip_differentSlots_addUp() {
        Floor floor = pickUpAll("@/[");

        floor.equip(PLAYER, new EntityId(2));
        floor.equip(PLAYER, new EntityId(3));

        assertThat(floor.player().stats())
                .isEqualTo(BASE.plus(new Stats(0, 4)).plus(new Stats(0, 0, 2, 0, 0)));
        assertThat(floor.player().equipped().keySet())
                .containsExactly(EquipmentSlot.WEAPON, EquipmentSlot.ARMOR);
    }

    @Test
    void equip_alreadyEquipped_doesNothing() {
        Floor floor = pickUpAll("@/");
        floor.equip(PLAYER, new EntityId(2));

        assertThat(floor.equip(PLAYER, new EntityId(2))).isEmpty();
    }

    @Test
    void unequip_removesTheBonus_andKeepsTheItem() {
        Floor floor = pickUpAll("@/");
        EntityId sword = new EntityId(2);
        floor.equip(PLAYER, sword);

        var events = floor.unequip(PLAYER, sword);

        assertThat(events)
                .containsExactly(
                        new ItemUnequipped(sword, StandardItemCatalog.SWORD, new Position(1, 0)));
        assertThat(floor.player().stats()).isEqualTo(BASE);
        assertThat(floor.player().inventory().find(sword)).isPresent();
    }

    @Test
    void unequip_notEquipped_doesNothing() {
        Floor floor = pickUpAll("@/");

        assertThat(floor.unequip(PLAYER, new EntityId(2))).isEmpty();
    }

    @Test
    void ringOfVigor_raisesTheMaximum_andTakingItOffKeepsHealthWithinIt() {
        Floor floor = pickUpAll("@=");
        EntityId ring = new EntityId(2);

        floor.equip(PLAYER, ring);
        assertThat(floor.player().stats().maxHealth()).isEqualTo(BASE.maxHealth() + 20);
        assertThat(floor.player().health()).isEqualTo(BASE.maxHealth());
        floor.heal(PLAYER, 20);
        assertThat(floor.player().health()).isEqualTo(BASE.maxHealth() + 20);

        floor.unequip(PLAYER, ring);
        assertThat(floor.player().health()).isEqualTo(BASE.maxHealth());
    }

    @Test
    void equip_aPotion_isRejected() {
        Floor floor = pickUpAll("@!");

        assertThatThrownBy(() -> floor.equip(PLAYER, new EntityId(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equip_anItemNotInTheInventory_isRejected() {
        Floor floor = FloorFixtures.parse("@/");

        assertThatThrownBy(() -> floor.equip(PLAYER, new EntityId(2)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> floor.unequip(PLAYER, new EntityId(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equipmentDefinition_cannotBeConsumable() {
        assertThatThrownBy(
                        () ->
                                new ItemDefinition(
                                        StandardItemCatalog.SWORD,
                                        ItemEffect.NONE,
                                        true,
                                        1,
                                        Optional.of(
                                                new Equipment(
                                                        EquipmentSlot.WEAPON, new Stats(0, 1)))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noEffect_givesNoEvents() {
        Floor floor = FloorFixtures.parse("@");

        assertThat(
                        ItemEffect.NONE.apply(
                                new EffectContext(PLAYER, floor, FixedRandomSource.ALWAYS_HIT)))
                .isEmpty();
    }
}
