package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Le celle che un attore occupa, come spostamenti dalla sua cella di riferimento, quella che il
 * piano tiene come posizione: per quasi tutti è solo la cella di riferimento.
 *
 * @param offsets gli spostamenti, il primo è sempre {@code (0, 0)} o la cella in alto a sinistra
 *     della forma
 */
public record Footprint(List<Position> offsets) {

    /** Una cella sola. */
    public static final Footprint SINGLE = new Footprint(List.of(new Position(0, 0)));

    public Footprint {
        offsets = List.copyOf(offsets);
        if (offsets.isEmpty()) {
            throw new IllegalArgumentException("Un'impronta occupa almeno una cella");
        }
        for (Position offset : offsets) {
            if (offset.x() < 0 || offset.y() < 0) {
                throw new IllegalArgumentException("Spostamento negativo: " + offset);
            }
        }
    }

    /**
     * Un cerchio di {@code diameter} celle di diametro: il quadrato senza gli angoli. Con 4 sono 12
     * celle.
     */
    public static Footprint circle(int diameter) {
        if (diameter < 1) {
            throw new IllegalArgumentException("Diametro non valido: " + diameter);
        }
        List<Position> cells = new ArrayList<>();
        double radius = diameter / 2.0;
        for (int y = 0; y < diameter; y++) {
            for (int x = 0; x < diameter; x++) {
                double dx = x + 0.5 - radius;
                double dy = y + 0.5 - radius;
                if (dx * dx + dy * dy <= radius * radius) {
                    cells.add(new Position(x, y));
                }
            }
        }
        return new Footprint(cells);
    }

    /** Un quadrato pieno di {@code side} celle di lato. */
    public static Footprint square(int side) {
        if (side < 1) {
            throw new IllegalArgumentException("Lato non valido: " + side);
        }
        List<Position> cells = new ArrayList<>();
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                cells.add(new Position(x, y));
            }
        }
        return new Footprint(cells);
    }

    /** Il lato del quadrato che contiene la forma, in celle: 1 per una cella sola. */
    public int size() {
        int size = 0;
        for (Position offset : offsets) {
            size = Math.max(size, Math.max(offset.x(), offset.y()) + 1);
        }
        return size;
    }

    /** Le celle occupate con la cella di riferimento in {@code anchor}. */
    public List<Position> at(Position anchor) {
        List<Position> cells = new ArrayList<>(offsets.size());
        for (Position offset : offsets) {
            cells.add(new Position(anchor.x() + offset.x(), anchor.y() + offset.y()));
        }
        return List.copyOf(cells);
    }
}
