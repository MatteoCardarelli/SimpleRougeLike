package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.List;

/**
 * Mostra gli eventi di un turno sulla scena, nell'ordine in cui sono accaduti. Quanto ci mette è
 * affar suo: la logica del turno è già finita.
 */
public interface EventPlayer {

    /**
     * Riproduce gli eventi e poi chiama {@code onFinished}, una volta sola, anche se la
     * riproduzione viene interrotta da {@link #finish()}.
     */
    void play(List<GameEvent> events, Scene scene, Runnable onFinished);

    /** Porta subito alla fine la riproduzione in corso; senza riproduzione non fa niente. */
    void finish();
}
