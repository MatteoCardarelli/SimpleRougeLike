package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Gli oggetti del player, in ordine di raccolta, fino a una capienza massima. */
public final class Inventory {

    private final List<Item> items = new ArrayList<>();
    private final int capacity;

    Inventory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capienza non valida: " + capacity);
        }
        this.capacity = capacity;
    }

    /** Una vista non modificabile, che riflette i cambiamenti successivi. */
    public List<Item> items() {
        return Collections.unmodifiableList(items);
    }

    public int capacity() {
        return capacity;
    }

    public boolean isFull() {
        return items.size() >= capacity;
    }

    public Optional<Item> find(EntityId id) {
        for (Item item : items) {
            if (item.id().equals(id)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /** Restituisce falso, senza aggiungere l'oggetto, se l'inventario è pieno. */
    boolean add(Item item) {
        if (isFull()) {
            return false;
        }
        items.add(item);
        return true;
    }

    Optional<Item> remove(EntityId id) {
        Optional<Item> item = find(id);
        item.ifPresent(items::remove);
        return item;
    }
}
