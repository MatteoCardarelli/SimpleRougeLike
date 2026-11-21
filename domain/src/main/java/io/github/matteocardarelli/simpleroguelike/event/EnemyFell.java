package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Un nemico è caduto dal soffitto ed è comparso sul piano nella cella {@code at}. */
public record EnemyFell(EntityId enemy, Kind kind, Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
