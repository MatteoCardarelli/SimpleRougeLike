package io.github.matteocardarelli.simpleroguelike.ui.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.TrapDiscovered;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.session.SnapshotMapper;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import java.util.List;
import org.junit.jupiter.api.Test;

class TurnSummariesTest {

    private static final EntityId PLAYER = new EntityId(1);

    @Test
    void of_aDiscoveredTrap_isADangerSignal_likeDamage() {
        TurnResult result = result(new TrapDiscovered(Kind.of("trap_spikes"), new Position(1, 0)));

        assertThat(TurnSummaries.of(result, PLAYER).playerDamaged()).isTrue();
    }

    @Test
    void of_withoutEvents_isNotADangerSignal() {
        assertThat(TurnSummaries.of(result(), PLAYER).playerDamaged()).isFalse();
    }

    private static TurnResult result(GameEvent... events) {
        Floor floor = FloorFixtures.parse("@^");
        return new TurnResult(
                1, true, List.of(events), new SnapshotMapper().from(floor, Phase.PLAYING, 1, 1));
    }
}
