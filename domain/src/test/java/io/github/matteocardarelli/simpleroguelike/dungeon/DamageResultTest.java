package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.ExperienceGained;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DamageResultTest {

    @Test
    void damageResult_copiesEvents() {
        List<GameEvent> events = new ArrayList<>();
        DamageResult result = new DamageResult(0, 0, false, events);

        events.add(new ExperienceGained(new Position(0, 0), 1));

        assertThat(result.events()).isEmpty();
    }
}
