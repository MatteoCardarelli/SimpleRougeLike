package io.github.matteocardarelli.simpleroguelike.ui.render;

/**
 * Il lato di una cella in pixel: un multiplo intero di 64, la misura degli sprite, così i pixel
 * dell'illustrazione restano uguali tra loro invece di essere stirati di un fattore frazionario.
 */
public final class CellSize {

    /** Il lato, in pixel, degli sprite. */
    public static final int ART_PIXELS = 64;

    private CellSize() {}

    /**
     * Il multiplo di {@link #ART_PIXELS} più vicino alla misura desiderata, almeno uno. A metà
     * strada vince il più piccolo, che lascia vedere più mappa.
     */
    public static float pixelPerfect(float preferredPx) {
        double ratio = preferredPx / ART_PIXELS;
        long multiple = Math.max(1, (long) Math.ceil(ratio - 0.5));
        return multiple * ART_PIXELS;
    }
}
