package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;

/**
 * Un nemico vivo visibile in almeno una delle sue celle.
 *
 * @param position la cella di riferimento: per un nemico grande è l'angolo in alto a sinistra del
 *     quadrato che lo contiene
 * @param size il lato di quel quadrato, in celle: 1 per un nemico normale
 * @param boss vero per il boss, di cui l'interfaccia mostra la salute
 */
public record ActorView(
        EntityId id,
        Kind kind,
        Position position,
        int health,
        int maxHealth,
        int size,
        boolean boss) {

    /** Un nemico normale, di una cella. */
    public ActorView(EntityId id, Kind kind, Position position, int health, int maxHealth) {
        this(id, kind, position, health, maxHealth, 1, false);
    }
}
