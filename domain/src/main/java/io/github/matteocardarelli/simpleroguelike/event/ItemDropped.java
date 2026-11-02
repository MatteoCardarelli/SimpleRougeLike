package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Un nemico morto ha lasciato cadere un oggetto nella sua cella. */
public record ItemDropped(EntityId item, Kind kind, Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
