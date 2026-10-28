package io.github.matteocardarelli.simpleroguelike.model;

import java.util.Optional;

/** Una cella della griglia. Le distanze sono di Manhattan, perché ci si muove in 4 direzioni. */
public record Position(int x, int y) {

    public Position translate(Direction direction) {
        return new Position(x + direction.dx(), y + direction.dy());
    }

    public int distanceTo(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    public boolean isAdjacentTo(Position other) {
        return distanceTo(other) == 1;
    }

    /**
     * La direzione che porta da questa cella a {@code other} con un passo solo.
     *
     * @return vuoto se le due celle non sono adiacenti
     */
    public Optional<Direction> directionTo(Position other) {
        for (Direction direction : Direction.values()) {
            if (translate(direction).equals(other)) {
                return Optional.of(direction);
            }
        }
        return Optional.empty();
    }
}
