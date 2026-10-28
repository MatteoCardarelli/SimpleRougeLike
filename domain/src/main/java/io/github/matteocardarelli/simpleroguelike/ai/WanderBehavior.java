package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Fa un passo a caso verso una delle celle libere intorno, solo nelle 4 direzioni. Aspetta se
 * nessuna cella intorno è libera.
 */
public final class WanderBehavior implements EnemyBehavior {

    @Override
    public EnemyIntent decide(Enemy enemy, FloorView floor, RandomSource random) {
        Position at = floor.positionOf(enemy.id());
        List<Direction> free = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (floor.isFree(at.translate(direction))) {
                free.add(direction);
            }
        }
        if (free.isEmpty()) {
            return new WaitIntent();
        }
        return new StepIntent(free.get(random.nextInt(free.size())));
    }
}
