package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.ui.input.PlayerInput;
import org.jspecify.annotations.Nullable;

/**
 * Blocca le azioni del giocatore mentre un turno viene riprodotto: tiene in coda solo l'ultima, e
 * la inoltra quando la riproduzione finisce. Così il gioco risponde subito senza accumulare turni,
 * e nessuna azione aggira il blocco.
 */
public final class GatedPlayerInput implements PlayerInput {

    private final PlayerInput target;
    private final TurnPlayback playback;
    private @Nullable Runnable pending;

    public GatedPlayerInput(PlayerInput target, TurnPlayback playback) {
        this.target = target;
        this.playback = playback;
    }

    @Override
    public void submit(Command command) {
        forward(() -> target.submit(command));
    }

    @Override
    public void swipe(Direction direction) {
        forward(() -> target.swipe(direction));
    }

    @Override
    public void toggleInventory() {
        forward(target::toggleInventory);
    }

    @Override
    public void chooseItem(EntityId item) {
        forward(() -> target.chooseItem(item));
    }

    /** Inoltra l'azione in coda, se c'è. */
    public void flush() {
        Runnable action = pending;
        pending = null;
        if (action != null) {
            action.run();
        }
    }

    private void forward(Runnable action) {
        if (playback.isBusy()) {
            pending = action;
        } else {
            action.run();
        }
    }
}
