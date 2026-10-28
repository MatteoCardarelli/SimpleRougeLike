package io.github.matteocardarelli.simpleroguelike.random;

/**
 * Una sorgente di numeri casuali. Il gioco non ne crea mai una da solo: arriva sempre come
 * argomento, così la stessa partita si può rigiocare dallo stesso seed.
 */
public interface RandomSource {

    /**
     * Restituisce un intero tra 0 compreso e {@code bound} escluso.
     *
     * @throws IllegalArgumentException se {@code bound} non è positivo
     */
    int nextInt(int bound);

    long nextLong();
}
