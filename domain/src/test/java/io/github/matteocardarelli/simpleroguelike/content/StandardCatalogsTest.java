package io.github.matteocardarelli.simpleroguelike.content;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemDefinition;
import io.github.matteocardarelli.simpleroguelike.effect.HealEffect;
import io.github.matteocardarelli.simpleroguelike.model.EquipmentSlot;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import org.junit.jupiter.api.Test;

class StandardCatalogsTest {

    @Test
    void items_containThePotionAndTheGreaterPotion() {
        assertThat(new StandardItemCatalog().all())
                .startsWith(
                        new ItemDefinition(
                                StandardItemCatalog.POTION, new HealEffect(25), true, 10),
                        new ItemDefinition(
                                StandardItemCatalog.GREATER_POTION, new HealEffect(60), true, 3));
    }

    @Test
    void items_haveAWeaponAnArmorAndARing_rarerThanThePotion() {
        var equipment =
                new StandardItemCatalog()
                        .all().stream()
                                .filter(definition -> definition.equipment().isPresent())
                                .toList();

        assertThat(equipment)
                .extracting(definition -> definition.equipment().orElseThrow().slot())
                .contains(EquipmentSlot.WEAPON, EquipmentSlot.ARMOR, EquipmentSlot.RING);
        assertThat(equipment).allMatch(definition -> definition.spawnWeight() < 10);
        assertThat(equipment).noneMatch(ItemDefinition::consumable);
    }

    @Test
    void enemies_containTheGoblinTheArcherAndTheWolf() {
        assertThat(new StandardEnemyCatalog().all())
                .containsExactly(
                        new EnemyDefinition(
                                StandardEnemyCatalog.GOBLIN,
                                new Stats(30, 7, 1, 0, 5),
                                25,
                                8,
                                StandardEnemyCatalog.CHASE_OR_WANDER,
                                10),
                        new EnemyDefinition(
                                StandardEnemyCatalog.ARCHER,
                                new Stats(20, 6, 0, 5, 10),
                                30,
                                8,
                                StandardEnemyCatalog.KEEP_DISTANCE,
                                4,
                                5),
                        new EnemyDefinition(
                                StandardEnemyCatalog.WOLF,
                                new Stats(14, 5, 0, 5, 10),
                                15,
                                10,
                                StandardEnemyCatalog.PACK,
                                3,
                                0,
                                3));
    }

    @Test
    void items_haveTheThrowingKnife_thatStacks() {
        assertThat(new StandardItemCatalog().all())
                .filteredOn(
                        definition -> definition.kind().equals(StandardItemCatalog.THROWING_KNIFE))
                .singleElement()
                .satisfies(knife -> assertThat(knife.stackable()).isTrue());
    }
}
