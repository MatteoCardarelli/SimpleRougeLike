package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    void inventory_addFindRemove() {
        Inventory inventory = new Inventory(2);
        Item first = new Item(new EntityId(1), FloorFixtures.potion());
        Item second = new Item(new EntityId(2), FloorFixtures.potion());

        assertThat(inventory.add(first)).isTrue();
        assertThat(inventory.add(second)).isTrue();
        assertThat(inventory.isFull()).isTrue();
        assertThat(inventory.add(new Item(new EntityId(3), FloorFixtures.potion()))).isFalse();
        assertThat(inventory.find(new EntityId(2))).contains(second);
        assertThat(inventory.remove(new EntityId(1))).contains(first);
        assertThat(inventory.remove(new EntityId(1))).isEmpty();
        assertThat(inventory.items()).containsExactly(second);
        assertThat(inventory.isFull()).isFalse();
    }
}
