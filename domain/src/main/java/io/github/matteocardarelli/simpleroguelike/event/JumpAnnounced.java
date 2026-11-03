package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/**
 * Un nemico grande si prepara a saltare: al prossimo turno atterrerà sulle celle {@code cells}, e
 * chi resta lì sotto verrà schiacciato.
 */
public record JumpAnnounced(EntityId jumper, List<Position> cells) implements GameEvent {

    public JumpAnnounced {
        cells = List.copyOf(cells);
    }

    @Override
    public List<Position> where() {
        return cells;
    }
}
