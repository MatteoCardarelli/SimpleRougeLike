package io.github.matteocardarelli.simpleroguelike.generation;

/**
 * Le caratteristiche di un piano da generare.
 *
 * @param minSpawnDistance la distanza minima, in passi, tra il player e ogni nemico all'inizio
 * @param maxAttempts quanti terreni provare prima di arrendersi, se uno non basta a rispettare il
 *     resto della specifica
 * @param doorCount quante porte, nei corridoi
 * @param chestCount quanti scrigni, ognuno con un oggetto
 * @param gateCount quanti cancelli con la loro leva, ognuno davanti a una zona chiusa con uno
 *     scrigno in più; se il terreno non ha un passaggio adatto, ce ne sono meno
 * @param stairsCount 1 se il piano ha le scale per il successivo, 0 per l'ultimo piano
 */
public record FloorSpec(
        int width,
        int height,
        int enemyCount,
        int itemCount,
        int minSpawnDistance,
        int maxAttempts,
        int doorCount,
        int chestCount,
        int gateCount,
        int stairsCount) {

    public FloorSpec {
        if (width < 3 || height < 3) {
            throw new IllegalArgumentException("Piano troppo piccolo: " + width + "x" + height);
        }
        if (enemyCount < 0
                || itemCount < 0
                || minSpawnDistance < 0
                || maxAttempts < 1
                || doorCount < 0
                || chestCount < 0
                || gateCount < 0
                || stairsCount < 0
                || stairsCount > 1) {
            throw new IllegalArgumentException(
                    "Valori non validi: enemyCount="
                            + enemyCount
                            + ", itemCount="
                            + itemCount
                            + ", minSpawnDistance="
                            + minSpawnDistance
                            + ", maxAttempts="
                            + maxAttempts
                            + ", doors="
                            + doorCount
                            + ", chests="
                            + chestCount
                            + ", gates="
                            + gateCount
                            + ", stairs="
                            + stairsCount);
        }
    }

    /** Un piano senza oggetti fissi. */
    public FloorSpec(
            int width,
            int height,
            int enemyCount,
            int itemCount,
            int minSpawnDistance,
            int maxAttempts) {
        this(width, height, enemyCount, itemCount, minSpawnDistance, maxAttempts, 0, 0, 0, 0);
    }
}
