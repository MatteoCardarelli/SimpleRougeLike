package io.github.matteocardarelli.simpleroguelike.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FloorSpecTest {

    @Test
    void constructor_validValues_areAccepted() {
        new FloorSpec(3, 3, 0, 0, 0, 1);
    }

    @Test
    void constructor_itemRange_isKept() {
        FloorSpec spec = new FloorSpec(3, 3, 0, 5, 7, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1);

        assertThat(spec.itemCountMin()).isEqualTo(5);
        assertThat(spec.itemCountMax()).isEqualTo(7);
    }

    @Test
    void constructor_minAboveMax_isRejected() {
        assertThatThrownBy(() -> new FloorSpec(3, 3, 0, 7, 5, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "2, 3, 0, 0, 0, 1",
        "3, 2, 0, 0, 0, 1",
        "3, 3, -1, 0, 0, 1",
        "3, 3, 0, -1, 0, 1",
        "3, 3, 0, 0, -1, 1",
        "3, 3, 0, 0, 0, 0"
    })
    void constructor_invalidValues_areRejected(
            int width, int height, int enemies, int items, int distance, int attempts) {
        assertThatThrownBy(() -> new FloorSpec(width, height, enemies, items, distance, attempts))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
