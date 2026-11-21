package io.github.matteocardarelli.simpleroguelike.ui.input;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import java.util.Set;

/**
 * Di un turno mostrato, quello che serve a {@link HoldToWalk} per decidere se continuare a
 * camminare.
 *
 * @param revision la revisione del risultato: serve a non valutare due volte lo stesso turno
 * @param playerProgressed il player si è mosso, ha attaccato o ha aperto una porta
 * @param playerDamaged il player ha subito danno
 * @param visibleEnemies gli id dei nemici visibili dopo il turno
 */
public record TurnSummary(
        long revision,
        boolean playerProgressed,
        boolean playerDamaged,
        Set<EntityId> visibleEnemies) {

    public TurnSummary {
        visibleEnemies = Set.copyOf(visibleEnemies);
    }
}
