package io.github.matteocardarelli.simpleroguelike.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StatsTest {

    @Test
    void stats_plus_addsBothValues() {
        assertThat(new Stats(100, 10).plus(new Stats(20, 2))).isEqualTo(new Stats(120, 12));
    }

    @Test
    void stats_zero_isAllowedForBonuses() {
        assertThat(new Stats(0, 0).plus(new Stats(0, 0))).isEqualTo(new Stats(0, 0));
    }

    @Test
    void stats_negative_isRejected() {
        assertThatThrownBy(() -> new Stats(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Stats(1, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
