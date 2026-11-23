package io.github.matteocardarelli.simpleroguelike.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.TrapDiscovered;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.rules.TrapRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrapDiscoveryHookTest {

    private static final TrapDiscoveryHook HOOK =
            new TrapDiscoveryHook(new TrapRules(Sessions.DAMAGE));

    @Test
    void afterTurn_discoversAnAdjacentTrap_sooner_orLater_fromTheGameplayStream() {
        Run run = run("@^.");
        List<GameEvent> found = new ArrayList<>();

        for (int turn = 0; turn < 100 && found.isEmpty(); turn++) {
            found.addAll(HOOK.afterTurn(run));
        }

        assertThat(found)
                .containsExactly(new TrapDiscovered(Kind.of("trap_spikes"), new Position(1, 0)));
        assertThat(HOOK.afterTurn(run)).isEmpty();
    }

    @Test
    void afterTurn_aFarTrap_staysHidden() {
        Run run = run("@.^");

        for (int turn = 0; turn < 100; turn++) {
            assertThat(HOOK.afterTurn(run)).isEmpty();
        }
        assertThat(run.floor().fixtureAt(new Position(2, 0)).orElseThrow().isHidden()).isTrue();
    }

    @Test
    void afterTurn_sameSeed_discoversOnTheSameTurn() {
        assertThat(turnOfDiscovery(run("@^."))).isEqualTo(turnOfDiscovery(run("@^.")));
    }

    @Test
    void order_isBeforeThePoisonAndTheLoot() {
        assertThat(HOOK.order()).isLessThan(PoisonHook.ORDER).isLessThan(LootHook.ORDER);
    }

    private static int turnOfDiscovery(Run run) {
        for (int turn = 1; turn <= 100; turn++) {
            if (!HOOK.afterTurn(run).isEmpty()) {
                return turn;
            }
        }
        return -1;
    }

    private static Run run(String... rows) {
        return Run.start(7, new FloorSpec(3, 3, 0, 0, 0, 1), new FixedFloorSource(rows));
    }
}
