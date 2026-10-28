package io.github.matteocardarelli.simpleroguelike.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Una griglia immutabile di valori, memorizzata per righe: la cella {@code (x, y)} è {@code
 * cells.get(y * width + x)}. Sostituisce gli array a due dimensioni, che si possono modificare da
 * fuori.
 *
 * @param cells i valori in ordine di riga; la lista viene copiata e non può contenere null
 */
public record Grid<T>(int width, int height, List<T> cells) {

    public Grid {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensioni non valide: " + width + "x" + height);
        }
        if (cells.size() != width * height) {
            throw new IllegalArgumentException(
                    "Una griglia "
                            + width
                            + "x"
                            + height
                            + " ha "
                            + width * height
                            + " celle, non "
                            + cells.size());
        }
        cells = List.copyOf(cells);
    }

    /** Crea una griglia chiedendo a {@code cell} il valore di ogni posizione. */
    public static <T> Grid<T> of(int width, int height, Function<Position, ? extends T> cell) {
        List<T> cells = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells.add(cell.apply(new Position(x, y)));
            }
        }
        return new Grid<>(width, height, cells);
    }

    /**
     * Il valore di una cella.
     *
     * @throws IndexOutOfBoundsException se la posizione è fuori dalla griglia
     */
    public T get(Position position) {
        if (!isInside(position)) {
            throw new IndexOutOfBoundsException(
                    position + " è fuori da una griglia " + width + "x" + height);
        }
        return cells.get(position.y() * width + position.x());
    }

    public boolean isInside(Position position) {
        return position.x() >= 0
                && position.x() < width
                && position.y() >= 0
                && position.y() < height;
    }

    /** Tutte le posizioni, in ordine di riga: prima {@code y}, poi {@code x}. */
    public List<Position> positions() {
        List<Position> positions = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                positions.add(new Position(x, y));
            }
        }
        return List.copyOf(positions);
    }

    public <R> Grid<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = new ArrayList<>(cells.size());
        for (T cell : cells) {
            mapped.add(mapper.apply(cell));
        }
        return new Grid<>(width, height, mapped);
    }
}
