package io.github.matteocardarelli.simpleroguelike.rules;

import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_HIT;
import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_MISS;
import static io.github.matteocardarelli.simpleroguelike.rules.Rules.DAMAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Fired;
import io.github.matteocardarelli.simpleroguelike.event.Missed;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;
import org.junit.jupiter.api.Test;

class RangedRulesTest {

    private static final RangedRules RANGED = new RangedRules(DAMAGE);
    private static final EntityId PLAYER = new EntityId(1);
    private static final Kind KNIFE = StandardItemCatalog.THROWING_KNIFE;

    @Test
    void shoot_stopsOnTheFirstActor_andHits() {
        Floor floor = FloorFixtures.parse("@..gg");
        EntityId first = new EntityId(2);
        int defense = floor.actor(first).orElseThrow().stats().defense();

        var events = RANGED.shoot(floor, PLAYER, Direction.RIGHT, 6, KNIFE, 8, ALWAYS_HIT);

        assertThat(events.get(0))
                .isEqualTo(
                        new Fired(
                                PLAYER,
                                Kind.of("player"),
                                KNIFE,
                                new Position(0, 0),
                                List.of(
                                        new Position(1, 0),
                                        new Position(2, 0),
                                        new Position(3, 0))));
        assertThat(events)
                .contains(new Damaged(first, new Position(3, 0), 8 - defense, 30 - (8 - defense)));
        assertThat(floor.actor(new EntityId(3)).orElseThrow().health()).isEqualTo(30);
    }

    @Test
    void shoot_canMiss() {
        Floor floor = FloorFixtures.parse("@.g");

        var events = RANGED.shoot(floor, PLAYER, Direction.RIGHT, 6, KNIFE, 8, ALWAYS_MISS);

        assertThat(events).hasSize(2);
        assertThat(events.get(1)).isInstanceOf(Missed.class);
    }

    @Test
    void shoot_stopsBeforeAWall_andWithinTheRange() {
        Floor walled = FloorFixtures.parse("@.#g");
        Floor far = FloorFixtures.parse("@....g");

        var blocked = RANGED.shoot(walled, PLAYER, Direction.RIGHT, 6, KNIFE, 8, ALWAYS_HIT);
        var limited = RANGED.shoot(far, PLAYER, Direction.RIGHT, 3, KNIFE, 8, ALWAYS_HIT);

        assertThat(blocked).singleElement().isInstanceOf(Fired.class);
        assertThat(((Fired) blocked.get(0)).path()).containsExactly(new Position(1, 0));
        assertThat(limited).singleElement().isInstanceOf(Fired.class);
        assertThat(((Fired) limited.get(0)).path()).hasSize(3);
    }

    @Test
    void shoot_byAnEnemy_doesNotHurtAnotherEnemy() {
        Floor floor = FloorFixtures.parse("@ga");
        EntityId archer = new EntityId(3);

        var events = RANGED.shoot(floor, archer, Direction.LEFT, 5, KNIFE, 6, ALWAYS_HIT);

        assertThat(events).singleElement().isInstanceOf(Fired.class);
        assertThat(floor.actor(new EntityId(2)).orElseThrow().health()).isEqualTo(30);
    }

    @Test
    void shoot_damageIsAtLeastOne() {
        Floor floor = FloorFixtures.parse("@g");

        var events = RANGED.shoot(floor, PLAYER, Direction.RIGHT, 1, KNIFE, 1, ALWAYS_HIT);

        assertThat(events).anyMatch(e -> e instanceof Damaged d && d.amount() == 1);
    }

    @Test
    void shoot_invalidValues_areRejected() {
        Floor floor = FloorFixtures.parse("@g");

        assertThatThrownBy(
                        () -> RANGED.shoot(floor, PLAYER, Direction.RIGHT, 0, KNIFE, 8, ALWAYS_HIT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> RANGED.shoot(floor, PLAYER, Direction.RIGHT, 3, KNIFE, 0, ALWAYS_HIT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
