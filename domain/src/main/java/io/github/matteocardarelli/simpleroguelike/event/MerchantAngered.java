package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player è uscito dal negozio senza pagare: il mercante diventa un nemico. */
public record MerchantAngered(EntityId merchant, Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of();
    }
}
