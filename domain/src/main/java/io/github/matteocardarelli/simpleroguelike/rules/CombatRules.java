package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Un attacco in mischia: il difensore perde tanta salute quanto l'attacco dell'attaccante. */
public final class CombatRules {

    private final DamageRules damage;

    public CombatRules(DamageRules damage) {
        this.damage = damage;
    }

    /**
     * Risolve l'attacco e restituisce {@code Attacked}, seguito dagli eventi del danno.
     *
     * @throws IllegalStateException se uno dei due è morto o non sono adiacenti
     */
    public List<GameEvent> resolveAttack(Floor floor, EntityId attacker, EntityId defender) {
        Actor attackerActor = requireAlive(floor, attacker);
        requireAlive(floor, defender);
        Position attackerAt = floor.positionOf(attacker);
        Position defenderAt = floor.positionOf(defender);
        if (!attackerAt.isAdjacentTo(defenderAt)) {
            throw new IllegalStateException(
                    attacker + " in " + attackerAt + " non è adiacente a " + defenderAt);
        }
        List<GameEvent> events = new ArrayList<>();
        events.add(new Attacked(attacker, defender, attackerAt, defenderAt));
        events.addAll(
                damage.apply(
                        floor, Optional.of(attacker), defender, attackerActor.stats().attack()));
        return List.copyOf(events);
    }

    private static Actor requireAlive(Floor floor, EntityId id) {
        return floor.actor(id)
                .filter(Actor::isAlive)
                .orElseThrow(() -> new IllegalStateException(id + " non è un attore vivo"));
    }
}
