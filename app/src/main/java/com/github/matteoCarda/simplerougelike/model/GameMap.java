package com.github.matteoCarda.simplerougelike.model;

import com.github.matteoCarda.simplerougelike.model.entity.Enemy;
import com.github.matteoCarda.simplerougelike.model.entity.Item;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.model.tile.Tile;
import java.util.List;

/**
 * Contenitore per lo stato completo di un livello di gioco. Aggrega la griglia delle tile e le
 * liste di entità dinamiche.
 */
public class GameMap {

    // Griglia statica del livello.
    private final Tile[][] grid;

    // Riferimenti diretti alle entità.
    private final Player player;
    private final List<Enemy> enemies;
    private final List<Item> items;

    // Dimensioni della mappa, cachate per efficienza.
    private final int width;
    private final int height;

    /**
     * Costruttore principale.
     *
     * @param grid La griglia di Tile generata.
     * @param player L'oggetto Player.
     * @param enemies La lista dei nemici.
     * @param items La lista degli oggetti.
     */
    public GameMap(Tile[][] grid, Player player, List<Enemy> enemies, List<Item> items) {
        this.grid = grid;
        this.player = player;
        this.enemies = enemies;
        this.items = items;
        this.width = grid.length;
        this.height = (grid.length > 0) ? grid[0].length : 0;
    }

    // --- GETTERS ---

    /**
     * Ritorna la Tile a una data coordinata, con controllo dei limiti.
     *
     * @return La Tile, o null se le coordinate sono fuori mappa.
     */
    public Tile getTile(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return grid[x][y];
        }
        return null;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Item> getItems() {
        return items;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
