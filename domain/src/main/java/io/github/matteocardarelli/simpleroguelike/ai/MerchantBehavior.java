package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.Optional;

/**
 * Il mercante: finché il player non lo deruba sta fermo al suo banco. Derubato colpisce da vicino
 * se il player gli è accanto, tira da lontano se lo ha in linea, e altrimenti lo insegue; ogni
 * decisione vale per una delle sue azioni del turno.
 */
public final class MerchantBehavior implements EnemyBehavior {

    private final EnemyBehavior chase;

    public MerchantBehavior(EnemyBehavior chase) {
        this.chase = chase;
    }

    @Override
    public EnemyIntent decide(Enemy enemy, FloorView floor, RandomSource random) {
        if (!enemy.isAngry() || !floor.player().isAlive()) {
            return new WaitIntent();
        }
        Position at = floor.positionOf(enemy.id());
        Position player = floor.positionOf(floor.playerId());
        Optional<Direction> melee = at.directionTo(player);
        if (melee.isPresent()) {
            return new StepIntent(melee.get());
        }
        Optional<Direction> aim = KiteBehavior.aim(floor, at, player, enemy.range());
        if (aim.isPresent()) {
            return new RangedAttackIntent(aim.get());
        }
        return chase.decide(enemy, floor, random);
    }
}
