package io.github.matteocardarelli.simpleroguelike.generation;

import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.List;
import java.util.function.ToIntFunction;

/** Sceglie un elemento a caso, con una probabilità proporzionale al suo peso. */
public final class WeightedPicker {

    private WeightedPicker() {}

    /**
     * Sceglie un elemento. Un elemento di peso 0 non viene mai scelto.
     *
     * @throws IllegalArgumentException se i pesi sono negativi o tutti zero
     */
    public static <T> T pick(
            List<T> options, ToIntFunction<? super T> weight, RandomSource random) {
        long total = 0;
        for (T option : options) {
            int value = weight.applyAsInt(option);
            if (value < 0) {
                throw new IllegalArgumentException("Peso negativo: " + value);
            }
            total += value;
        }
        if (total == 0) {
            throw new IllegalArgumentException("Nessun elemento con peso positivo");
        }
        int roll = random.nextInt(Math.toIntExact(total));
        for (T option : options) {
            roll -= weight.applyAsInt(option);
            if (roll < 0) {
                return option;
            }
        }
        throw new AssertionError("La somma dei pesi è cambiata durante la scelta");
    }
}
