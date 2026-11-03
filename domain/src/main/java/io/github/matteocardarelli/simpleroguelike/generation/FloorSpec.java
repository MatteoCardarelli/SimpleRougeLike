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
 * @param lockedDoorCount quante delle porte sono chiuse a chiave; per ognuna c'è una chiave che si
 *     raggiunge senza passare dalle porte chiuse a chiave
 * @param bossCount 1 per il piano del boss: un'arena con il boss, la sua porta e la sua chiave
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
        int stairsCount,
        int lockedDoorCount,
        int bossCount) {

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
                || stairsCount > 1
                || lockedDoorCount < 0
                || lockedDoorCount > doorCount
                || bossCount < 0
                || bossCount > 1) {
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
                            + stairsCount
                            + ", lockedDoors="
                            + lockedDoorCount
                            + ", boss="
                            + bossCount);
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
        this(width, height, enemyCount, itemCount, minSpawnDistance, maxAttempts, 0, 0, 0, 0, 0, 0);
    }

    /** Un piano senza porte chiuse a chiave. */
    public FloorSpec(
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
        this(
                width,
                height,
                enemyCount,
                itemCount,
                minSpawnDistance,
                maxAttempts,
                doorCount,
                chestCount,
                gateCount,
                stairsCount,
                0,
                0);
    }

    /** Un piano senza boss. */
    public FloorSpec(
            int width,
            int height,
            int enemyCount,
            int itemCount,
            int minSpawnDistance,
            int maxAttempts,
            int doorCount,
            int chestCount,
            int gateCount,
            int stairsCount,
            int lockedDoorCount) {
        this(
                width,
                height,
                enemyCount,
                itemCount,
                minSpawnDistance,
                maxAttempts,
                doorCount,
                chestCount,
                gateCount,
                stairsCount,
                lockedDoorCount,
                0);
    }
}
