package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;

/** Un oggetto nell'inventario del player. */
public record InventoryEntry(EntityId id, Kind kind) {}
