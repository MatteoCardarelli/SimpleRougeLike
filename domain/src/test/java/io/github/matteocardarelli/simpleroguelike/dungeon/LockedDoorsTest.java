package io.github.matteocardarelli.simpleroguelike.dungeon;

import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_HIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.event.DoorLocked;
import io.github.matteocardarelli.simpleroguelike.event.DoorUnlocked;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import org.junit.jupiter.api.Test;

class LockedDoorsTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final ActionRules ACTIONS =
            new ActionRules(
                    new CombatRules(new DamageRules(new LevelingRules(new Stats(20, 2), 150))));

    @Test
    void lockedDoor_withoutAKey_staysClosed_andSaysSo() {
        Floor floor = FloorFixtures.parse("@K.");

        var result = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.events()).containsExactly(new DoorLocked(new Position(1, 0)));
        assertThat(floor.isWalkable(new Position(1, 0))).isFalse();
    }

    @Test
    void lockedDoor_withAKey_opens_andSpendsTheKey() {
        Floor floor = FloorFixtures.parse("@kK.");
        ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);
        assertThat(floor.player().inventory().items()).hasSize(1);

        var result = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.events()).containsExactly(new DoorUnlocked(new Position(2, 0)));
        assertThat(floor.isWalkable(new Position(2, 0))).isTrue();
        assertThat(floor.player().inventory().items()).isEmpty();
    }

    @Test
    void keys_stack_andDoNotOpenTheBossDoor() {
        Floor floor = FloorFixtures.parse("@kk");
        floor.move(PLAYER, Direction.RIGHT);
        floor.pickUp(PLAYER);
        floor.move(PLAYER, Direction.RIGHT);
        floor.pickUp(PLAYER);

        assertThat(floor.player().inventory().items())
                .singleElement()
                .satisfies(
                        keys -> {
                            assertThat(keys.kind()).isEqualTo(StandardItemCatalog.KEY);
                            assertThat(keys.count()).isEqualTo(2);
                        });
    }

    @Test
    void dropItem_onlyOnAFreeFloorCell() {
        Floor floor = FloorFixtures.parse("@.!+");
        Item key = new Item(new EntityId(9), FloorFixtures.item(StandardItemCatalog.KEY));

        assertThat(floor.dropItem(new Position(2, 0), key)).isFalse();
        assertThat(floor.dropItem(new Position(3, 0), key)).isFalse();
        assertThat(floor.dropItem(new Position(1, 0), key)).isTrue();
        assertThat(floor.itemAt(new Position(1, 0))).contains(key);
    }

    @Test
    void fallen_areTheDeadNotYetRemoved() {
        Floor floor = FloorFixtures.parse("@gg");
        floor.damage(new EntityId(3), 1000);

        assertThat(floor.fallen()).extracting(Enemy::id).containsExactly(new EntityId(3));
        floor.removeDead();
        assertThat(floor.fallen()).isEmpty();
    }
}
