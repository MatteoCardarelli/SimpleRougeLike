package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;

/** Il player, sempre visibile a sé stesso. */
public record PlayerView(
        EntityId id,
        Position position,
        int health,
        int maxHealth,
        int level,
        int experience,
        int experienceToNextLevel) {}
