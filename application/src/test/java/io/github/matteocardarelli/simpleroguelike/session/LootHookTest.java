package io.github.matteocardarelli.simpleroguelike.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.event.ItemDropped;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import org.junit.jupiter.api.Test;

class LootHookTest {

    private static final LootHook LOOT = new LootHook(new StandardItemCatalog());
    private static final FloorSpec SPEC = new FloorSpec(5, 3, 0, 0, 0, 1);

    @Test
    void afterTurn_aDeadGoblin_sometimesDropsAnItem_inItsCell() {
        EnemyDefinition goblin = new StandardEnemyCatalog().all().get(0);
        int drops = 0;
        for (long seed = 1; seed <= 200; seed++) {
            Run run = Run.start(seed, SPEC, new FixedFloorSource("@.g"));
            run.floor().damage(new EntityId(2), 1000);

            var events = LOOT.afterTurn(run);

            if (!events.isEmpty()) {
                drops++;
                assertThat(events).singleElement().isInstanceOf(ItemDropped.class);
                assertThat(run.floor().itemAt(new Position(2, 0))).isPresent();
            }
        }
        assertThat(drops).isBetween(goblin.dropChance(), goblin.dropChance() * 3);
    }

    @Test
    void afterTurn_noDeadEnemy_dropsNothing() {
        Run run = Run.start(1, SPEC, new FixedFloorSource("@.g"));

        assertThat(LOOT.afterTurn(run)).isEmpty();
    }

    @Test
    void afterTurn_aCellWithAnItem_getsNothingMore() {
        for (long seed = 1; seed <= 50; seed++) {
            Run run = Run.start(seed, SPEC, new FixedFloorSource("@.g"));
            run.floor().damage(new EntityId(2), 1000);
            run.floor()
                    .dropItem(
                            new Position(2, 0),
                            new io.github.matteocardarelli.simpleroguelike.dungeon.Item(
                                    new EntityId(99), new StandardItemCatalog().all().get(0)));

            assertThat(LOOT.afterTurn(run)).isEmpty();
        }
    }
}
