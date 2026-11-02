package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Un attacco è andato a vuoto: segue il suo {@link Attacked}, al posto del danno. */
public record Missed(EntityId attacker, EntityId target, Position attackerAt, Position targetAt)
        implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(attackerAt, targetAt);
    }
}
