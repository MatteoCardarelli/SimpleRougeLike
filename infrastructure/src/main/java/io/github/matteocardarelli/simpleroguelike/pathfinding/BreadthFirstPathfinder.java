package io.github.matteocardarelli.simpleroguelike.pathfinding;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.port.Pathfinder;
import java.util.ArrayDeque;
import java.util.Optional;

/**
 * Il percorso più breve in 4 direzioni, con una ricerca in ampiezza. I vicini si esplorano sempre
 * nell'ordine di {@link Direction}: tra due percorsi della stessa lunghezza vince sempre lo stesso,
 * quindi il risultato dipende solo dagli argomenti, senza bisogno di un generatore casuale.
 *
 * <p>Non tiene stato tra una chiamata e l'altra. Se l'arrivo non si può raggiungere, non c'è passo:
 * chi chiama decide cosa fare.
 */
public final class BreadthFirstPathfinder implements Pathfinder {

    @Override
    public Optional<Direction> nextStep(
            Grid<TileType> terrain, CellSet blocked, Position from, Position to) {
        if (from.equals(to)) {
            return Optional.empty();
        }
        int width = terrain.width();
        // Per ogni cella raggiunta, la direzione del primo passo da from con cui ci si arriva.
        Direction[] firstStep = new Direction[width * terrain.height()];
        boolean[] seen = new boolean[width * terrain.height()];
        seen[index(from, width)] = true;
        ArrayDeque<Position> queue = new ArrayDeque<>();
        queue.add(from);
        while (!queue.isEmpty()) {
            Position cell = queue.poll();
            for (Direction direction : Direction.values()) {
                Position next = cell.translate(direction);
                if (!terrain.isInside(next) || seen[index(next, width)]) {
                    continue;
                }
                Direction first = cell.equals(from) ? direction : firstStep[index(cell, width)];
                if (next.equals(to)) {
                    return Optional.of(first);
                }
                if (terrain.get(next).isWalkable() && !blocked.contains(next)) {
                    seen[index(next, width)] = true;
                    firstStep[index(next, width)] = first;
                    queue.add(next);
                }
            }
        }
        return Optional.empty();
    }

    private static int index(Position cell, int width) {
        return cell.y() * width + cell.x();
    }
}
