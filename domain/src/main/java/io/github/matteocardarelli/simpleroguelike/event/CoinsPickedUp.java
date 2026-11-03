package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player ha raccolto un mucchio di monete. */
public record CoinsPickedUp(int amount, Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
