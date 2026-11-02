package io.github.matteocardarelli.simpleroguelike.generation;

import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Restituisce piani disegnati in ASCII, come in {@link FloorFixtures}, con gli id della partita.
 * Ignora la specifica e il caso: serve a giocare turni completi su una situazione nota. Il primo
 * piano è il primo disegno; scendendo si passa ai disegni successivi, e dopo l'ultimo si ripete
 * l'ultimo.
 */
public final class FixedFloorSource implements FloorSource {

    private final int inventoryCapacity;
    private final List<String[]> floors;
    private int next;

    public FixedFloorSource(String... rows) {
        this(FloorFixtures.INVENTORY_CAPACITY, rows);
    }

    public FixedFloorSource(int inventoryCapacity, String... rows) {
        this.inventoryCapacity = inventoryCapacity;
        this.floors = List.<String[]>of(rows.clone());
    }

    private FixedFloorSource(List<String[]> floors) {
        this.inventoryCapacity = FloorFixtures.INVENTORY_CAPACITY;
        List<String[]> copies = new ArrayList<>();
        for (String[] rows : floors) {
            copies.add(rows.clone());
        }
        this.floors = List.copyOf(copies);
    }

    /** Più piani disegnati, uno per profondità. */
    public static FixedFloorSource floors(String[]... floors) {
        return new FixedFloorSource(List.of(floors));
    }

    @Override
    public Floor create(FloorSpec spec, RandomSource random, EntityIdSequence ids) {
        next = 1;
        return FloorFixtures.parse(ids, inventoryCapacity, Optional.empty(), floors.get(0));
    }

    @Override
    public Floor createNext(
            FloorSpec spec, RandomSource random, EntityIdSequence ids, Player player) {
        String[] rows = floors.get(Math.min(next, floors.size() - 1));
        next++;
        return FloorFixtures.parse(ids, inventoryCapacity, Optional.of(player), rows);
    }
}
