package io.github.matteocardarelli.simpleroguelike.ui.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CellSizeTest {

    @ParameterizedTest
    @CsvSource({"84, 64", "96, 64", "105, 128", "128, 128", "40, 64", "160, 128", "161, 192"})
    void pixelPerfect_isTheNearestMultipleOf64_withTiesGoingDown(float preferred, float expected) {
        assertThat(CellSize.pixelPerfect(preferred)).isEqualTo(expected);
    }
}
