package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player ha aperto una porta chiusa a chiave, consumando una chiave. */
public record DoorUnlocked(Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
