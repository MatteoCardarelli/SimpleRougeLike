package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.Collection;
import java.util.List;

/** Avvisa tutti gli ascoltatori, nell'ordine in cui li riceve. */
public final class CompositeEventListener implements EventListener {

    private final List<EventListener> listeners;

    public CompositeEventListener(Collection<EventListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    @Override
    public void onEventPlayed(GameEvent event) {
        for (EventListener listener : listeners) {
            listener.onEventPlayed(event);
        }
    }
}
