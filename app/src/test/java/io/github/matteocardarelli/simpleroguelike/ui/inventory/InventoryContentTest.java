package io.github.matteocardarelli.simpleroguelike.ui.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InventoryContentTest {

    private static final InventoryEntry POTION =
            new InventoryEntry(new EntityId(7), Kind.of("potion"));

    @Test
    void selectedEntry_isTheSelectedItem() {
        InventoryContent content =
                new InventoryContent(List.of(POTION), Optional.of(new EntityId(7)));

        assertThat(content.selectedEntry()).contains(POTION);
    }

    @Test
    void selectedEntry_ofAnItemNoLongerInTheInventory_isEmpty() {
        InventoryContent content = new InventoryContent(List.of(), Optional.of(new EntityId(7)));

        assertThat(content.selectedEntry()).isEmpty();
    }
}
