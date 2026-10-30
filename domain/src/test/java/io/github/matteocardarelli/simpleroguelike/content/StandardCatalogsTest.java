package io.github.matteocardarelli.simpleroguelike.content;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemDefinition;
import io.github.matteocardarelli.simpleroguelike.effect.HealEffect;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import org.junit.jupiter.api.Test;

class StandardCatalogsTest {

    @Test
    void items_containThePotionAndTheGreaterPotion() {
        assertThat(new StandardItemCatalog().all())
                .containsExactly(
                        new ItemDefinition(
                                StandardItemCatalog.POTION, new HealEffect(25), true, 10),
                        new ItemDefinition(
                                StandardItemCatalog.GREATER_POTION, new HealEffect(60), true, 3));
    }

    @Test
    void enemies_containTheGoblin() {
        assertThat(new StandardEnemyCatalog().all())
                .containsExactly(
                        new EnemyDefinition(
                                StandardEnemyCatalog.GOBLIN,
                                new Stats(30, 5),
                                25,
                                8,
                                StandardEnemyCatalog.CHASE_OR_WANDER,
                                10));
    }
}
