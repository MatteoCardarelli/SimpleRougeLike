package io.github.matteocardarelli.simpleroguelike.effect;

import io.github.matteocardarelli.simpleroguelike.dungeon.EffectContext;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemEffect;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.List;

/** Cura chi usa l'oggetto di {@code amount}, senza superare la sua salute massima. */
public record HealEffect(int amount) implements ItemEffect {

    public HealEffect {
        if (amount <= 0) {
            throw new IllegalArgumentException("Una cura deve essere positiva: " + amount);
        }
    }

    @Override
    public List<GameEvent> apply(EffectContext context) {
        return context.floor()
                .heal(context.user(), amount)
                .<List<GameEvent>>map(List::of)
                .orElse(List.of());
    }
}
