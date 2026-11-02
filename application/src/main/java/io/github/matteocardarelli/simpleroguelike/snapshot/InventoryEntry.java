package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.EquipmentSlot;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.Optional;

/**
 * Un oggetto nell'inventario del player.
 *
 * @param slot lo spazio in cui si equipaggia, vuoto per un oggetto che si usa
 * @param equipped se il player lo porta addosso
 */
public record InventoryEntry(
        EntityId id, Kind kind, Optional<EquipmentSlot> slot, boolean equipped) {

    /** Un oggetto che si usa, come una pozione. */
    public InventoryEntry(EntityId id, Kind kind) {
        this(id, kind, Optional.empty(), false);
    }
}
