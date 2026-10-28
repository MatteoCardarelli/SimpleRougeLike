package io.github.matteocardarelli.simpleroguelike.rules;

import static io.github.matteocardarelli.simpleroguelike.rules.Rules.LEVELING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.event.ExperienceGained;
import io.github.matteocardarelli.simpleroguelike.event.LeveledUp;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import org.junit.jupiter.api.Test;

class LevelingRulesTest {

    @Test
    void nextThreshold_growsByPercent_roundingDown() {
        assertThat(LEVELING.nextThreshold(100)).isEqualTo(150);
        assertThat(LEVELING.nextThreshold(151)).isEqualTo(226);
    }

    @Test
    void nextThreshold_alwaysGrowsByAtLeastOne() {
        assertThat(new LevelingRules(0, 0, 100).nextThreshold(5)).isEqualTo(6);
        assertThat(LEVELING.nextThreshold(1)).isEqualTo(2);
    }

    @Test
    void nextThreshold_overflow_throws() {
        assertThatThrownBy(() -> LEVELING.nextThreshold(Integer.MAX_VALUE))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void constructor_shrinkingThreshold_isRejected() {
        assertThatThrownBy(() -> new LevelingRules(1, 1, 99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bonusPerLevel_isTheConfiguredStats() {
        assertThat(LEVELING.bonusPerLevel()).isEqualTo(new Stats(20, 2));
    }

    @Test
    void grantExperience_twoLevels_emitsOneLeveledUpEach() {
        Floor floor = FloorFixtures.parse("@");

        var events = LEVELING.grantExperience(floor, 250);

        Position at = new Position(0, 0);
        assertThat(events)
                .containsExactly(
                        new ExperienceGained(at, 250), new LeveledUp(at, 2), new LeveledUp(at, 3));
        assertThat(floor.player().level()).isEqualTo(3);
        assertThat(floor.player().experience()).isZero();
        assertThat(floor.player().experienceToNextLevel()).isEqualTo(225);
        assertThat(floor.player().stats()).isEqualTo(new Stats(140, 14));
    }

    @Test
    void grantExperience_zero_emitsNothing() {
        Floor floor = FloorFixtures.parse("@");

        assertThat(LEVELING.grantExperience(floor, 0)).isEmpty();
    }

    @Test
    void grantExperience_deadPlayer_emitsNothing() {
        Floor floor = FloorFixtures.parse("@");
        floor.damage(new EntityId(1), 1000);

        assertThat(LEVELING.grantExperience(floor, 500)).isEmpty();
        assertThat(floor.player().experience()).isZero();
    }
}
