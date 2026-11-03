package io.github.matteocardarelli.simpleroguelike.random;

/**
 * Una sorgente casuale deterministica: lo stesso seed dà la stessa sequenza in qualunque processo,
 * sulla JVM come su Android. È l'algoritmo SplitMix64 di {@code java.util.SplittableRandom},
 * riscritto qui con gli stessi numeri perché lo stato, due {@code long}, si possa leggere e
 * ripristinare: una partita salvata riprende con la stessa sequenza. Non è thread-safe.
 */
public final class SeededRandomSource implements RandomSource {

    private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;

    private long seed;
    private final long gamma;

    public SeededRandomSource(long seed) {
        this(seed, GOLDEN_GAMMA);
    }

    private SeededRandomSource(long seed, long gamma) {
        this.seed = seed;
        this.gamma = gamma;
    }

    /** Riprende una sorgente dallo stato letto con {@link #state()}. */
    public static SeededRandomSource restore(State state) {
        if ((state.gamma() & 1L) == 0) {
            throw new IllegalArgumentException("Gamma non valido: " + state.gamma());
        }
        return new SeededRandomSource(state.seed(), state.gamma());
    }

    /** Lo stato della sorgente, da salvare. */
    public State state() {
        return new State(seed, gamma);
    }

    /** Lo stato di una sorgente: il seed corrente e il suo passo, sempre dispari. */
    public record State(long seed, long gamma) {}

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Limite non positivo: " + bound);
        }
        int r = mix32(nextSeed());
        int m = bound - 1;
        if ((bound & m) == 0) {
            return r & m;
        }
        for (int u = r >>> 1; u + m - (r = u % bound) < 0; u = mix32(nextSeed()) >>> 1) {
            // Scarta i valori che renderebbero la distribuzione non uniforme.
        }
        return r;
    }

    @Override
    public long nextLong() {
        return mix64(nextSeed());
    }

    /**
     * Crea una sorgente indipendente, in modo deterministico. Due flussi ricavati così dallo stesso
     * seed non si influenzano: usare di più uno non cambia i numeri dell'altro.
     */
    public SeededRandomSource split() {
        return new SeededRandomSource(nextLong(), mixGamma(nextSeed()));
    }

    private long nextSeed() {
        seed += gamma;
        return seed;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static int mix32(long z) {
        z = (z ^ (z >>> 33)) * 0x62a9d9ed799705f5L;
        return (int) (((z ^ (z >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
    }

    private static long mixGamma(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        z = (z ^ (z >>> 33)) | 1L;
        int n = Long.bitCount(z ^ (z >>> 1));
        return (n < 24) ? z ^ 0xaaaaaaaaaaaaaaaaL : z;
    }
}
