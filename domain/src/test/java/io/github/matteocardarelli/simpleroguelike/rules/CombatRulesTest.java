package io.github.matteocardarelli.simpleroguelike.rules;

import static io.github.matteocardarelli.simpleroguelike.rules.Rules.COMBAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import org.junit.jupiter.api.Test;

class CombatRulesTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId GOBLIN = new EntityId(2);

    @Test
    void resolveAttack_usesAttackerAttack() {
        Floor floor = FloorFixtures.parse("@g");

        var events = COMBAT.resolveAttack(floor, GOBLIN, PLAYER);

        assertThat(events).contains(new Damaged(PLAYER, new Position(0, 0), 5, 95));
    }

    @Test
    void resolveAttack_notAdjacent_isRejected() {
        Floor floor = FloorFixtures.parse("@.g");

        assertThatThrownBy(() -> COMBAT.resolveAttack(floor, PLAYER, new EntityId(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolveAttack_deadDefender_isRejected() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(GOBLIN, 1000);

        assertThatThrownBy(() -> COMBAT.resolveAttack(floor, PLAYER, GOBLIN))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> COMBAT.resolveAttack(floor, GOBLIN, PLAYER))
                .isInstanceOf(IllegalStateException.class);
    }
}
