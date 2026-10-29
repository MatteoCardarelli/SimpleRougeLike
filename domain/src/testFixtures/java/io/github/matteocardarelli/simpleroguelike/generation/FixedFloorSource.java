package io.github.matteocardarelli.simpleroguelike.generation;

import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;

/**
 * Restituisce sempre lo stesso piano disegnato in ASCII, come in {@link FloorFixtures}, con gli id
 * della partita. Ignora la specifica e il caso: serve a giocare turni completi su una situazione
 * nota.
 */
public final class FixedFloorSource implements FloorSource {

    private final int inventoryCapacity;
    private final String[] rows;

    public FixedFloorSource(String... rows) {
        this(FloorFixtures.INVENTORY_CAPACITY, rows);
    }

    public FixedFloorSource(int inventoryCapacity, String... rows) {
        this.inventoryCapacity = inventoryCapacity;
        this.rows = rows.clone();
    }

    @Override
    public Floor create(FloorSpec spec, RandomSource random, EntityIdSequence ids) {
        return FloorFixtures.parse(ids, inventoryCapacity, rows);
    }
}
