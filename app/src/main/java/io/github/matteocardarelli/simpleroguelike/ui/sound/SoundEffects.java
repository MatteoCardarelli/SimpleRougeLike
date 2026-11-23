package io.github.matteocardarelli.simpleroguelike.ui.sound;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.ui.playback.EventListener;
import org.jspecify.annotations.Nullable;

/**
 * Suona l'effetto di ogni evento mostrato, se i suoni sono accesi. Sapere chi è il giocatore serve
 * a distinguere i suoi passi e i suoi colpi da quelli dei nemici: lo comunica l'Activity a ogni
 * schermata.
 */
public final class SoundEffects implements EventListener {

    private final SoundMap map;
    private final SoundOutput output;
    private boolean enabled = true;
    private @Nullable EntityId player;

    public SoundEffects(SoundMap map, SoundOutput output) {
        this.map = map;
        this.output = output;
    }

    /** Accende o spegne i suoni: spenti, non suona niente. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Chi è il giocatore della partita in corso. */
    public void setPlayer(EntityId player) {
        this.player = player;
    }

    /** Suona un effetto che non nasce da un evento, come la vittoria. */
    public void play(Sound sound) {
        if (enabled) {
            output.play(sound);
        }
    }

    @Override
    public void onEventPlayed(GameEvent event) {
        EntityId current = player;
        if (!enabled || current == null) {
            return;
        }
        map.soundFor(event, current).ifPresent(output::play);
    }
}
