package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;

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

    public ItemDefinition definition() {
        return definition;
    }
}
