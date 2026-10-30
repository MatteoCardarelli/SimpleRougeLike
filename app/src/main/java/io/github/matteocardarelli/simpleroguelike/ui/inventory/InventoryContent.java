package io.github.matteocardarelli.simpleroguelike.ui.inventory;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import java.util.List;
import java.util.Optional;

/**
 * Cosa mostra l'inventario: gli oggetti e quello selezionato, se c'è.
 *
 * @param items gli oggetti, in ordine di raccolta
 */
public record InventoryContent(List<InventoryEntry> items, Optional<EntityId> selected) {

    public InventoryContent {
        items = List.copyOf(items);
    }

    /** L'oggetto selezionato, se è ancora nell'inventario. */
    public Optional<InventoryEntry> selectedEntry() {
        for (InventoryEntry entry : items) {
            if (selected.isPresent() && entry.id().equals(selected.get())) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }
}
