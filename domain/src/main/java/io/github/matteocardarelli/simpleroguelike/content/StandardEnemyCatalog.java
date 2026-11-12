package io.github.matteocardarelli.simpleroguelike.content;

import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.Footprint;
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

    /** Debole da solo, nasce in branchi di tre che accerchiano il player. */
    public static final Kind WOLF = Kind.of("wolf");

    /** Accerchia il player con il resto del branco; se il player è lontano, vaga. */
    public static final BehaviorKey PACK = BehaviorKey.of("pack");

    /** Il boss dell'ultimo piano: uno slime grande come un cerchio di 4 celle. */
    public static final Kind GIANT_SLIME = Kind.of("giant_slime");

    /** Salta sul player e lo schiaccia, poi riposa un turno. */
    public static final BehaviorKey JUMP_AND_CRUSH = BehaviorKey.of("jump_and_crush");

    /** Vende la merce del negozio; derubato diventa fortissimo e fa due azioni per turno. */
    public static final Kind MERCHANT = Kind.of("merchant");

    /** Sta al banco; derubato colpisce da vicino e da lontano e insegue. */
    public static final BehaviorKey MERCHANT_BEHAVIOR = BehaviorKey.of("merchant");

    /** Tiene la distanza e tira; se il player è lontano, vaga. */
    public static final BehaviorKey KEEP_DISTANCE = BehaviorKey.of("keep_distance");

    private static final List<EnemyDefinition> ENEMIES =
            List.of(
                    new EnemyDefinition(
                            GOBLIN,
                            new Stats(30, 7, 1, 0, 5),
                            25,
                            8,
                            CHASE_OR_WANDER,
                            10,
                            0,
                            1,
                            25),
                    new EnemyDefinition(
                            ARCHER, new Stats(20, 6, 0, 5, 10), 30, 8, KEEP_DISTANCE, 4, 5, 1, 30),
                    new EnemyDefinition(
                            WOLF, new Stats(14, 5, 0, 5, 10), 15, 10, PACK, 3, 0, 3, 15),
                    new EnemyDefinition(
                            GIANT_SLIME,
                            new Stats(220, 16, 4, 10, 0),
                            300,
                            14,
                            JUMP_AND_CRUSH,
                            0,
                            0,
                            1,
                            0,
                            Footprint.circle(4),
                            Footprint.circle(8)),
                    new EnemyDefinition(
                            MERCHANT,
                            new Stats(400, 30, 10, 40, 20),
                            100,
                            40,
                            MERCHANT_BEHAVIOR,
                            0,
                            6,
                            1,
                            0,
                            Footprint.SINGLE,
                            false,
                            true,
                            2));

    @Override
    public List<EnemyDefinition> all() {
        return ENEMIES;
    }
}
