package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Un attore ha perso salute, per qualunque causa. */
public record Damaged(EntityId target, Position at, int amount, int remainingHealth)
        implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
