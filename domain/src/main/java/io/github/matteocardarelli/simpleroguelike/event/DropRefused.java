package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player non ha potuto lasciare a terra un oggetto: non è cambiato niente. */
public record DropRefused(EntityId item, Kind kind, Position at, Reason reason)
        implements GameEvent {

    /** Perché l'oggetto è rimasto nell'inventario. */
    public enum Reason {
        /** La cella ha un oggetto fisso, per esempio una porta aperta o le scale. */
        FIXTURE,
        /** È merce del negozio non pagata, e la cella è fuori dal negozio. */
        UNPAID
    }

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
