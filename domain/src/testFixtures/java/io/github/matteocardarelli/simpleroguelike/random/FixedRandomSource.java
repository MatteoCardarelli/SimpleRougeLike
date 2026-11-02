package io.github.matteocardarelli.simpleroguelike.random;

/**
 * Una sorgente casuale che dà sempre lo stesso numero, per i test che non devono dipendere dal
 * caso: {@link #ALWAYS_HIT} fa andare a segno ogni colpo, {@link #ALWAYS_MISS} lo fa mancare.
 */
public final class FixedRandomSource implements RandomSource {

    /** Tira sempre 0: ogni colpo con almeno la probabilità minima va a segno. */
    public static final FixedRandomSource ALWAYS_HIT = new FixedRandomSource(0);

    /** Tira sempre 99: ogni colpo sotto la probabilità massima del 100% manca. */
    public static final FixedRandomSource ALWAYS_MISS = new FixedRandomSource(99);

    private final int value;

    /**
     * @param value il numero da dare, ridotto a {@code bound - 1} se è troppo grande
     */
    public FixedRandomSource(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Valore negativo: " + value);
        }
        this.value = value;
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Limite non positivo: " + bound);
        }
        return Math.min(value, bound - 1);
    }

    @Override
    public long nextLong() {
        return value;
    }
}
