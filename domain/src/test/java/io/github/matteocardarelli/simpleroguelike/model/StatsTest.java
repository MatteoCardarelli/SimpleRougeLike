package io.github.matteocardarelli.simpleroguelike.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StatsTest {

    @Test
    void stats_plus_addsEveryValue() {
        assertThat(new Stats(100, 10, 2, 10, 5).plus(new Stats(20, 2, 1, 3, 4)))
                .isEqualTo(new Stats(120, 12, 3, 13, 9));
    }

    @Test
    void stats_withHealthAndAttackOnly_hasTheOthersAtZero() {
        assertThat(new Stats(100, 10)).isEqualTo(new Stats(100, 10, 0, 0, 0));
    }

    @Test
    void stats_zero_isAllowedForBonuses() {
        assertThat(new Stats(0, 0).plus(new Stats(0, 0))).isEqualTo(new Stats(0, 0));
    }

    @Test
    void stats_negative_isRejected() {
        assertThatThrownBy(() -> new Stats(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Stats(1, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Stats(1, 1, -1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Stats(1, 1, 0, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Stats(1, 1, 0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
