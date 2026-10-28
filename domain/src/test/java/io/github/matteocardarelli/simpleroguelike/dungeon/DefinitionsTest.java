package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefinitionsTest {

    @Test
    void definitions_negativeValues_areRejected() {
        Kind kind = Kind.of("thing");
        BehaviorKey behavior = BehaviorKey.of("idle");

        assertThatThrownBy(() -> new ItemDefinition(kind, context -> List.of(), true, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnemyDefinition(kind, new Stats(0, 1), 1, 1, behavior, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnemyDefinition(kind, new Stats(1, 1), -1, 1, behavior, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnemyDefinition(kind, new Stats(1, 1), 1, -1, behavior, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnemyDefinition(kind, new Stats(1, 1), 1, 1, behavior, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
