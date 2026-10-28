package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.LevelingPolicy;
import io.github.matteocardarelli.simpleroguelike.event.ExperienceGained;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.LeveledUp;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import java.util.ArrayList;
import java.util.List;

/**
 * La progressione del player. A ogni livello salute massima e attacco crescono di un valore fisso,
 * e l'esperienza per il livello successivo cresce di {@code thresholdGrowthPercent} per cento: con
 * 150 la soglia aumenta del 50%. Solo interi: il risultato si arrotonda per difetto e cresce sempre
 * di almeno 1.
 */
public final class LevelingRules implements LevelingPolicy {

    private final Stats bonusPerLevel;
    private final int thresholdGrowthPercent;

    public LevelingRules(int healthPerLevel, int attackPerLevel, int thresholdGrowthPercent) {
        if (thresholdGrowthPercent < 100) {
            throw new IllegalArgumentException(
                    "La soglia non può diminuire: " + thresholdGrowthPercent + "%");
        }
        this.bonusPerLevel = new Stats(healthPerLevel, attackPerLevel);
        this.thresholdGrowthPercent = thresholdGrowthPercent;
    }

    @Override
    public Stats bonusPerLevel() {
        return bonusPerLevel;
    }

    @Override
    public int nextThreshold(int current) {
        long grown = (long) current * thresholdGrowthPercent / 100;
        return Math.toIntExact(Math.max(current + 1L, grown));
    }

    /**
     * Dà esperienza al player.
     *
     * @return {@code ExperienceGained} e un {@code LeveledUp} per ogni livello guadagnato; vuoto se
     *     {@code amount} è 0 o il player è morto
     */
    public List<GameEvent> grantExperience(Floor floor, int amount) {
        if (amount == 0 || !floor.player().isAlive()) {
            return List.of();
        }
        int levelBefore = floor.player().level();
        int gained = floor.grantExperience(amount, this);
        Position at = floor.positionOf(floor.playerId());
        List<GameEvent> events = new ArrayList<>();
        events.add(new ExperienceGained(at, amount));
        for (int level = levelBefore + 1; level <= levelBefore + gained; level++) {
            events.add(new LeveledUp(at, level));
        }
        return List.copyOf(events);
    }
}
