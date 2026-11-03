package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.Optional;

/**
 * Un oggetto, a terra o nell'inventario. Cosa fa lo dice la sua definizione. Un oggetto che si
 * impila, come i coltelli, conta quanti pezzi ha: sempre almeno 1.
 */
public final class Item {

    private final EntityId id;
    private final ItemDefinition definition;
    private int count = 1;

    public Item(EntityId id, ItemDefinition definition) {
        this.id = id;
        this.definition = definition;
    }

    public EntityId id() {
        return id;
    }

    public Kind kind() {
        return definition.kind();
    }

    public boolean isConsumable() {
        return definition.consumable();
    }

    /** Quanti pezzi ha: più di 1 solo per un oggetto che si impila. */
    public int count() {
        return count;
    }

    /** Danno e portata, se l'oggetto si lancia. */
    public Optional<Throwing> throwing() {
        return definition.throwing();
    }

    /** Rimette il numero di pezzi salvato; solo per riprendere una partita. */
    void restoreCount(int value) {
        if (value < 1 || (value > 1 && !definition.stackable())) {
            throw new IllegalArgumentException("Pezzi salvati non validi: " + value);
        }
        count = value;
    }

    /** Aggiunge i pezzi di un altro oggetto dello stesso tipo. */
    void absorb(Item other) {
        if (!definition.stackable() || !other.kind().equals(kind())) {
            throw new IllegalArgumentException(
                    other.kind().value() + " non si impila su " + kind().value());
        }
        count = Math.addExact(count, other.count);
    }

    /**
     * Toglie un pezzo.
     *
     * @return vero se non ne resta nessuno
     */
    boolean spendOne() {
        count--;
        return count == 0;
    }

    /** Lo spazio e il bonus, se l'oggetto si equipaggia. */
    public Optional<Equipment> equipment() {
        return definition.equipment();
    }

    public ItemDefinition definition() {
        return definition;
    }
}
