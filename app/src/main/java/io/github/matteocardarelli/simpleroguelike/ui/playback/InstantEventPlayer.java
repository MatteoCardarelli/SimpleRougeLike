package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.List;

/**
 * Riproduce gli eventi senza animazioni: avvisa l'ascoltatore di ognuno e finisce subito. È il
 * riproduttore della prima versione; quello animato lo sostituirà senza cambiare chi lo usa.
 */
public final class InstantEventPlayer implements EventPlayer {

    private final EventListener listener;

    public InstantEventPlayer(EventListener listener) {
        this.listener = listener;
    }

    @Override
    public void play(List<GameEvent> events, Scene scene, Runnable onFinished) {
        for (GameEvent event : events) {
            listener.onEventPlayed(event);
        }
        onFinished.run();
    }

    @Override
    public void finish() {
        // Una riproduzione istantanea è già finita quando play restituisce.
    }
}
