package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.Stats;

/**
 * I valori della progressione del player: cosa si guadagna con un livello e quanto costa il
 * successivo.
 */
public interface LevelingPolicy {

    /** L'aumento delle statistiche a ogni livello. */
    Stats bonusPerLevel();

    /**
     * L'esperienza che serve per il prossimo livello, sempre maggiore di {@code current}.
     *
     * @param current l'esperienza che serviva per il livello appena raggiunto
     */
    int nextThreshold(int current);
}
