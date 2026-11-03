package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/**
 * Un nemico è atterrato dopo un salto: la sua cella di riferimento passa da {@code from} a {@code
 * to}. Chi era sotto è stato spinto via e colpito, con eventi separati.
 */
public record Landed(EntityId jumper, Kind kind, Position from, Position to) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(from, to);
    }
}
