package io.github.matteocardarelli.simpleroguelike.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Un insieme immutabile di celle di una griglia {@code width x height}, memorizzato come maschera
 * di bit. Al posto di un {@code Set<Position>}: l'ordine di iterazione è sempre quello di riga,
 * qualunque sia il modo in cui l'insieme è stato creato, {@code contains} costa O(1) e non serve
 * nessun hash.
 */
public final class CellSet {

    private final int width;
    private final int height;
    private final long[] bits;

    private CellSet(int width, int height, long[] bits) {
        this.width = width;
        this.height = height;
        this.bits = bits;
    }

    public static CellSet empty(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensioni non valide: " + width + "x" + height);
        }
        return new CellSet(width, height, new long[words(width * height)]);
    }

    /**
     * L'insieme delle celle date; i doppioni contano una volta.
     *
     * @throws IllegalArgumentException se una cella è fuori dalla griglia
     */
    public static CellSet of(int width, int height, Collection<Position> cells) {
        CellSet empty = empty(width, height);
        long[] bits = empty.bits;
        for (Position cell : cells) {
            if (!empty.isInside(cell)) {
                throw new IllegalArgumentException(
                        cell + " è fuori da una griglia " + width + "x" + height);
            }
            int index = empty.index(cell);
            bits[index >>> 6] |= 1L << index;
        }
        return new CellSet(width, height, bits);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Falso anche per una cella fuori dalla griglia. */
    public boolean contains(Position cell) {
        if (!isInside(cell)) {
            return false;
        }
        int index = index(cell);
        return (bits[index >>> 6] & (1L << index)) != 0;
    }

    public int size() {
        int size = 0;
        for (long word : bits) {
            size += Long.bitCount(word);
        }
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /** Le celle dell'insieme, in ordine di riga: prima {@code y}, poi {@code x}. */
    public List<Position> positions() {
        List<Position> positions = new ArrayList<>(size());
        for (int index = 0; index < width * height; index++) {
            if ((bits[index >>> 6] & (1L << index)) != 0) {
                positions.add(new Position(index % width, index / width));
            }
        }
        return List.copyOf(positions);
    }

    /**
     * Le celle che stanno in almeno uno dei due insiemi.
     *
     * @throws IllegalArgumentException se i due insiemi hanno dimensioni diverse
     */
    public CellSet union(CellSet other) {
        if (width != other.width || height != other.height) {
            throw new IllegalArgumentException(
                    "Dimensioni diverse: "
                            + width
                            + "x"
                            + height
                            + " e "
                            + other.width
                            + "x"
                            + other.height);
        }
        long[] union = new long[bits.length];
        for (int i = 0; i < bits.length; i++) {
            union[i] = bits[i] | other.bits[i];
        }
        return new CellSet(width, height, union);
    }

    private boolean isInside(Position cell) {
        return cell.x() >= 0 && cell.x() < width && cell.y() >= 0 && cell.y() < height;
    }

    private int index(Position cell) {
        return cell.y() * width + cell.x();
    }

    private static int words(int cells) {
        return (cells + 63) >>> 6;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof CellSet that
                && width == that.width
                && height == that.height
                && Arrays.equals(bits, that.bits);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * width + height) + Arrays.hashCode(bits);
    }

    @Override
    public String toString() {
        return "CellSet" + width + "x" + height + positions();
    }
}
