package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un passo in una direzione, la stessa regola per il player e per i nemici: contro un attore ostile
 * è un attacco, in una cella libera è uno spostamento, altrimenti non succede niente. Il player,
 * quando si sposta, raccoglie l'oggetto che trova a terra; contro un oggetto fisso lo usa: apre una
 * porta o uno scrigno, tira una leva; contro il mercante paga la merce presa, e uscendo dal negozio
 * senza pagare lo fa arrabbiare.
 */
public final class ActionRules {

    private final CombatRules combat;

    public ActionRules(CombatRules combat) {
        this.combat = combat;
    }

    /**
     * Fa fare un passo all'attore.
     *
     * @param random il flusso di gameplay, per un eventuale attacco
     * @throws IllegalStateException se l'attore è morto
     */
    public ActionResult step(
            Floor floor, EntityId actorId, Direction direction, RandomSource random) {
        Actor actor =
                floor.actor(actorId)
                        .filter(Actor::isAlive)
                        .orElseThrow(
                                () -> new IllegalStateException(actorId + " non è un attore vivo"));
        Position to = floor.positionOf(actorId).translate(direction);

        // Un attore grande urta chiunque stia in una delle celle della sua impronta spostata.
        Optional<Actor> occupant = Optional.empty();
        for (Position cell : actor.footprint().at(to)) {
            Optional<Actor> other = floor.actorAt(cell);
            if (other.isPresent() && !other.get().id().equals(actorId)) {
                occupant = other;
                break;
            }
        }
        if (occupant.isPresent()) {
            if (!actor.faction().isHostileTo(occupant.get().faction())) {
                boolean merchant =
                        floor.enemy(occupant.get().id()).map(Enemy::isMerchant).orElse(false);
                if (actorId.equals(floor.playerId()) && merchant) {
                    return ActionResult.performed(List.of(floor.trade(actorId)));
                }
                return ActionResult.blocked();
            }
            return ActionResult.performed(
                    combat.resolveAttack(floor, actorId, occupant.get().id(), random));
        }
        if (!floor.canOccupy(actorId, to)) {
            if (actorId.equals(floor.playerId()) && floor.fixtureAt(to).isPresent()) {
                List<GameEvent> used = floor.interact(actorId, to);
                return used.isEmpty() ? ActionResult.blocked() : ActionResult.performed(used);
            }
            return ActionResult.blocked();
        }
        List<GameEvent> events = new ArrayList<>();
        events.add(floor.move(actorId, direction));
        if (actorId.equals(floor.playerId())) {
            floor.pickUp(actorId).ifPresent(events::add);
            floor.checkTheft().ifPresent(events::add);
        }
        return ActionResult.performed(events);
    }
}
