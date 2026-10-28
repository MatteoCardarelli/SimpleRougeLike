package io.github.matteocardarelli.simpleroguelike.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GridTest {

    @Test
    void get_readsRowMajorCells() {
        Grid<String> grid = new Grid<>(3, 2, List.of("a", "b", "c", "d", "e", "f"));

        assertThat(grid.get(new Position(0, 0))).isEqualTo("a");
        assertThat(grid.get(new Position(2, 0))).isEqualTo("c");
        assertThat(grid.get(new Position(0, 1))).isEqualTo("d");
        assertThat(grid.get(new Position(2, 1))).isEqualTo("f");
    }

    @Test
    void get_outside_throws() {
        Grid<String> grid = Grid.of(2, 2, p -> ".");

        assertThatThrownBy(() -> grid.get(new Position(2, 0)))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> grid.get(new Position(0, -1)))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void isInside_checksAllFourEdges() {
        Grid<String> grid = Grid.of(3, 2, p -> ".");

        assertThat(grid.isInside(new Position(0, 0))).isTrue();
        assertThat(grid.isInside(new Position(2, 1))).isTrue();
        assertThat(grid.isInside(new Position(-1, 0))).isFalse();
        assertThat(grid.isInside(new Position(3, 0))).isFalse();
        assertThat(grid.isInside(new Position(0, -1))).isFalse();
        assertThat(grid.isInside(new Position(0, 2))).isFalse();
    }

    @Test
    void of_asksEveryPosition_inRowOrder() {
        Grid<Position> grid = Grid.of(2, 2, p -> p);

        assertThat(grid.cells())
                .containsExactly(
                        new Position(0, 0),
                        new Position(1, 0),
                        new Position(0, 1),
                        new Position(1, 1));
        assertThat(grid.positions()).isEqualTo(grid.cells());
    }

    @Test
    void map_keepsDimensionsAndOrder() {
        Grid<Integer> grid = new Grid<>(2, 1, List.of(1, 2));

        Grid<String> mapped = grid.map(n -> "n" + n);

        assertThat(mapped).isEqualTo(new Grid<>(2, 1, List.of("n1", "n2")));
    }

    @Test
    void constructor_wrongCellCount_isRejected() {
        assertThatThrownBy(() -> new Grid<>(2, 2, List.of("a")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_emptyDimensions_areRejected() {
        assertThatThrownBy(() -> new Grid<>(0, 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Grid<>(1, 0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_copiesTheList() {
        List<String> cells = new ArrayList<>(List.of("a", "b"));
        Grid<String> grid = new Grid<>(2, 1, cells);

        cells.set(0, "z");

        assertThat(grid.get(new Position(0, 0))).isEqualTo("a");
        assertThatThrownBy(() -> grid.cells().set(0, "z"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
