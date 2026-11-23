package io.github.matteocardarelli.simpleroguelike.effect;

import io.github.matteocardarelli.simpleroguelike.dungeon.EffectContext;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemEffect;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.List;

/**
 * Toglie il veleno a chi usa l'oggetto. Se non è avvelenato non succede niente e l'oggetto non si
 * consuma.
 */
public record CureEffect() implements ItemEffect {

    @Override
    public List<GameEvent> apply(EffectContext context) {
        return context.floor()
                .curePoison(context.user())
                .<List<GameEvent>>map(List::of)
                .orElse(List.of());
    }

    @Override
    public boolean spentBy(List<GameEvent> events) {
        return !events.isEmpty();
    }
}
