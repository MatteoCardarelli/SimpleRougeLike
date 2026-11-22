package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Un attore è stato avvelenato; {@code turns} sono i turni di veleno, ricominciati da capo. */
public record Poisoned(EntityId target, Position at, int turns) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
