package io.github.matteocardarelli.simpleroguelike.model;

/**
 * Identità di un attore o di un oggetto, unica in una partita. L'ordine per id è quello in cui gli
 * attori agiscono.
 */
public record EntityId(int value) implements Comparable<EntityId> {

    public EntityId {
        if (value < 0) {
            throw new IllegalArgumentException("Un id non può essere negativo: " + value);
        }
    }

    @Override
    public int compareTo(EntityId other) {
        return Integer.compare(value, other.value);
    }
}
