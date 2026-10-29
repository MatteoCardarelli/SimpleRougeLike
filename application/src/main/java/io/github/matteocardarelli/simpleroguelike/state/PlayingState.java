package io.github.matteocardarelli.simpleroguelike.state;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;

/**
 * Si gioca: accetta ogni comando. Dopo un turno controlla prima la morte del player, poi la
 * vittoria, così se accadono insieme vince la sconfitta.
 */
public record PlayingState() implements GameState {

    @Override
    public boolean accepts(Command command) {
        return true;
    }

    @Override
    public GameState next(FloorView floor) {
        if (!floor.player().isAlive()) {
            return new GameOverState();
        }
        if (floor.enemies().isEmpty()) {
            return new VictoryState();
        }
        return this;
    }

    @Override
    public Phase phase() {
        return Phase.PLAYING;
    }
}
