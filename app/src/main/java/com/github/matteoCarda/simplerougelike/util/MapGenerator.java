package com.github.matteoCarda.simplerougelike.util;

import com.github.matteoCarda.simplerougelike.model.GameMap;
import com.github.matteoCarda.simplerougelike.model.entity.Enemy;
import com.github.matteoCarda.simplerougelike.model.entity.Item;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.model.entity.PotionItem;
import com.github.matteoCarda.simplerougelike.model.tile.FloorTile;
import com.github.matteoCarda.simplerougelike.model.tile.Tile;
import com.github.matteoCarda.simplerougelike.model.tile.WallTile;
import java.util.ArrayList;
import java.util.List;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * Classe di utilità per la generazione procedurale della mappa di gioco. Utilizza la libreria
 * SquidLib per creare la struttura del dungeon.
 */
public class MapGenerator {
    private final RNG rng;

    /**
     * Costruttore che inizializza il generatore di numeri casuali (RNG). L'RNG è fondamentale per
     * garantire che ogni mappa sia diversa.
     */
    public MapGenerator() {
        this.rng = new RNG();
    }

    /**
     * Genera una mappa di gioco completa con stanze, corridoi, nemici e oggetti.
     *
     * @param width Larghezza della mappa da generare.
     * @param height Altezza della mappa da generare.
     * @param enemyCount Numero di nemici da posizionare casualmente.
     * @param itemCount Numero di oggetti da posizionare casualmente.
     * @return Un oggetto GameMap pronto per essere utilizzato nel gioco.
     * @throws IllegalStateException se la generazione del dungeon non produce spazi calpestabili.
     */
    public GameMap generateMap(int width, int height, int enemyCount, int itemCount) {
        // 1. USARE SQUIDLIB PER GENERARE LA STRUTTURA DEL DUNGEON
        DungeonGenerator dungeonGenerator = new DungeonGenerator(width, height, rng);
        char[][] generatedGrid = dungeonGenerator.generate();

        // 2. CONVERTIRE LA GRIGLIA DI CARATTERI IN OGGETTI TILE
        Tile[][] grid = new Tile[width][height];
        // MODIFICA: Usiamo DungeonUtility per ottenere le coordinate, è più pulito.
        ArrayList<Coord> floorCoords = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (generatedGrid[x][y] == '#') {
                    grid[x][y] = new WallTile(x, y);
                } else {
                    grid[x][y] = new FloorTile(x, y);
                    floorCoords.add(Coord.get(x, y));
                }
            }
        }

        // CONTROLLO DI SICUREZZA: assicurati che ci siano abbastanza spazi liberi.
        if (floorCoords.size() < enemyCount + itemCount + 1) {
            throw new IllegalStateException(
                    "Mappa troppo piccola o dungeon non valido: non ci sono abbastanza caselle  libere per posizionare tutto.");
        }

        // 3. NUOVA LOGICA DI POSIZIONAMENTO SPARSO

        // POSIZIONA IL GIOCATORE
        // Scegli un indice casuale, prendi la coordinata, e poi rimuovila dalla lista.
        Coord playerCoord = floorCoords.remove(rng.nextInt(floorCoords.size()));
        Player player = new Player(playerCoord.x, playerCoord.y);

        // POSIZIONA I NEMICI
        List<Enemy> enemies = new ArrayList<>();
        for (int i = 0; i < enemyCount; i++) {
            // Ripeti il processo: scegli un indice casuale, prendi la coordinata e rimuovila.
            Coord enemyCoord = floorCoords.remove(rng.nextInt(floorCoords.size()));
            enemies.add(new Enemy(enemyCoord.x, enemyCoord.y));
        }

        // POSIZIONA GLI OGGETTI
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            // Stesso processo per gli oggetti.
            Coord itemCoord = floorCoords.remove(rng.nextInt(floorCoords.size()));
            items.add(new PotionItem(itemCoord.x, itemCoord.y));
        }

        // 4. CREARE E RESTITUIRE L'OGGETTO GAMEMAP COMPLETO
        return new GameMap(grid, player, enemies, items);
    }
}
