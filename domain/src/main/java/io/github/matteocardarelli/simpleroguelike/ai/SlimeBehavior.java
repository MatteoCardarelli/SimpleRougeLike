package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.List;
import java.util.Optional;

/**
 * La strategia dello slime gigante, che combatte solo saltando, in un ciclo di quattro turni che
 * ricomincia sempre dal primo: fermo, si carica, si alza in aria con l'ombra sul player, atterra.
 *
 * <p>Il ciclo va avanti solo con il player nella stanza del boss; fuori, lo slime aspetta senza
 * cambiare fase. Su un piano senza stanza (un salvataggio di prima) conta la distanza di
 * inseguimento.
 *
 * <p>L'ombra copre il player su una cella del bordo, mai su una interna: un passo lo porta fuori,
 * se un muro non lo impedisce. Tra le ombre che stanno tutte nella stanza vince quella che lascia
 * al player meno passi liberi per uscire, poi quella con il centro più vicino al centro della
 * stanza, poi la prima in ordine di riga e colonna. Così in mezzo alla stanza si scappa sempre,
 * mentre con le spalle al muro o in un angolo è facile venire schiacciati.
 */
public final class SlimeBehavior implements EnemyBehavior {

    @Override
    public EnemyIntent decide(Enemy enemy, FloorView floor, RandomSource random) {
        if (!floor.player().isAlive()) {
            return new WaitIntent();
        }
        if (enemy.isAirborne()) {
            return new LandIntent();
        }
        Position player = floor.positionOf(floor.playerId());
        if (!isInReach(enemy, floor, player)) {
            return new WaitIntent();
        }
        return switch (enemy.jumpPhase()) {
            case RESTING -> new RestIntent();
            case READY -> new ChargeIntent();
            case CHARGING ->
                    shadow(enemy, floor, player)
                            .<EnemyIntent>map(JumpIntent::new)
                            .orElseGet(WaitIntent::new);
            case AIRBORNE -> new LandIntent();
        };
    }

    /** Vero se il player è nella stanza del boss o, senza stanza, entro la distanza di caccia. */
    private static boolean isInReach(Enemy enemy, FloorView floor, Position player) {
        CellSet room = floor.bossRoom();
        if (!room.isEmpty()) {
            return room.contains(player);
        }
        for (Position cell : floor.cellsOf(enemy.id())) {
            if (cell.distanceTo(player) <= enemy.aggroRange()) {
                return true;
            }
        }
        return false;
    }

    /** La cella di riferimento dell'ombra, se ce n'è una che mette il player sul bordo. */
    static Optional<Position> shadow(Enemy enemy, FloorView floor, Position player) {
        CellSet room = floor.bossRoom();
        int size = enemy.footprint().size();
        // I centri si confrontano raddoppiati, per restare sugli interi.
        Optional<Position> roomCenter = doubledCenter(room);
        Position best = null;
        int bestExits = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        for (Position offset : enemy.footprint().offsets()) {
            Position anchor = new Position(player.x() - offset.x(), player.y() - offset.y());
            List<Position> cells = enemy.footprint().at(anchor);
            if (!floor.canLand(enemy.id(), anchor) || !inside(cells, room)) {
                continue;
            }
            int border = 0;
            int exits = 0;
            for (Direction direction : Direction.values()) {
                Position next = player.translate(direction);
                if (cells.contains(next)) {
                    continue;
                }
                border++;
                if (isOpen(enemy, floor, next)) {
                    exits++;
                }
            }
            if (border == 0) {
                continue;
            }
            int distance =
                    roomCenter
                            .map(
                                    c ->
                                            Math.abs(2 * anchor.x() + size - 1 - c.x())
                                                    + Math.abs(2 * anchor.y() + size - 1 - c.y()))
                            .orElse(0);
            if (best == null
                    || exits < bestExits
                    || (exits == bestExits && distance < bestDistance)
                    || (exits == bestExits && distance == bestDistance && before(anchor, best))) {
                best = anchor;
                bestExits = exits;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Vero se il player può fare un passo nella cella: lo slime stesso sarà in aria. */
    private static boolean isOpen(Enemy enemy, FloorView floor, Position cell) {
        if (!floor.isWalkable(cell)) {
            return false;
        }
        Optional<Actor> occupant = floor.actorAt(cell);
        return !occupant.isPresent() || occupant.get().id().equals(enemy.id());
    }

    private static boolean inside(List<Position> cells, CellSet room) {
        if (room.isEmpty()) {
            return true;
        }
        for (Position cell : cells) {
            if (!room.contains(cell)) {
                return false;
            }
        }
        return true;
    }

    /** Il centro del rettangolo che contiene la stanza, con le coordinate raddoppiate. */
    private static Optional<Position> doubledCenter(CellSet room) {
        if (room.isEmpty()) {
            return Optional.empty();
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Position cell : room.positions()) {
            minX = Math.min(minX, cell.x());
            minY = Math.min(minY, cell.y());
            maxX = Math.max(maxX, cell.x());
            maxY = Math.max(maxY, cell.y());
        }
        return Optional.of(new Position(minX + maxX, minY + maxY));
    }

    private static boolean before(Position a, Position b) {
        return a.y() < b.y() || (a.y() == b.y() && a.x() < b.x());
    }
}
