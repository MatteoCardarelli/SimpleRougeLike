package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Landed;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * L'atterraggio di un salto. Chi era sotto l'impronta viene spinto fuori e colpito, senza tiro per
 * colpire: chi non si sposta quando vede l'annuncio viene schiacciato. Il danno è il doppio
 * dell'attacco del nemico meno la difesa, almeno 1.
 */
public final class JumpRules {

    private final DamageRules damage;

    public JumpRules(DamageRules damage) {
        this.damage = damage;
    }

    /**
     * Fa atterrare il nemico.
     *
     * @return gli eventi dell'atterraggio, seguiti da quelli del danno; vuoto se il salto è andato
     *     a vuoto
     */
    public List<GameEvent> land(Floor floor, EntityId jumper) {
        Actor actor =
                floor.actor(jumper)
                        .filter(Actor::isAlive)
                        .orElseThrow(() -> new IllegalStateException(jumper + " non è vivo"));
        Optional<Position> target = floor.enemy(jumper).flatMap(e -> e.pendingJump());
        Position playerAt = floor.positionOf(floor.playerId());
        boolean underneath =
                target.isPresent()
                        && floor.player().isAlive()
                        && actor.footprint().at(target.get()).contains(playerAt);
        List<GameEvent> events = new ArrayList<>(floor.land(jumper));
        boolean landed = events.stream().anyMatch(Landed.class::isInstance);
        if (landed && underneath) {
            int amount = Math.max(1, 2 * actor.stats().attack() - floor.player().stats().defense());
            events.addAll(damage.apply(floor, Optional.of(jumper), floor.playerId(), amount));
        }
        return List.copyOf(events);
    }
}
