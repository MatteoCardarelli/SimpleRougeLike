package io.github.matteocardarelli.simpleroguelike.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PositionTest {

    @Test
    void translate_eachDirection_movesOneCell() {
        Position origin = new Position(3, 3);

        assertThat(origin.translate(Direction.UP)).isEqualTo(new Position(3, 2));
        assertThat(origin.translate(Direction.DOWN)).isEqualTo(new Position(3, 4));
        assertThat(origin.translate(Direction.LEFT)).isEqualTo(new Position(2, 3));
        assertThat(origin.translate(Direction.RIGHT)).isEqualTo(new Position(4, 3));
    }

    @Test
    void distanceTo_diagonal_isManhattan() {
        assertThat(new Position(1, 1).distanceTo(new Position(4, 5))).isEqualTo(7);
        assertThat(new Position(4, 5).distanceTo(new Position(1, 1))).isEqualTo(7);
    }

    @Test
    void isAdjacentTo_onlyForCellsAtDistanceOne() {
        Position origin = new Position(2, 2);

        assertThat(origin.isAdjacentTo(new Position(2, 3))).isTrue();
        assertThat(origin.isAdjacentTo(new Position(3, 3))).isFalse();
        assertThat(origin.isAdjacentTo(origin)).isFalse();
    }

    @Test
    void directionTo_adjacentCell_returnsTheStep() {
        Position origin = new Position(2, 2);

        assertThat(origin.directionTo(new Position(2, 1))).contains(Direction.UP);
        assertThat(origin.directionTo(new Position(1, 2))).contains(Direction.LEFT);
    }

    @Test
    void directionTo_farOrDiagonalCell_isEmpty() {
        Position origin = new Position(2, 2);

        assertThat(origin.directionTo(new Position(2, 4))).isEmpty();
        assertThat(origin.directionTo(new Position(3, 3))).isEmpty();
        assertThat(origin.directionTo(origin)).isEmpty();
    }
}
