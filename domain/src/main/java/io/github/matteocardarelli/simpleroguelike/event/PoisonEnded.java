package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il veleno di un attore è finito da solo. */
public record PoisonEnded(EntityId target, Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
