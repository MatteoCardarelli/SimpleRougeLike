package io.github.matteocardarelli.simpleroguelike.random;

import java.util.SplittableRandom;

/**
 * Una sorgente casuale deterministica: lo stesso seed dà la stessa sequenza in qualunque processo,
 * sulla JVM come su Android. L'algoritmo di {@link SplittableRandom} è fissato dalla sua specifica.
 */
public final class SeededRandomSource implements RandomSource {

    private final SplittableRandom random;

    public SeededRandomSource(long seed) {
        this(new SplittableRandom(seed));
    }

    private SeededRandomSource(SplittableRandom random) {
        this.random = random;
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    @Override
    public long nextLong() {
        return random.nextLong();
    }

    /**
     * Crea una sorgente indipendente, in modo deterministico. Due flussi ricavati così dallo stesso
     * seed non si influenzano: usare di più uno non cambia i numeri dell'altro.
     */
    public SeededRandomSource split() {
        return new SeededRandomSource(random.split());
    }
}
