package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import org.junit.jupiter.api.Test;

class ItemTest {

    @Test
    void item_takesValuesFromItsDefinition() {
        Item potion = new Item(new EntityId(4), FloorFixtures.potion());

        assertThat(potion.id()).isEqualTo(new EntityId(4));
        assertThat(potion.kind()).isEqualTo(Kind.of("potion"));
        assertThat(potion.isConsumable()).isTrue();
    }
}
