package io.github.matteocardarelli.simpleroguelike.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeededRandomSourceTest {

    @Test
    void sameSeed_givesSameSequence() {
        assertThat(sample(new SeededRandomSource(42)))
                .isEqualTo(sample(new SeededRandomSource(42)));
    }

    @Test
    void differentSeeds_giveDifferentSequences() {
        assertThat(sample(new SeededRandomSource(1)))
                .isNotEqualTo(sample(new SeededRandomSource(2)));
    }

    @Test
    void nextInt_staysWithinBound() {
        SeededRandomSource random = new SeededRandomSource(7);

        for (int i = 0; i < 1000; i++) {
            assertThat(random.nextInt(3)).isBetween(0, 2);
        }
    }

    @Test
    void nextInt_nonPositiveBound_isRejected() {
        assertThatThrownBy(() -> new SeededRandomSource(7).nextInt(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void split_isDeterministic() {
        SeededRandomSource first = new SeededRandomSource(42);
        SeededRandomSource second = new SeededRandomSource(42);

        assertThat(sample(first.split())).isEqualTo(sample(second.split()));
    }

    @Test
    void split_streamsAreIndependent() {
        SeededRandomSource parentA = new SeededRandomSource(42);
        SeededRandomSource generationA = parentA.split();
        SeededRandomSource gameplayA = parentA.split();
        SeededRandomSource parentB = new SeededRandomSource(42);
        SeededRandomSource generationB = parentB.split();
        SeededRandomSource gameplayB = parentB.split();

        sample(gameplayA);
        sample(gameplayA);

        assertThat(sample(generationA)).isEqualTo(sample(generationB));
        assertThat(sample(gameplayB)).isNotEqualTo(sample(generationB));
    }

    private static List<Long> sample(RandomSource random) {
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            values.add(random.nextLong());
            values.add((long) random.nextInt(1000));
        }
        return values;
    }
}
