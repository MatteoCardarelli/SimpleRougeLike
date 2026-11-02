package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;

/** Il player ha tirato una leva, che ha aperto i cancelli nelle celle {@code opened}. */
public record LeverPulled(Position at, List<Position> opened) implements GameEvent {

    public LeverPulled {
        opened = List.copyOf(opened);
    }

    @Override
    public List<Position> where() {
        List<Position> cells = new ArrayList<>(opened.size() + 1);
        cells.add(at);
        cells.addAll(opened);
        return List.copyOf(cells);
    }
}
