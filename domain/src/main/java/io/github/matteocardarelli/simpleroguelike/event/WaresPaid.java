package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player ha pagato al mercante tutto quello che aveva preso. */
public record WaresPaid(int amount, Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
