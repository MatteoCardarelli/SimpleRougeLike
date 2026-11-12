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
 * @param packSize quanti nemici di questo tipo nascono insieme, vicini: 1 per chi va da solo
 * @param dropChance la probabilità, in percentuale, che alla morte lasci cadere un oggetto
 * @param footprint le celle che occupa
 * @param boss vero per il boss dell'ultimo piano: sconfiggerlo vince la partita
 * @param merchant vero per un mercante: neutrale finché il player non lo deruba
 * @param speed quante azioni fa in un turno: 1 per quasi tutti
 * @param arena per un boss, la forma della sua stanza, un quadrato o un cerchio più grande della
 *     sua impronta; per gli altri non conta
 */
public record EnemyDefinition(
        Kind kind,
        Stats stats,
        int experienceReward,
        int aggroRange,
        BehaviorKey behavior,
        int spawnWeight,
        int range,
        int packSize,
        int dropChance,
        Footprint footprint,
        boolean boss,
        boolean merchant,
        int speed,
        Footprint arena) {

    public EnemyDefinition {
        if (stats.maxHealth() <= 0) {
            throw new IllegalArgumentException("Un nemico deve avere salute: " + stats);
        }
        if (experienceReward < 0
                || aggroRange < 0
                || spawnWeight < 0
                || range < 0
                || packSize < 1
                || dropChance < 0
                || dropChance > 100
                || speed < 1) {
            throw new IllegalArgumentException(
                    "Valori non validi: experienceReward="
                            + experienceReward
                            + ", aggroRange="
                            + aggroRange
                            + ", spawnWeight="
                            + spawnWeight
                            + ", range="
                            + range
                            + ", packSize="
                            + packSize
                            + ", dropChance="
                            + dropChance);
        }
        if (boss && arena.size() <= footprint.size()) {
            throw new IllegalArgumentException(
                    "La stanza del boss deve essere più grande del boss: " + arena.size());
        }
    }

    /** Un nemico con una stanza grande il doppio della sua impronta, se è un boss. */
    public EnemyDefinition(
            Kind kind,
            Stats stats,
            int experienceReward,
            int aggroRange,
            BehaviorKey behavior,
            int spawnWeight,
            int range,
            int packSize,
            int dropChance,
            Footprint footprint,
            boolean boss,
            boolean merchant,
            int speed) {
        this(
                kind,
                stats,
                experienceReward,
                aggroRange,
                behavior,
                spawnWeight,
                range,
                packSize,
                dropChance,
                footprint,
                boss,
                merchant,
                speed,
                Footprint.square(2 * footprint.size()));
    }

    /** Un boss con la sua stanza, con un'azione per turno. */
    public EnemyDefinition(
            Kind kind,
            Stats stats,
            int experienceReward,
            int aggroRange,
            BehaviorKey behavior,
            int spawnWeight,
            int range,
            int packSize,
            int dropChance,
            Footprint footprint,
            Footprint arena) {
        this(
                kind,
                stats,
                experienceReward,
                aggroRange,
                behavior,
                spawnWeight,
                range,
                packSize,
                dropChance,
                footprint,
                true,
                false,
                1,
                arena);
    }

    /** Un nemico che non è un mercante, con un'azione per turno. */
    public EnemyDefinition(
            Kind kind,
            Stats stats,
            int experienceReward,
            int aggroRange,
            BehaviorKey behavior,
            int spawnWeight,
            int range,
            int packSize,
            int dropChance,
            Footprint footprint,
            boolean boss) {
        this(
                kind,
                stats,
                experienceReward,
                aggroRange,
                behavior,
                spawnWeight,
                range,
                packSize,
                dropChance,
                footprint,
                boss,
                false,
                1);
    }

    /** Un nemico di una cella, che non è un boss. */
    public EnemyDefinition(
            Kind kind,
            Stats stats,
            int experienceReward,
            int aggroRange,
            BehaviorKey behavior,
            int spawnWeight,
            int range,
            int packSize,
            int dropChance) {
        this(
                kind,
                stats,
                experienceReward,
                aggroRange,
                behavior,
                spawnWeight,
                range,
                packSize,
                dropChance,
                Footprint.SINGLE,
                false);
    }

    /** Un nemico che combatte solo da vicino e va da solo. */
    public EnemyDefinition(
            Kind kind,
            Stats stats,
            int experienceReward,
            int aggroRange,
            BehaviorKey behavior,
            int spawnWeight) {
        this(kind, stats, experienceReward, aggroRange, behavior, spawnWeight, 0, 1, 0);
    }

    /** Un nemico senza bottino. */
    public EnemyDefinition(
            Kind kind,
            Stats stats,
            int experienceReward,
            int aggroRange,
            BehaviorKey behavior,
            int spawnWeight,
            int range,
            int packSize) {
        this(kind, stats, experienceReward, aggroRange, behavior, spawnWeight, range, packSize, 0);
    }

    /** Un nemico che va da solo, con un colpo a distanza di portata {@code range}. */
    public EnemyDefinition(
            Kind kind,
            Stats stats,
            int experienceReward,
            int aggroRange,
            BehaviorKey behavior,
            int spawnWeight,
            int range) {
        this(kind, stats, experienceReward, aggroRange, behavior, spawnWeight, range, 1, 0);
    }
}
