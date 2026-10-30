package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;

/**
 * Chi vuole sapere quando un evento viene mostrato: suoni, vibrazione, il registro dei messaggi.
 */
public interface EventListener {

    void onEventPlayed(GameEvent event);
}
