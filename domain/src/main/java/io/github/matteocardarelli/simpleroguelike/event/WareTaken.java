package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player ha preso una merce del negozio: la deve pagare al mercante prima di uscire. */
public record WareTaken(EntityId item, Kind kind, int price, Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
