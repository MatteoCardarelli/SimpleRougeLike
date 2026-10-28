package io.github.matteocardarelli.simpleroguelike.generation;

/**
 * Le caratteristiche di un piano da generare.
 *
 * @param minSpawnDistance la distanza minima, in passi, tra il player e ogni nemico all'inizio
 * @param maxAttempts quanti terreni provare prima di arrendersi, se uno non basta a rispettare il
 *     resto della specifica
 */
public record FloorSpec(
        int width,
        int height,
        int enemyCount,
        int itemCount,
        int minSpawnDistance,
        int maxAttempts) {

    public FloorSpec {
        if (width < 3 || height < 3) {
            throw new IllegalArgumentException("Piano troppo piccolo: " + width + "x" + height);
        }
        if (enemyCount < 0 || itemCount < 0 || minSpawnDistance < 0 || maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "Valori non validi: enemyCount="
                            + enemyCount
                            + ", itemCount="
                            + itemCount
                            + ", minSpawnDistance="
                            + minSpawnDistance
                            + ", maxAttempts="
                            + maxAttempts);
        }
    }
}
