package io.github.matteocardarelli.simpleroguelike.state;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;

/**
 * Il player è morto: nessun comando è accettato, e la morte è definitiva. Si esce solo con una
 * partita nuova.
 */
public record GameOverState() implements GameState {

    @Override
    public boolean accepts(Command command) {
        return false;
    }

    @Override
    public GameState next(FloorView floor, boolean lastFloor) {
        return this;
    }

    @Override
    public Phase phase() {
        return Phase.GAME_OVER;
    }
}
