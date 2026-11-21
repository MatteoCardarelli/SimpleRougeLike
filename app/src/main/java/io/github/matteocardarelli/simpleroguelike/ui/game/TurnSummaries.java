package io.github.matteocardarelli.simpleroguelike.ui.game;

import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.DoorOpened;
import io.github.matteocardarelli.simpleroguelike.event.DoorUnlocked;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import io.github.matteocardarelli.simpleroguelike.ui.input.TurnSummary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Riassume un risultato del turno per {@link
 * io.github.matteocardarelli.simpleroguelike.ui.input.HoldToWalk}. Gli eventi si riconoscono con un
 * registro per classe, non con {@code instanceof}: un passo avanti è un movimento o un attacco del
 * player, o una porta che si apre o si sblocca; un danno è quello subito dal player.
 */
final class TurnSummaries {

    /** Se un evento, visto dal player, conta. */
    private interface Test {
        boolean apply(GameEvent event, EntityId player);
    }

    private static final Map<Class<? extends GameEvent>, Test> PROGRESS = new HashMap<>();
    private static final Map<Class<? extends GameEvent>, Test> DAMAGE = new HashMap<>();

    static {
        PROGRESS.put(Moved.class, (e, player) -> ((Moved) e).actor().equals(player));
        PROGRESS.put(Attacked.class, (e, player) -> ((Attacked) e).attacker().equals(player));
        PROGRESS.put(DoorOpened.class, (e, player) -> true);
        PROGRESS.put(DoorUnlocked.class, (e, player) -> true);
        DAMAGE.put(Damaged.class, (e, player) -> ((Damaged) e).target().equals(player));
    }

    private TurnSummaries() {}

    static TurnSummary of(TurnResult result, EntityId player) {
        boolean progressed = false;
        boolean damaged = false;
        for (GameEvent event : result.events()) {
            progressed |= matches(PROGRESS, event, player);
            damaged |= matches(DAMAGE, event, player);
        }
        Set<EntityId> enemies = new HashSet<>();
        for (ActorView enemy : result.snapshot().visibleEnemies()) {
            enemies.add(enemy.id());
        }
        return new TurnSummary(result.revision(), progressed, damaged, enemies);
    }

    private static boolean matches(
            Map<Class<? extends GameEvent>, Test> tests, GameEvent event, EntityId player) {
        Test test = tests.get(event.getClass());
        return test != null && test.apply(event, player);
    }
}
