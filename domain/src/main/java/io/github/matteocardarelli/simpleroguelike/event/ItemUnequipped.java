package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player si è tolto un oggetto, che resta nell'inventario. */
public record ItemUnequipped(EntityId item, Kind kind, Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
