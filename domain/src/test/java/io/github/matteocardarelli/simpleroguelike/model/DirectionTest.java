package io.github.matteocardarelli.simpleroguelike.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DirectionTest {

    @Test
    void direction_offsets_areUnitSteps() {
        for (Direction direction : Direction.values()) {
            assertThat(Math.abs(direction.dx()) + Math.abs(direction.dy())).isEqualTo(1);
        }
    }
}
