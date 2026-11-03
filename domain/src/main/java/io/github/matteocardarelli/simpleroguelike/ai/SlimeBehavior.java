package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * La strategia dello slime gigante, "salta e schiaccia". Se ha annunciato un salto, atterra; dopo
 * l'atterraggio riposa un turno. Se il player è a portata, annuncia un salto che lo copre con la
 * sua impronta, scegliendo la cella di riferimento che lo mette più al centro; se il player gli è
 * accanto, una volta su due lo colpisce da vicino. Lontano dal player, aspetta.
 */
public final class SlimeBehavior implements EnemyBehavior {

    @Override
    public EnemyIntent decide(Enemy enemy, FloorView floor, RandomSource random) {
        if (!floor.player().isAlive()) {
            return new WaitIntent();
        }
        if (enemy.pendingJump().isPresent()) {
            return new LandIntent();
        }
        if (enemy.isTired()) {
            return new RestIntent();
        }
        Position player = floor.positionOf(floor.playerId());
        List<Position> cells = floor.cellsOf(enemy.id());
        int distance = Integer.MAX_VALUE;
        for (Position cell : cells) {
            distance = Math.min(distance, cell.distanceTo(player));
        }
        if (distance > enemy.aggroRange()) {
            return new WaitIntent();
        }
        Optional<StepIntent> melee = melee(cells, player);
        if (melee.isPresent() && random.nextInt(2) == 0) {
            return melee.get();
        }
        Position here = floor.positionOf(enemy.id());
        for (Position offset : centered(enemy)) {
            Position anchor = new Position(player.x() - offset.x(), player.y() - offset.y());
            if (!anchor.equals(here) && floor.canLand(enemy.id(), anchor)) {
                return new JumpIntent(anchor);
            }
        }
        return melee.<EnemyIntent>map(step -> step).orElseGet(WaitIntent::new);
    }

    /** Il passo che colpisce il player, se una cella dello slime gli è accanto. */
    private static Optional<StepIntent> melee(List<Position> cells, Position player) {
        for (Position cell : cells) {
            if (cell.isAdjacentTo(player)) {
                return cell.directionTo(player).map(StepIntent::new);
            }
        }
        return Optional.empty();
    }

    /**
     * Gli spostamenti dell'impronta, dal più vicino al centro: il player finisce sotto il centro.
     */
    private static List<Position> centered(Enemy enemy) {
        List<Position> offsets = new ArrayList<>(enemy.footprint().offsets());
        double center = (enemy.footprint().size() - 1) / 2.0;
        offsets.sort(
                Comparator.comparingDouble(
                        (Position p) -> Math.abs(p.x() - center) + Math.abs(p.y() - center)));
        return offsets;
    }
}
