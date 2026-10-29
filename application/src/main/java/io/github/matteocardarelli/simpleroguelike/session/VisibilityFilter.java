package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * La nebbia di guerra sugli eventi: un evento arriva all'interfaccia solo se è avvenuto in una
 * cella che il player vedeva prima o dopo il turno. Un evento senza celle è globale e passa sempre.
 * Così gli eventi non rivelano niente che lo snapshot nasconde.
 */
public final class VisibilityFilter {

    public List<GameEvent> filter(
            List<GameEvent> events, CellSet visibleBefore, CellSet visibleAfter) {
        CellSet visible = visibleBefore.union(visibleAfter);
        List<GameEvent> kept = new ArrayList<>();
        for (GameEvent event : events) {
            if (event.where().isEmpty() || anyVisible(event.where(), visible)) {
                kept.add(event);
            }
        }
        return List.copyOf(kept);
    }

    private static boolean anyVisible(List<Position> cells, CellSet visible) {
        for (Position cell : cells) {
            if (visible.contains(cell)) {
                return true;
            }
        }
        return false;
    }
}
