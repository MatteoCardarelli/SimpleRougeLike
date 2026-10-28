package io.github.matteocardarelli.simpleroguelike.model;

/** Le quattro direzioni in cui ci si muove e si attacca. L'asse y cresce verso il basso. */
public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }
}
