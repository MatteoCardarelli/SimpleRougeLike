package io.github.matteocardarelli.simpleroguelike.port;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import org.junit.jupiter.api.Test;

/** Il contratto di {@link FieldOfView}, per l'adapter vero e per i fake. */
public abstract class FieldOfViewContractTest {

    private static final Grid<TileType> HALL =
            FloorFixtures.terrain(
                    "###############",
                    "#.............#",
                    "#.............#",
                    "#.............#",
                    "#.............#",
                    "#.............#",
                    "#.............#",
                    "#......#......#",
                    "#.............#",
                    "#.............#",
                    "#.............#",
                    "#.............#",
                    "#.............#",
                    "#.............#",
                    "###############");

    /** Un'istanza nuova a ogni chiamata. */
    protected abstract FieldOfView createFieldOfView();

    @Test
    void visibleFrom_hasTheTerrainDimensions() {
        CellSet visible = createFieldOfView().visibleFrom(HALL, new Position(3, 3), 4);

        assertThat(visible.width()).isEqualTo(HALL.width());
        assertThat(visible.height()).isEqualTo(HALL.height());
    }

    @Test
    void visibleFrom_includesTheOriginAndItsNeighbours() {
        Position origin = new Position(5, 5);

        CellSet visible = createFieldOfView().visibleFrom(HALL, origin, 3);

        assertThat(visible.contains(origin)).isTrue();
        for (Direction direction : Direction.values()) {
            assertThat(visible.contains(origin.translate(direction))).as("%s", direction).isTrue();
        }
    }

    @Test
    void visibleFrom_includesAdjacentWalls() {
        Position origin = new Position(1, 1);

        CellSet visible = createFieldOfView().visibleFrom(HALL, origin, 3);

        assertThat(visible.contains(new Position(0, 1))).isTrue();
        assertThat(visible.contains(new Position(1, 0))).isTrue();
    }

    @Test
    void visibleFrom_nothingBeyondTheRadius() {
        Position origin = new Position(7, 7);
        int radius = 3;

        CellSet visible = createFieldOfView().visibleFrom(HALL, origin, radius);

        for (Position cell : visible.positions()) {
            assertThat(Math.abs(cell.x() - origin.x())).as("%s", cell).isLessThanOrEqualTo(radius);
            assertThat(Math.abs(cell.y() - origin.y())).as("%s", cell).isLessThanOrEqualTo(radius);
        }
    }

    @Test
    void visibleFrom_dependsOnlyOnItsArguments() {
        Position origin = new Position(4, 9);
        CellSet first = createFieldOfView().visibleFrom(HALL, origin, 5);
        FieldOfView used = createFieldOfView();
        used.visibleFrom(HALL, new Position(10, 2), 2);
        used.visibleFrom(FloorFixtures.terrain("###", "#.#", "###"), new Position(1, 1), 8);

        assertThat(createFieldOfView().visibleFrom(HALL, origin, 5)).isEqualTo(first);
        assertThat(used.visibleFrom(HALL, origin, 5)).isEqualTo(first);
    }
}
