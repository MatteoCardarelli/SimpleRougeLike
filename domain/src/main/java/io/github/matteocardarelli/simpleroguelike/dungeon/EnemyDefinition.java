package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;

/**
 * Un tipo di nemico, condiviso da tutti i nemici di quel tipo.
 *
 * @param experienceReward l'esperienza che il player guadagna uccidendolo
 * @param aggroRange la distanza entro cui si accorge del player
 * @param behavior la strategia con cui decide cosa fare
 * @param spawnWeight quanto spesso compare sul piano rispetto agli altri tipi; 0 vuol dire mai
 * @param range quante celle in linea retta raggiunge il suo colpo a distanza; 0 se combatte solo da
 *     vicino
 */
public record EnemyDefinition(
        Kind kind,
        Stats stats,
        int experienceReward,
        int aggroRange,
        BehaviorKey behavior,
        int spawnWeight,
        int range) {

    public EnemyDefinition {
        if (stats.maxHealth() <= 0) {
            throw new IllegalArgumentException("Un nemico deve avere salute: " + stats);
        }
        if (experienceReward < 0 || aggroRange < 0 || spawnWeight < 0 || range < 0) {
            throw new IllegalArgumentException(
                    "Valori negativi: experienceReward="
                            + experienceReward
                            + ", aggroRange="
                            + aggroRange
                            + ", spawnWeight="
                            + spawnWeight
                            + ", range="
                            + range);
        }
    }

    /** Un nemico che combatte solo da vicino. */
    public EnemyDefinition(
            Kind kind,
            Stats stats,
            int experienceReward,
            int aggroRange,
            BehaviorKey behavior,
            int spawnWeight) {
        this(kind, stats, experienceReward, aggroRange, behavior, spawnWeight, 0);
    }
}
