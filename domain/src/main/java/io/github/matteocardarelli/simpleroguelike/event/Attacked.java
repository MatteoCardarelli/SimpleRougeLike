package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Un attore ne ha attaccato un altro. Il danno, se c'è, è un {@link Damaged} separato. */
public record Attacked(EntityId attacker, EntityId target, Position attackerAt, Position targetAt)
        implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(attackerAt, targetAt);
    }
}
