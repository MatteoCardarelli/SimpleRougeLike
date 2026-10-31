package io.github.matteocardarelli.simpleroguelike.ui.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InventoryContentTest {

    private static final InventoryEntry POTION =
            new InventoryEntry(new EntityId(7), Kind.of("potion"));
    private static final PlayerView PLAYER =
            new PlayerView(new EntityId(1), new Position(3, 4), 38, 50, 1, 0, 100);

    @Test
    void selectedEntry_isTheSelectedItem() {
        InventoryContent content =
                new InventoryContent(List.of(POTION), Optional.of(new EntityId(7)), PLAYER);

        assertThat(content.selectedEntry()).contains(POTION);
    }

    @Test
    void selectedEntry_ofAnItemNoLongerInTheInventory_isEmpty() {
        InventoryContent content =
                new InventoryContent(List.of(), Optional.of(new EntityId(7)), PLAYER);

        assertThat(content.selectedEntry()).isEmpty();
    }
}
