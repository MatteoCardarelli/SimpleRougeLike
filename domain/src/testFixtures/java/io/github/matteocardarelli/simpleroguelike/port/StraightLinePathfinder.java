package io.github.matteocardarelli.simpleroguelike.port;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un {@link Pathfinder} finto e prevedibile: va dritto verso l'arrivo, prima lungo l'asse con la
 * distanza maggiore (a parità, in orizzontale), poi lungo l'altro se il primo passo è bloccato. Non
 * aggira gli ostacoli. Passa lo stesso contratto dell'adapter vero.
 */
public final class StraightLinePathfinder implements Pathfinder {

    @Override
    public Optional<Direction> nextStep(
            Grid<TileType> terrain, CellSet blocked, Position from, Position to) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        Optional<Direction> horizontal =
                dx == 0 ? Optional.empty() : Optional.of(dx > 0 ? Direction.RIGHT : Direction.LEFT);
        Optional<Direction> vertical =
                dy == 0 ? Optional.empty() : Optional.of(dy > 0 ? Direction.DOWN : Direction.UP);
        List<Optional<Direction>> candidates = new ArrayList<>();
        if (Math.abs(dx) >= Math.abs(dy)) {
            candidates.add(horizontal);
            candidates.add(vertical);
        } else {
            candidates.add(vertical);
            candidates.add(horizontal);
        }
        for (Optional<Direction> candidate : candidates) {
            if (candidate.isPresent()) {
                Position next = from.translate(candidate.get());
                boolean free =
                        terrain.isInside(next)
                                && terrain.get(next).isWalkable()
                                && !blocked.contains(next);
                if (next.equals(to) || free) {
                    return candidate;
                }
            }
        }
        return Optional.empty();
    }
}
