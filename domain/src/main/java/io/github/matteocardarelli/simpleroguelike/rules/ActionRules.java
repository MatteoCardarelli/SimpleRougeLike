package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un passo in una direzione, la stessa regola per il player e per i nemici: contro un attore ostile
 * è un attacco, in una cella libera è uno spostamento, altrimenti non succede niente. Il player,
 * quando si sposta, raccoglie l'oggetto che trova a terra.
 */
public final class ActionRules {

    private final CombatRules combat;

    public ActionRules(CombatRules combat) {
        this.combat = combat;
    }

    /**
     * Fa fare un passo all'attore.
     *
     * @throws IllegalStateException se l'attore è morto
     */
    public ActionResult step(Floor floor, EntityId actorId, Direction direction) {
        Actor actor =
                floor.actor(actorId)
                        .filter(Actor::isAlive)
                        .orElseThrow(
                                () -> new IllegalStateException(actorId + " non è un attore vivo"));
        Position to = floor.positionOf(actorId).translate(direction);

        Optional<Actor> occupant = floor.actorAt(to);
        if (occupant.isPresent()) {
            if (!actor.faction().isHostileTo(occupant.get().faction())) {
                return ActionResult.blocked();
            }
            return ActionResult.performed(
                    combat.resolveAttack(floor, actorId, occupant.get().id()));
        }
        if (!floor.isFree(to)) {
            return ActionResult.blocked();
        }
        List<GameEvent> events = new ArrayList<>();
        events.add(floor.move(actorId, direction));
        if (actorId.equals(floor.playerId())) {
            floor.pickUp(actorId).ifPresent(events::add);
        }
        return ActionResult.performed(events);
    }
}
