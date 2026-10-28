package io.github.matteocardarelli.simpleroguelike.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntityIdTest {

    @Test
    void entityId_negative_isRejected() {
        assertThatThrownBy(() -> new EntityId(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void entityId_compareTo_ordersByValue() {
        List<EntityId> ids =
                new ArrayList<>(List.of(new EntityId(3), new EntityId(0), new EntityId(2)));

        ids.sort(null);

        assertThat(ids).containsExactly(new EntityId(0), new EntityId(2), new EntityId(3));
    }
}
