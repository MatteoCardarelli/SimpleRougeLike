package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.Fired;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Missed;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un colpo a distanza, la stessa regola per un coltello lanciato dal player e per una freccia di un
 * nemico. Il proiettile va in linea retta nella direzione scelta, al massimo per {@code range}
 * celle, e si ferma sul primo attore o prima di un muro. Contro un ostile va a segno con la stessa
 * probabilità di un attacco in mischia, e il danno è {@code power} meno la difesa, almeno 1. Contro
 * un alleato si ferma senza fare niente.
 */
public final class RangedRules {

    /** Il proiettile dei nemici che colpiscono da lontano, per i messaggi e le animazioni. */
    public static final Kind ARROW = Kind.of("arrow");

    private final DamageRules damage;

    public RangedRules(DamageRules damage) {
        this.damage = damage;
    }

    /**
     * Risolve il colpo e restituisce {@code Fired}, seguito dagli eventi del danno oppure da {@code
     * Missed} se il proiettile ha raggiunto un ostile.
     *
     * @param projectile il tipo del proiettile, per i messaggi e le animazioni
     * @param power il danno del colpo, prima della difesa
     * @param random il flusso di gameplay, da cui si tira un numero solo se c'è un bersaglio
     * @throws IllegalStateException se chi colpisce è morto
     * @throws IllegalArgumentException se portata o danno non sono positivi
     */
    public List<GameEvent> shoot(
            Floor floor,
            EntityId shooter,
            Direction direction,
            int range,
            Kind projectile,
            int power,
            RandomSource random) {
        if (range <= 0 || power <= 0) {
            throw new IllegalArgumentException("Portata e danno: " + range + ", " + power);
        }
        Actor shooterActor =
                floor.actor(shooter)
                        .filter(Actor::isAlive)
                        .orElseThrow(
                                () -> new IllegalStateException(shooter + " non è un attore vivo"));
        Position from = floor.positionOf(shooter);
        List<Position> path = new ArrayList<>();
        Optional<Actor> target = Optional.empty();
        Position cell = from;
        for (int step = 0; step < range; step++) {
            cell = cell.translate(direction);
            if (!floor.isWalkable(cell)) {
                break;
            }
            path.add(cell);
            target = floor.actorAt(cell);
            if (target.isPresent()) {
                break;
            }
        }
        List<GameEvent> events = new ArrayList<>();
        events.add(new Fired(shooter, shooterActor.kind(), projectile, from, path));
        if (target.isPresent() && shooterActor.faction().isHostileTo(target.get().faction())) {
            Actor hit = target.get();
            if (random.nextInt(100) < CombatRules.hitChance(shooterActor.stats(), hit.stats())) {
                int amount = Math.max(1, power - hit.stats().defense());
                events.addAll(damage.apply(floor, Optional.of(shooter), hit.id(), amount));
            } else {
                events.add(new Missed(shooter, hit.id(), from, floor.positionOf(hit.id())));
            }
        }
        return List.copyOf(events);
    }
}
