package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Missed;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un attacco in mischia. Va a segno con una probabilità, in percentuale, di {@value
 * #BASE_HIT_CHANCE} più la precisione dell'attaccante meno la schivata del difensore, sempre tra
 * {@value #MIN_HIT_CHANCE} e {@value #MAX_HIT_CHANCE}: nessun colpo è certo o impossibile. Il danno
 * è l'attacco meno la difesa, almeno 1. Il caso arriva dal flusso di gameplay: la stessa partita si
 * rigioca identica.
 */
public final class CombatRules {

    public static final int BASE_HIT_CHANCE = 80;
    public static final int MIN_HIT_CHANCE = 5;
    public static final int MAX_HIT_CHANCE = 95;

    private final DamageRules damage;

    public CombatRules(DamageRules damage) {
        this.damage = damage;
    }

    /** La probabilità, in percentuale, che un colpo di {@code attacker} vada a segno. */
    public static int hitChance(Stats attacker, Stats defender) {
        int chance = BASE_HIT_CHANCE + attacker.accuracy() - defender.evasion();
        return Math.max(MIN_HIT_CHANCE, Math.min(MAX_HIT_CHANCE, chance));
    }

    /** Il danno di un colpo a segno: l'attacco meno la difesa, almeno 1. */
    public static int damage(Stats attacker, Stats defender) {
        return Math.max(1, attacker.attack() - defender.defense());
    }

    /**
     * Risolve l'attacco e restituisce {@code Attacked}, seguito dagli eventi del danno oppure da
     * {@code Missed}.
     *
     * @param random il flusso di gameplay, da cui si tira un numero solo
     * @throws IllegalStateException se uno dei due è morto o non sono adiacenti
     */
    public List<GameEvent> resolveAttack(
            Floor floor, EntityId attacker, EntityId defender, RandomSource random) {
        Actor attackerActor = requireAlive(floor, attacker);
        Actor defenderActor = requireAlive(floor, defender);
        // Con un attore grande conta la cella toccata, non la sua cella di riferimento.
        Position defenderAt =
                floor.contactCell(attacker, defender)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                attacker + " non è adiacente a " + defender));
        Position attackerAt = floor.positionOf(attacker);
        for (Position cell : floor.cellsOf(attacker)) {
            if (cell.isAdjacentTo(defenderAt)) {
                attackerAt = cell;
                break;
            }
        }
        List<GameEvent> events = new ArrayList<>();
        events.add(new Attacked(attacker, defender, attackerAt, defenderAt));
        Stats offense = attackerActor.stats();
        Stats defense = defenderActor.stats();
        if (random.nextInt(100) < hitChance(offense, defense)) {
            events.addAll(
                    damage.apply(floor, Optional.of(attacker), defender, damage(offense, defense)));
        } else {
            events.add(new Missed(attacker, defender, attackerAt, defenderAt));
        }
        return List.copyOf(events);
    }

    private static Actor requireAlive(Floor floor, EntityId id) {
        return floor.actor(id)
                .filter(Actor::isAlive)
                .orElseThrow(() -> new IllegalStateException(id + " non è un attore vivo"));
    }
}
