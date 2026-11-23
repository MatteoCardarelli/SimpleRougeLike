package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Il veleno a fine turno: ogni attore vivo avvelenato subisce {@value #DAMAGE} danni, che passano
 * da {@link DamageRules} come ogni altro danno, e perde un turno di veleno; a zero il veleno
 * finisce. Il veleno non ha autore, quindi chi muore avvelenato non dà esperienza.
 */
public final class PoisonRules {

    public static final int DAMAGE = 2;

    private final DamageRules damage;

    public PoisonRules(DamageRules damage) {
        this.damage = damage;
    }

    /** Applica un turno di veleno a tutti gli avvelenati e restituisce i suoi eventi. */
    public List<GameEvent> tick(Floor floor) {
        List<GameEvent> events = new ArrayList<>();
        for (EntityId id : floor.poisonedActors()) {
            events.addAll(damage.apply(floor, Optional.empty(), id, DAMAGE));
            floor.wearPoison(id).ifPresent(events::add);
        }
        return List.copyOf(events);
    }
}
