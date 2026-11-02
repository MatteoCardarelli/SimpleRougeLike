package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.port.Pathfinder;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * La strategia di un branco: accerchiare il player. I membri del branco che lo inseguono, cioè i
 * nemici vivi con la stessa strategia entro la loro distanza di inseguimento, si dividono le celle
 * libere intorno al player: ognuno, in ordine di id, prende quella più vicina a sé tra quelle che i
 * membri prima di lui non hanno preso. Chi è già accanto al player attacca. Chi resta senza cella
 * va verso il player come un inseguitore qualunque.
 *
 * <p>Non tiene stato: le scelte dei compagni le ricalcola, nello stesso ordine, a ogni decisione.
 */
public final class SurroundBehavior implements EnemyBehavior {

    private final Pathfinder pathfinder;

    public SurroundBehavior(Pathfinder pathfinder) {
        this.pathfinder = pathfinder;
    }

    @Override
    public EnemyIntent decide(Enemy enemy, FloorView floor, RandomSource random) {
        if (!floor.player().isAlive()) {
            return new WaitIntent();
        }
        Position player = floor.positionOf(floor.playerId());
        Position at = floor.positionOf(enemy.id());
        Optional<Direction> attack = at.directionTo(player);
        if (attack.isPresent()) {
            return new StepIntent(attack.get());
        }
        Position target = targetOf(enemy, floor, player).orElse(player);
        if (target.equals(at)) {
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
                .nextStep(floor.movementGrid(), blocked, at, target)
                .<EnemyIntent>map(StepIntent::new)
                .orElseGet(WaitIntent::new);
    }

    /** La cella intorno al player che spetta a {@code enemy}, se ne resta una. */
    private static Optional<Position> targetOf(Enemy enemy, FloorView floor, Position player) {
        List<Position> around = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            Position cell = player.translate(direction);
            if (floor.isWalkable(cell)) {
                around.add(cell);
            }
        }
        for (Enemy member : floor.enemies()) {
            if (!member.behavior().equals(enemy.behavior())) {
                continue;
            }
            Position at = floor.positionOf(member.id());
            if (at.distanceTo(player) > member.aggroRange()) {
                continue;
            }
            if (at.isAdjacentTo(player)) {
                around.remove(at);
                continue;
            }
            Position best = null;
            for (Position cell : around) {
                if (best == null || cell.distanceTo(at) < best.distanceTo(at)) {
                    best = cell;
                }
            }
            if (member.id().equals(enemy.id())) {
                return Optional.ofNullable(best);
            }
            if (best != null) {
                around.remove(best);
            }
        }
        return Optional.empty();
    }
}
