package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.port.Pathfinder;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * La strategia di un branco: un gruppetto che decide come una mente sola. La mente riguarda solo i
 * membri vivi del branco di chi decide (stesso id del branco), non gli altri nemici dello stesso
 * tipo; un nemico senza branco è un branco da solo.
 *
 * <p>Se il player è entro la distanza di inseguimento di un membro qualunque, tutto il branco lo
 * caccia, anche i membri più lontani: in ordine di id, ognuno prende la cella libera intorno al
 * player più vicina a sé tra quelle che i membri prima di lui non hanno preso. Chi è già accanto al
 * player attacca; chi resta senza cella va verso il player. Altrimenti il branco resta unito: chi è
 * a più di {@value #REGROUP_DISTANCE} passi dal capo, il membro con l'id più basso, gli va
 * incontro, e gli altri aspettano.
 *
 * <p>Non tiene stato: le scelte dei compagni le ricalcola, nello stesso ordine, a ogni decisione.
 */
public final class SurroundBehavior implements EnemyBehavior {

    /** Oltre quanti passi dal capo un membro, a riposo, torna verso di lui. */
    public static final int REGROUP_DISTANCE = 3;

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
        List<Enemy> pack = packOf(enemy, floor);
        if (isHunting(pack, floor, player)) {
            Position target = targetOf(enemy, pack, floor, player).orElse(player);
            return target.equals(at) ? new WaitIntent() : stepTowards(enemy, floor, at, target);
        }
        Enemy leader = pack.get(0);
        Position leaderAt = floor.positionOf(leader.id());
        if (leader.id().equals(enemy.id()) || at.distanceTo(leaderAt) <= REGROUP_DISTANCE) {
            return new WaitIntent();
        }
        return stepTowards(enemy, floor, at, leaderAt);
    }

    /** I membri vivi del branco di {@code enemy}, in ordine di id; solo lui se va da solo. */
    private static List<Enemy> packOf(Enemy enemy, FloorView floor) {
        Optional<EntityId> pack = enemy.pack();
        if (!pack.isPresent()) {
            return List.of(enemy);
        }
        List<Enemy> members = new ArrayList<>();
        for (Enemy other : floor.enemies()) {
            if (other.pack().equals(pack)) {
                members.add(other);
            }
        }
        return members;
    }

    /** Vero se almeno un membro ha il player entro la sua distanza di inseguimento. */
    private static boolean isHunting(List<Enemy> pack, FloorView floor, Position player) {
        for (Enemy member : pack) {
            if (floor.positionOf(member.id()).distanceTo(player) <= member.aggroRange()) {
                return true;
            }
        }
        return false;
    }

    /** Il prossimo passo verso {@code target}, aggirando gli altri nemici. */
    private EnemyIntent stepTowards(Enemy enemy, FloorView floor, Position at, Position target) {
        List<Position> others = new ArrayList<>();
        for (Enemy other : floor.enemies()) {
            Position where = floor.positionOf(other.id());
            if (!other.id().equals(enemy.id()) && !where.equals(target)) {
                others.add(where);
            }
        }
        CellSet blocked = CellSet.of(floor.terrain().width(), floor.terrain().height(), others);
        return pathfinder
                .nextStep(floor.movementGrid(), blocked, at, target)
                .<EnemyIntent>map(StepIntent::new)
                .orElseGet(WaitIntent::new);
    }

    /** La cella intorno al player che spetta a {@code enemy}, se ne resta una. */
    private static Optional<Position> targetOf(
            Enemy enemy, List<Enemy> pack, FloorView floor, Position player) {
        List<Position> around = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            Position cell = player.translate(direction);
            if (floor.isWalkable(cell)) {
                around.add(cell);
            }
        }
        for (Enemy member : pack) {
            Position at = floor.positionOf(member.id());
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
