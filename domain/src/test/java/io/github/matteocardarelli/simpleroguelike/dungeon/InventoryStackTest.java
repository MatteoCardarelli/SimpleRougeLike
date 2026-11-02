package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import org.junit.jupiter.api.Test;

class InventoryStackTest {

    private static final EntityId PLAYER = new EntityId(1);

    private static Floor pickUpAll(int capacity, String row) {
        Floor floor = FloorFixtures.parse(capacity, row);
        for (int i = 1; i < row.length(); i++) {
            floor.move(PLAYER, Direction.RIGHT);
            floor.pickUp(PLAYER);
        }
        return floor;
    }

    @Test
    void knives_stackInOnePlace() {
        Floor floor = pickUpAll(5, "@---");

        assertThat(floor.player().inventory().items())
                .singleElement()
                .satisfies(
                        knives -> {
                            assertThat(knives.id()).isEqualTo(new EntityId(2));
                            assertThat(knives.count()).isEqualTo(3);
                        });
        assertThat(floor.itemsOnGround()).isEmpty();
    }

    @Test
    void knives_stackEvenWithAFullInventory() {
        Floor floor = pickUpAll(2, "@-!-");

        assertThat(floor.player().inventory().isFull()).isTrue();
        assertThat(floor.player().inventory().items().get(0).count()).isEqualTo(2);
    }

    @Test
    void spendThrowable_takesOne_andTheLastRemovesTheStack() {
        Floor floor = pickUpAll(5, "@--");
        EntityId knives = new EntityId(2);

        Throwing throwing = floor.spendThrowable(PLAYER, knives);

        assertThat(throwing.range()).isPositive();
        assertThat(floor.player().inventory().find(knives).orElseThrow().count()).isEqualTo(1);
        floor.spendThrowable(PLAYER, knives);
        assertThat(floor.player().inventory().find(knives)).isEmpty();
    }

    @Test
    void spendThrowable_onAPotion_isRejected() {
        Floor floor = pickUpAll(5, "@!");

        assertThatThrownBy(() -> floor.spendThrowable(PLAYER, new EntityId(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwing_needsPositiveValues() {
        assertThatThrownBy(() -> new Throwing(0, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Throwing(3, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
