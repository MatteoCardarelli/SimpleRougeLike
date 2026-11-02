package io.github.matteocardarelli.simpleroguelike.state;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;

/** Nessun nemico vivo: nessun comando è accettato. Si esce solo con una partita nuova. */
public record VictoryState() implements GameState {

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
        return Phase.VICTORY;
    }
}
