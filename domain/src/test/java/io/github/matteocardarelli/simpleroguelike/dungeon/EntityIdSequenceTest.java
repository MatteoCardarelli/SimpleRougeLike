package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import org.junit.jupiter.api.Test;

class EntityIdSequenceTest {

    @Test
    void entityIdSequence_countsFromOne() {
        EntityIdSequence ids = new EntityIdSequence();

        assertThat(ids.next()).isEqualTo(new EntityId(1));
        assertThat(ids.next()).isEqualTo(new EntityId(2));
    }
}
