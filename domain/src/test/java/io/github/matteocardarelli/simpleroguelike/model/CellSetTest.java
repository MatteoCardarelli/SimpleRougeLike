package io.github.matteocardarelli.simpleroguelike.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CellSetTest {

    @Test
    void positions_anyInsertionOrder_isRowOrder() {
        CellSet cells =
                CellSet.of(
                        3, 3, List.of(new Position(2, 2), new Position(0, 1), new Position(1, 0)));

        assertThat(cells.positions())
                .containsExactly(new Position(1, 0), new Position(0, 1), new Position(2, 2));
    }

    @Test
    void contains_onlyAddedCells() {
        CellSet cells = CellSet.of(3, 3, List.of(new Position(1, 1)));

        assertThat(cells.contains(new Position(1, 1))).isTrue();
        assertThat(cells.contains(new Position(0, 1))).isFalse();
    }

    @Test
    void contains_outside_isFalse() {
        CellSet cells = CellSet.of(2, 2, List.of(new Position(0, 0)));

        assertThat(cells.contains(new Position(-1, 0))).isFalse();
        assertThat(cells.contains(new Position(2, 0))).isFalse();
        assertThat(cells.contains(new Position(0, -1))).isFalse();
        assertThat(cells.contains(new Position(0, 2))).isFalse();
    }

    @Test
    void of_duplicates_countOnce() {
        CellSet cells = CellSet.of(2, 2, List.of(new Position(1, 1), new Position(1, 1)));

        assertThat(cells.size()).isEqualTo(1);
    }

    @Test
    void of_cellOutside_isRejected() {
        assertThatThrownBy(() -> CellSet.of(2, 2, List.of(new Position(2, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void empty_hasNoCells() {
        CellSet empty = CellSet.empty(4, 3);

        assertThat(empty.isEmpty()).isTrue();
        assertThat(empty.size()).isZero();
        assertThat(empty.positions()).isEmpty();
        assertThat(empty.width()).isEqualTo(4);
        assertThat(empty.height()).isEqualTo(3);
    }

    @Test
    void empty_invalidDimensions_areRejected() {
        assertThatThrownBy(() -> CellSet.empty(0, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CellSet.empty(3, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void largeGrid_usesSeveralWords() {
        List<Position> cells = List.of(new Position(0, 0), new Position(63, 0), new Position(5, 9));
        CellSet set = CellSet.of(64, 10, cells);

        assertThat(set.size()).isEqualTo(3);
        assertThat(set.positions()).containsExactlyElementsOf(cells);
        assertThat(set.contains(new Position(6, 9))).isFalse();
    }

    @Test
    void union_containsCellsOfBoth() {
        CellSet a = CellSet.of(3, 1, List.of(new Position(0, 0)));
        CellSet b = CellSet.of(3, 1, List.of(new Position(2, 0)));

        CellSet union = a.union(b);

        assertThat(union.positions()).containsExactly(new Position(0, 0), new Position(2, 0));
        assertThat(a.size()).isEqualTo(1);
    }

    @Test
    void union_differentDimensions_isRejected() {
        assertThatThrownBy(() -> CellSet.empty(2, 2).union(CellSet.empty(2, 3)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CellSet.empty(2, 2).union(CellSet.empty(3, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equals_byContentAndDimensions() {
        CellSet a = CellSet.of(3, 3, List.of(new Position(1, 1), new Position(0, 0)));
        CellSet b = CellSet.of(3, 3, List.of(new Position(0, 0), new Position(1, 1)));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(CellSet.of(3, 3, List.of(new Position(0, 0))));
        assertThat(CellSet.empty(3, 3)).isNotEqualTo(CellSet.empty(3, 2));
        assertThat(CellSet.empty(3, 3)).isNotEqualTo(CellSet.empty(2, 3));
        assertThat(a).isNotEqualTo("CellSet");
    }

    @Test
    void of_copiesTheCollection() {
        List<Position> source = new ArrayList<>(List.of(new Position(0, 0)));
        CellSet cells = CellSet.of(2, 2, source);

        source.add(new Position(1, 1));

        assertThat(cells.size()).isEqualTo(1);
        assertThatThrownBy(() -> cells.positions().add(new Position(1, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toString_listsTheCells() {
        assertThat(CellSet.of(2, 2, List.of(new Position(1, 0))).toString())
                .contains("2x2")
                .contains("x=1");
    }
}
