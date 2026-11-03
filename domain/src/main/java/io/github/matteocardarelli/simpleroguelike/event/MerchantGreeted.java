package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player ha salutato il mercante senza niente da pagare. */
public record MerchantGreeted(Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
