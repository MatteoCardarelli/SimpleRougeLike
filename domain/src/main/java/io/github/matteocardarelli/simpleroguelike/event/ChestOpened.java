package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;
import java.util.Optional;

/**
 * Il player ha aperto uno scrigno: quello che conteneva, se c'era, ora è a terra nella sua cella.
 */
public record ChestOpened(Position at, Optional<Kind> contents) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
