package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.dungeon.DamageResult;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * L'unico modo di infliggere danno fuori dal piano. Se il bersaglio muore e la fonte è il player,
 * il player guadagna l'esperienza del nemico: vale per un attacco come per un veleno o un effetto.
 */
public final class DamageRules {

    private final LevelingRules leveling;

    public DamageRules(LevelingRules leveling) {
        this.leveling = leveling;
    }

    /**
     * Infligge il danno e restituisce i suoi eventi, seguiti da quelli dell'esperienza se c'è.
     *
     * @param source chi infligge il danno; vuoto per un danno senza autore, come una trappola, che
     *     non dà esperienza
     */
    public List<GameEvent> apply(
            Floor floor, Optional<EntityId> source, EntityId target, int amount) {
        DamageResult result = floor.damage(target, amount);
        List<GameEvent> events = new ArrayList<>(result.events());
        boolean byPlayer = source.map(floor.playerId()::equals).orElse(false);
        if (result.killed() && byPlayer) {
            int reward = floor.enemy(target).map(Enemy::experienceReward).orElse(0);
            events.addAll(leveling.grantExperience(floor, reward));
        }
        return List.copyOf(events);
    }
}
