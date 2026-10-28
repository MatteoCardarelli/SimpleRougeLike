package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player è salito di livello. */
public record LeveledUp(Position at, int newLevel) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
