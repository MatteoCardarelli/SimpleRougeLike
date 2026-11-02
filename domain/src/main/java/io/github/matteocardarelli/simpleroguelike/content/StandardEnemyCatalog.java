package io.github.matteocardarelli.simpleroguelike.content;

import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import java.util.List;

/** I nemici del gioco. Un nemico nuovo è una riga in più qui. */
public final class StandardEnemyCatalog implements EnemyCatalog {

    public static final Kind GOBLIN = Kind.of("goblin");

    /** Insegue il player quando è abbastanza vicino, altrimenti vaga. */
    public static final BehaviorKey CHASE_OR_WANDER = BehaviorKey.of("chase_or_wander");

    /**
     * Tira frecce da lontano, si avvicina solo per avere il player a tiro, e scappa se è vicino.
     */
    public static final Kind ARCHER = Kind.of("archer");

    /** Tiene la distanza e tira; se il player è lontano, vaga. */
    public static final BehaviorKey KEEP_DISTANCE = BehaviorKey.of("keep_distance");

    private static final List<EnemyDefinition> ENEMIES =
            List.of(
                    new EnemyDefinition(
                            GOBLIN, new Stats(30, 7, 1, 0, 5), 25, 8, CHASE_OR_WANDER, 10),
                    new EnemyDefinition(
                            ARCHER, new Stats(20, 6, 0, 5, 10), 30, 8, KEEP_DISTANCE, 4, 5));

    @Override
    public List<EnemyDefinition> all() {
        return ENEMIES;
    }
}
