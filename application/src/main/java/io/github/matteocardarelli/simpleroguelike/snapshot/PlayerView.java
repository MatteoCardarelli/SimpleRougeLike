package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;

/** Il player, sempre visibile a sé stesso, con le statistiche di combattimento da mostrare. */
public record PlayerView(
        EntityId id,
        Position position,
        int health,
        int maxHealth,
        int level,
        int experience,
        int experienceToNextLevel,
        int attack,
        int defense,
        int accuracy,
        int evasion,
        int coins,
        int keys,
        int bossKeys) {

    /** Un player senza chiavi. */
    public PlayerView(
            EntityId id,
            Position position,
            int health,
            int maxHealth,
            int level,
            int experience,
            int experienceToNextLevel,
            int attack,
            int defense,
            int accuracy,
            int evasion,
            int coins) {
        this(
                id,
                position,
                health,
                maxHealth,
                level,
                experience,
                experienceToNextLevel,
                attack,
                defense,
                accuracy,
                evasion,
                coins,
                0,
                0);
    }

    /** Un player senza monete né chiavi. */
    public PlayerView(
            EntityId id,
            Position position,
            int health,
            int maxHealth,
            int level,
            int experience,
            int experienceToNextLevel,
            int attack,
            int defense,
            int accuracy,
            int evasion) {
        this(
                id,
                position,
                health,
                maxHealth,
                level,
                experience,
                experienceToNextLevel,
                attack,
                defense,
                accuracy,
                evasion,
                0);
    }
}
