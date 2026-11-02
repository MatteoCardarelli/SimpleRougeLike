package io.github.matteocardarelli.simpleroguelike.state;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;

/**
 * La fase della partita, con il suo comportamento: quali comandi accetta e in quale fase si passa
 * dopo un turno. Chi la usa chiama solo i suoi metodi, mai {@code instanceof}.
 */
public sealed interface GameState permits PlayingState, GameOverState, VictoryState {

    boolean accepts(Command command);

    /**
     * La fase dopo un turno, guardando il piano com'è adesso.
     *
     * @param lastFloor vero sull'ultimo piano della partita, l'unico dove si può vincere
     */
    GameState next(FloorView floor, boolean lastFloor);

    Phase phase();
}
