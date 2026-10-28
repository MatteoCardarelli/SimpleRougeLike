package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.port.Pathfinder;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Va verso il player lungo un percorso che evita gli altri nemici vivi. Se è già accanto al player,
 * il passo verso di lui è un attacco. Aspetta se il player è morto o se non c'è un passo possibile.
 */
public final class ChaseBehavior implements EnemyBehavior {

    private final Pathfinder pathfinder;

    public ChaseBehavior(Pathfinder pathfinder) {
        this.pathfinder = pathfinder;
    }

    @Override
    public EnemyIntent decide(Enemy enemy, FloorView floor, RandomSource random) {
        if (!floor.player().isAlive()) {
            return new WaitIntent();
        }
        List<Position> others = new ArrayList<>();
        for (Enemy other : floor.enemies()) {
            if (!other.id().equals(enemy.id())) {
                others.add(floor.positionOf(other.id()));
            }
        }
        CellSet blocked = CellSet.of(floor.terrain().width(), floor.terrain().height(), others);
        return pathfinder
                .nextStep(
                        floor.terrain(),
                        blocked,
                        floor.positionOf(enemy.id()),
                        floor.positionOf(floor.playerId()))
                .<EnemyIntent>map(StepIntent::new)
                .orElseGet(WaitIntent::new);
    }
}
