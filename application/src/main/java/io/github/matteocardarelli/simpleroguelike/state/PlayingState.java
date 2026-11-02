package io.github.matteocardarelli.simpleroguelike.state;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;

/**
 * Si gioca: accetta ogni comando. Dopo un turno controlla prima la morte del player, poi la
 * vittoria, così se accadono insieme vince la sconfitta. Si vince solo sull'ultimo piano, quando
 * non resta nessun nemico vivo.
 */
public record PlayingState() implements GameState {

    @Override
    public boolean accepts(Command command) {
        return true;
    }

    @Override
    public GameState next(FloorView floor, boolean lastFloor) {
        if (!floor.player().isAlive()) {
            return new GameOverState();
        }
        if (lastFloor && floor.enemies().isEmpty()) {
            return new VictoryState();
        }
        return this;
    }

    @Override
    public Phase phase() {
        return Phase.PLAYING;
    }
}
