package io.github.matteocardarelli.simpleroguelike.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import io.github.matteocardarelli.simpleroguelike.rules.PoisonRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import org.junit.jupiter.api.Test;

class PoisonHookTest {

    private static final PoisonHook HOOK =
            new PoisonHook(
                    new PoisonRules(new DamageRules(new LevelingRules(new Stats(20, 2), 150))));
    private static final EntityId PLAYER = new EntityId(1);

    @Test
    void afterTurn_aPoisonedPlayer_takesThePoisonDamage_onceAndOnlyWhilePoisoned() {
        Run run = run();
        run.floor().poison(PLAYER);

        assertThat(HOOK.afterTurn(run)).anyMatch(Damaged.class::isInstance);
        assertThat(run.floor().player().health()).isEqualTo(98);
        for (int i = 0; i < 4; i++) {
            HOOK.afterTurn(run);
        }
        assertThat(HOOK.afterTurn(run)).isEmpty();
        assertThat(run.floor().player().health()).isEqualTo(90);
    }

    @Test
    void order_isBeforeTheLoot() {
        assertThat(HOOK.order()).isLessThan(LootHook.ORDER);
    }

    private static Run run() {
        return Run.start(1, new FloorSpec(3, 3, 0, 0, 0, 1), new FixedFloorSource("@.x"));
    }
}
