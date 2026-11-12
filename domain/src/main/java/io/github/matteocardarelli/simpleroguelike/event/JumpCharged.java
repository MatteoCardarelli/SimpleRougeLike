package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/**
 * Un nemico grande si carica per saltare, nelle celle {@code cells}: al prossimo turno si alzerà in
 * aria e la sua ombra coprirà il player.
 */
public record JumpCharged(EntityId jumper, Kind kind, List<Position> cells) implements GameEvent {

    public JumpCharged {
        cells = List.copyOf(cells);
    }

    @Override
    public List<Position> where() {
        return cells;
    }
}
