package io.github.matteocardarelli.simpleroguelike.rules;

import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_HIT;
import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_MISS;
import static io.github.matteocardarelli.simpleroguelike.rules.Rules.COMBAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Missed;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource;
import org.junit.jupiter.api.Test;

class CombatRulesTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId GOBLIN = new EntityId(2);
    private static final Position PLAYER_AT = new Position(0, 0);
    private static final Position GOBLIN_AT = new Position(1, 0);

    @Test
    void resolveAttack_hit_dealsAttackMinusDefense() {
        Floor floor = FloorFixtures.parse("@g");
        Stats goblin = floor.actor(GOBLIN).orElseThrow().stats();
        int expected = goblin.attack() - FloorFixtures.PLAYER_STATS.defense();

        var events = COMBAT.resolveAttack(floor, GOBLIN, PLAYER, ALWAYS_HIT);

        assertThat(events)
                .startsWith(new Attacked(GOBLIN, PLAYER, GOBLIN_AT, PLAYER_AT))
                .contains(new Damaged(PLAYER, PLAYER_AT, expected, 100 - expected));
    }

    @Test
    void resolveAttack_miss_dealsNoDamage() {
        Floor floor = FloorFixtures.parse("@g");

        var events = COMBAT.resolveAttack(floor, PLAYER, GOBLIN, ALWAYS_MISS);

        assertThat(events)
                .containsExactly(
                        new Attacked(PLAYER, GOBLIN, PLAYER_AT, GOBLIN_AT),
                        new Missed(PLAYER, GOBLIN, PLAYER_AT, GOBLIN_AT));
        assertThat(floor.actor(GOBLIN).orElseThrow().health())
                .isEqualTo(floor.actor(GOBLIN).orElseThrow().stats().maxHealth());
    }

    @Test
    void resolveAttack_hitsBelowTheChance_andMissesFromIt() {
        Stats player = FloorFixtures.PLAYER_STATS;
        Floor first = FloorFixtures.parse("@g");
        int chance = CombatRules.hitChance(player, first.actor(GOBLIN).orElseThrow().stats());

        var below = COMBAT.resolveAttack(first, PLAYER, GOBLIN, new FixedRandomSource(chance - 1));
        var from =
                COMBAT.resolveAttack(
                        FloorFixtures.parse("@g"), PLAYER, GOBLIN, new FixedRandomSource(chance));

        assertThat(below).noneMatch(Missed.class::isInstance);
        assertThat(from).anyMatch(Missed.class::isInstance);
    }

    @Test
    void hitChance_isTheBasePlusAccuracyMinusEvasion() {
        assertThat(CombatRules.hitChance(new Stats(1, 1, 0, 10, 0), new Stats(1, 1, 0, 0, 5)))
                .isEqualTo(CombatRules.BASE_HIT_CHANCE + 10 - 5);
    }

    @Test
    void hitChance_staysBetweenTheLimits() {
        assertThat(CombatRules.hitChance(new Stats(1, 1, 0, 500, 0), new Stats(1, 1)))
                .isEqualTo(CombatRules.MAX_HIT_CHANCE);
        assertThat(CombatRules.hitChance(new Stats(1, 1), new Stats(1, 1, 0, 0, 500)))
                .isEqualTo(CombatRules.MIN_HIT_CHANCE);
    }

    @Test
    void damage_isAttackMinusDefense_atLeastOne() {
        assertThat(CombatRules.damage(new Stats(1, 10), new Stats(1, 1, 3, 0, 0))).isEqualTo(7);
        assertThat(CombatRules.damage(new Stats(1, 2), new Stats(1, 1, 9, 0, 0))).isEqualTo(1);
    }

    @Test
    void resolveAttack_notAdjacent_isRejected() {
        Floor floor = FloorFixtures.parse("@.g");

        assertThatThrownBy(() -> COMBAT.resolveAttack(floor, PLAYER, new EntityId(2), ALWAYS_HIT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolveAttack_deadDefender_isRejected() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(GOBLIN, 1000);

        assertThatThrownBy(() -> COMBAT.resolveAttack(floor, PLAYER, GOBLIN, ALWAYS_HIT))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> COMBAT.resolveAttack(floor, GOBLIN, PLAYER, ALWAYS_HIT))
                .isInstanceOf(IllegalStateException.class);
    }
}
