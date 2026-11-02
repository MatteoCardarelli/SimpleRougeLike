package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player ha equipaggiato un oggetto dell'inventario: le sue statistiche cambiano. */
public record ItemEquipped(EntityId item, Kind kind, Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
