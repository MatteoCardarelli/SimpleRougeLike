package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player ha urtato una porta chiusa a chiave senza la chiave giusta. */
public record DoorLocked(Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
