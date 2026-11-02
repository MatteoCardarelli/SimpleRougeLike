package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.EquipmentSlot;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.Optional;

/**
 * Un oggetto nell'inventario del player.
 *
 * @param slot lo spazio in cui si equipaggia, vuoto per un oggetto che si usa o si lancia
 * @param equipped se il player lo porta addosso
 * @param throwable se si lancia, come un coltello
 * @param count quanti pezzi ha: più di 1 solo per un oggetto che si impila
 */
public record InventoryEntry(
        EntityId id,
        Kind kind,
        Optional<EquipmentSlot> slot,
        boolean equipped,
        boolean throwable,
        int count) {

    /** Un oggetto che si usa, come una pozione. */
    public InventoryEntry(EntityId id, Kind kind) {
        this(id, kind, Optional.empty(), false, false, 1);
    }

    /** Un oggetto che si usa o si equipaggia, in un solo pezzo. */
    public InventoryEntry(EntityId id, Kind kind, Optional<EquipmentSlot> slot, boolean equipped) {
        this(id, kind, slot, equipped, false, 1);
    }
}
