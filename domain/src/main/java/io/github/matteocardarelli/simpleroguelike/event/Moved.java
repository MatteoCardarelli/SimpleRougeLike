package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Un attore si è spostato di una cella. */
public record Moved(EntityId actor, Kind kind, Position from, Position to) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(from, to);
    }
}
