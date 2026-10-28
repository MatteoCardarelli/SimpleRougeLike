package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;

/**
 * Sceglie a ogni turno tra due strategie: la prima se il player è vivo ed entro {@code aggroRange}
 * passi dal nemico, la seconda altrimenti.
 */
public final class AggroBehavior implements EnemyBehavior {

    private final EnemyBehavior chase;
    private final EnemyBehavior wander;

    public AggroBehavior(EnemyBehavior chase, EnemyBehavior wander) {
        this.chase = chase;
        this.wander = wander;
    }

    @Override
    public EnemyIntent decide(Enemy enemy, FloorView floor, RandomSource random) {
        int distance = floor.positionOf(enemy.id()).distanceTo(floor.positionOf(floor.playerId()));
        boolean aggro = floor.player().isAlive() && distance <= enemy.aggroRange();
        return (aggro ? chase : wander).decide(enemy, floor, random);
    }
}
