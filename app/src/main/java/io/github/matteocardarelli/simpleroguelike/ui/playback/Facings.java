package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.Optional;

/** Verso dove deve guardare chi si sposta o attacca da una cella a un'altra. */
final class Facings {

    private Facings() {}

    /**
     * La direzione da {@code from} verso {@code to}: vince l'asse con la distanza maggiore, in
     * parità quello orizzontale. Vuota se le celle coincidono.
     */
    static Optional<Direction> toward(Position from, Position to) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        if (dx == 0 && dy == 0) {
            return Optional.empty();
        }
        if (Math.abs(dx) >= Math.abs(dy)) {
            return Optional.of(dx > 0 ? Direction.RIGHT : Direction.LEFT);
        }
        return Optional.of(dy > 0 ? Direction.DOWN : Direction.UP);
    }
}
