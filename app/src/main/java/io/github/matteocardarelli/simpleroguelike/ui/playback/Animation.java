package io.github.matteocardarelli.simpleroguelike.ui.playback;

/** Un cambiamento della scena che dura un po': un passo, un colpo, un proiettile in volo. */
public interface Animation {

    /** Quanto dura, in millisecondi. */
    long durationMs();

    /**
     * Porta la scena al punto {@code fraction} dell'animazione: 0 all'inizio, 1 alla fine. Con 1 la
     * scena è com'è a animazione finita.
     */
    void apply(float fraction);
}
