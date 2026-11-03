package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.Optional;

/**
 * La strategia di chi colpisce da lontano. Se il player è in linea retta, entro la portata e senza
 * niente in mezzo, tira. Se il player è accanto, si allontana di un passo; combatte da vicino se
 * non può, o se si è appena allontanato: chi lo insegue subito riesce a colpirlo. Altrimenti si
 * avvicina con la strategia di inseguimento, finché non lo ha in linea.
 */
public final class KiteBehavior implements EnemyBehavior {

    private final EnemyBehavior approach;

    /**
     * @param approach come avvicinarsi quando il player non è a tiro
     */
    public KiteBehavior(EnemyBehavior approach) {
        this.approach = approach;
    }

    @Override
    public EnemyIntent decide(Enemy enemy, FloorView floor, RandomSource random) {
        if (!floor.player().isAlive()) {
            return new WaitIntent();
        }
        Position at = floor.positionOf(enemy.id());
        Position player = floor.positionOf(floor.playerId());
        int distance = at.distanceTo(player);
        if (distance == 1) {
            for (Direction direction : Direction.values()) {
                Position away = at.translate(direction);
                if (!enemy.hasJustRetreated()
                        && floor.isFree(away)
                        && away.distanceTo(player) > distance) {
                    return new RetreatIntent(direction);
                }
            }
            return new StepIntent(at.directionTo(player).orElseThrow(IllegalStateException::new));
        }
        Optional<Direction> aim = aim(floor, at, player, enemy.range());
        if (aim.isPresent()) {
            return new RangedAttackIntent(aim.get());
        }
        return approach.decide(enemy, floor, random);
    }

    /**
     * La direzione in cui tirare da {@code from} a {@code to}: in linea retta, entro {@code range}
     * celle, con tutte le celle in mezzo attraversabili e senza attori.
     *
     * @return vuoto se il bersaglio non è a tiro
     */
    public static Optional<Direction> aim(FloorView floor, Position from, Position to, int range) {
        int distance = from.distanceTo(to);
        if (distance == 0 || distance > range || (from.x() != to.x() && from.y() != to.y())) {
            return Optional.empty();
        }
        for (Direction direction : Direction.values()) {
            Position cell = from;
            for (int step = 1; step <= distance; step++) {
                cell = cell.translate(direction);
                if (cell.equals(to)) {
                    return Optional.of(direction);
                }
                if (!floor.isFree(cell)) {
                    break;
                }
            }
        }
        return Optional.empty();
    }
}
