package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player ha guadagnato esperienza. */
public record ExperienceGained(Position at, int amount) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
