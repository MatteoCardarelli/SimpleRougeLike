package io.github.matteocardarelli.simpleroguelike.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeightedPickerTest {

    @Test
    void pick_followsTheWeights() {
        List<String> options = List.of("a", "bbb");

        assertThat(WeightedPicker.pick(options, String::length, fixed(0))).isEqualTo("a");
        assertThat(WeightedPicker.pick(options, String::length, fixed(1))).isEqualTo("bbb");
        assertThat(WeightedPicker.pick(options, String::length, fixed(3))).isEqualTo("bbb");
    }

    @Test
    void pick_zeroWeight_isNeverChosen() {
        List<String> options = List.of("", "x", "");
        SeededRandomSource random = new SeededRandomSource(1);

        for (int i = 0; i < 100; i++) {
            assertThat(WeightedPicker.pick(options, String::length, random)).isEqualTo("x");
        }
    }

    @Test
    void pick_allZero_isRejected() {
        assertThatThrownBy(() -> WeightedPicker.pick(List.of("", ""), String::length, fixed(0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WeightedPicker.pick(List.<String>of(), String::length, fixed(0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pick_negativeWeight_isRejected() {
        assertThatThrownBy(() -> WeightedPicker.pick(List.of("a"), s -> -1, fixed(0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pick_asksForANumberBelowTheTotal() {
        int[] bound = new int[1];
        RandomSource spy =
                new RandomSource() {
                    @Override
                    public int nextInt(int b) {
                        bound[0] = b;
                        return 0;
                    }

                    @Override
                    public long nextLong() {
                        return 0;
                    }
                };

        WeightedPicker.pick(List.of("ab", "cde"), String::length, spy);

        assertThat(bound[0]).isEqualTo(5);
    }

    private static RandomSource fixed(int value) {
        return new RandomSource() {
            @Override
            public int nextInt(int bound) {
                return value;
            }

            @Override
            public long nextLong() {
                return value;
            }
        };
    }
}
