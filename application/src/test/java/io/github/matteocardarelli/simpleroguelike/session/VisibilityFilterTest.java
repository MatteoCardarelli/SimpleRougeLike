package io.github.matteocardarelli.simpleroguelike.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisibilityFilterTest {

    private static final VisibilityFilter FILTER = new VisibilityFilter();
    private static final EntityId ID = new EntityId(3);
    private static final Kind GOBLIN = Kind.of("goblin");

    @Test
    void filter_keepsEventsInCellsSeenBeforeOrAfter() {
        GameEvent seenBefore = new Damaged(ID, new Position(0, 0), 1, 9);
        GameEvent seenAfter = new Damaged(ID, new Position(2, 0), 1, 8);
        GameEvent dark = new Damaged(ID, new Position(1, 0), 1, 7);

        List<GameEvent> kept =
                FILTER.filter(
                        List.of(seenBefore, dark, seenAfter),
                        cells(new Position(0, 0)),
                        cells(new Position(2, 0)));

        assertThat(kept).containsExactly(seenBefore, seenAfter);
    }

    @Test
    void filter_movementIntoTheLight_isKept() {
        GameEvent entering = new Moved(ID, GOBLIN, new Position(1, 0), new Position(2, 0));

        assertThat(FILTER.filter(List.of(entering), cells(), cells(new Position(2, 0))))
                .containsExactly(entering);
    }

    @Test
    void filter_globalEvent_isAlwaysKept() {
        GameEvent global = List::of;

        assertThat(FILTER.filter(List.of(global), cells(), cells())).containsExactly(global);
    }

    private static CellSet cells(Position... visible) {
        return CellSet.of(3, 1, List.of(visible));
    }
}
