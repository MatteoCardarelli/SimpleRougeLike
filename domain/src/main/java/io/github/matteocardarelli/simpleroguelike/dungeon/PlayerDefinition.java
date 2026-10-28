package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.Stats;

/**
 * I valori con cui parte il player.
 *
 * @param experienceToFirstLevel l'esperienza che serve per arrivare al livello 2
 * @param inventoryCapacity quanti oggetti può portare
 */
public record PlayerDefinition(Stats stats, int experienceToFirstLevel, int inventoryCapacity) {

    public PlayerDefinition {
        if (stats.maxHealth() <= 0 || experienceToFirstLevel <= 0 || inventoryCapacity <= 0) {
            throw new IllegalArgumentException(
                    "Valori non validi: stats="
                            + stats
                            + ", experienceToFirstLevel="
                            + experienceToFirstLevel
                            + ", inventoryCapacity="
                            + inventoryCapacity);
        }
    }
}
