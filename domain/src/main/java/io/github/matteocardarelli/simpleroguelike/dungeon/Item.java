package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.Optional;

/** Un oggetto, a terra o nell'inventario. Cosa fa lo dice la sua definizione. */
public final class Item {

    private final EntityId id;
    private final ItemDefinition definition;

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

    /** Lo spazio e il bonus, se l'oggetto si equipaggia. */
    public Optional<Equipment> equipment() {
        return definition.equipment();
    }

    public ItemDefinition definition() {
        return definition;
    }
}
