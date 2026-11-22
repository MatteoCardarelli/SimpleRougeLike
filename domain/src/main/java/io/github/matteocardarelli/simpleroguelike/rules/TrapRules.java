package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.dungeon.FixtureType;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.TrapTriggered;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Le trappole nascoste. Scattano quando il player entra nella loro cella, una volta sola: le punte
 * infliggono {@value #SPIKES_DAMAGE} danni, il dardo {@value #DART_DAMAGE} più il veleno. Il danno
 * passa da {@link DamageRules}, senza autore, quindi chi muore in una trappola non dà esperienza. I
 * nemici le ignorano: ci passano sopra e non succede niente.
 *
 * <p>Il player scopre una trappola nascosta nelle quattro celle accanto a lui, una volta su {@value
 * #DISCOVERY_ONE_IN} per turno, col flusso di gameplay.
 */
public final class TrapRules {

    public static final int SPIKES_DAMAGE = 8;
    public static final int DART_DAMAGE = 3;

    /** Ogni turno, ogni trappola nascosta accanto al player si scopre una volta su tanti. */
    public static final int DISCOVERY_ONE_IN = 3;

    private final DamageRules damage;

    public TrapRules(DamageRules damage) {
        this.damage = damage;
    }

    /**
     * Fa scattare la trappola nella cella in cui il player è appena entrato, se c'è e non è già
     * scattata.
     *
     * @return gli eventi: {@code TrapTriggered}, poi il danno e l'eventuale veleno; vuoto se non
     *     scatta niente
     */
    public List<GameEvent> trigger(Floor floor, Position at) {
        Optional<FixtureType> sprung = floor.springTrap(at);
        if (!sprung.isPresent()) {
            return List.of();
        }
        EntityId player = floor.playerId();
        FixtureType type = sprung.get();
        int amount = type == FixtureType.TRAP_SPIKES ? SPIKES_DAMAGE : DART_DAMAGE;
        List<GameEvent> events = new ArrayList<>();
        events.add(new TrapTriggered(type.kind(), at, amount));
        events.addAll(damage.apply(floor, Optional.empty(), player, amount));
        if (type == FixtureType.TRAP_DART) {
            floor.poison(player).ifPresent(events::add);
        }
        return List.copyOf(events);
    }

    /**
     * Prova a scoprire ogni trappola nascosta nelle quattro celle accanto al player: un tiro per
     * trappola, in ordine di direzione.
     *
     * @param random il flusso di gameplay
     */
    public List<GameEvent> discover(Floor floor, RandomSource random) {
        if (!floor.player().isAlive()) {
            return List.of();
        }
        Position at = floor.positionOf(floor.playerId());
        List<GameEvent> events = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            Position next = at.translate(direction);
            boolean hidden =
                    floor.fixtureAt(next).map(f -> f.type().isTrap() && f.isHidden()).orElse(false);
            if (hidden && random.nextInt(DISCOVERY_ONE_IN) == 0) {
                floor.discoverTrap(next).ifPresent(events::add);
            }
        }
        return List.copyOf(events);
    }
}
