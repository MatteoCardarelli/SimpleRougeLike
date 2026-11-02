package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/**
 * Il player è sceso per le scale al piano {@code depth}. È un evento globale: lo vede sempre, e la
 * scena si azzera sul piano nuovo.
 */
public record FloorChanged(int depth) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of();
    }
}
