package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player è entrato in una trappola in {@code at}, che ha inflitto {@code damage} danni. */
public record TrapTriggered(Kind kind, Position at, int damage) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
