package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Gli oggetti del player, in ordine di raccolta, fino a una capienza massima. Un oggetto che si
 * impila si aggiunge alla pila dello stesso tipo, se c'è, senza occupare un posto in più.
 */
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
        if (item.definition().stackable()) {
            for (Item stack : items) {
                if (stack.kind().equals(item.kind())) {
                    stack.absorb(item);
                    return true;
                }
            }
        }
        if (isFull()) {
            return false;
        }
        items.add(item);
        return true;
    }

    /** Toglie un pezzo dell'oggetto, e l'oggetto quando non ne resta nessuno. */
    void removeOne(EntityId id) {
        Item item =
                find(id).orElseThrow(
                                () -> new IllegalArgumentException(id + " non è nell'inventario"));
        if (item.spendOne()) {
            items.remove(item);
        }
    }

    Optional<Item> remove(EntityId id) {
        Optional<Item> item = find(id);
        item.ifPresent(items::remove);
        return item;
    }
}
