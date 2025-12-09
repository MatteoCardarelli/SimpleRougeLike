package com.github.matteoCarda.simplerougelike.controller;

import com.github.matteoCarda.simplerougelike.model.GameMap;
import com.github.matteoCarda.simplerougelike.model.entity.Character;
import com.github.matteoCarda.simplerougelike.model.entity.Enemy;
import com.github.matteoCarda.simplerougelike.model.entity.Item;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.service.CharacterService;
import com.github.matteoCarda.simplerougelike.service.CombatService;
import com.github.matteoCarda.simplerougelike.service.EnemyAIService;
import com.github.matteoCarda.simplerougelike.service.ItemService;
import com.github.matteoCarda.simplerougelike.service.PlayerService;
import com.github.matteoCarda.simplerougelike.util.MapGenerator;

import java.util.ArrayList;
import java.util.List;

import squidpony.squidai.DijkstraMap;
import squidpony.squidgrid.FOV;

/**
 * Gestore centrale dello stato e della logica di gioco (Singleton).
 * Fa da ponte tra l'input dell'utente, i servizi di logica e lo stato del modello.
 */
public class GameController {
    private static GameController instance;

    // Stato del gioco
    private GameMap gameMap;
    private Player player;
    private GameState gameState;
    private PlayerService playerService;


    // Servizi delegati alla logica di business
    private final MapGenerator mapGenerator;
    private final CombatService combatService;
    private final CharacterService characterService;
    private final ItemService itemService;
    private final EnemyAIService enemyAIService;

    // Gestione del Field of View (FOV)
    private static final int PLAYER_VISION_RADIUS = 8;
    private FOV fov;
    private GameUpdateListener gameUpdateListener;
    private double[][] playerFov; // Mappa di visibilità per il rendering
    private double[][] resistanceMap; // Mappa statica della "resistenza" alla luce (1.0 = muro)

    public enum GameState { PLAYING, GAME_OVER, PAUSED, VICTORY }
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    /**
     * Accesso all'istanza unica del GameController.
     */
    public static synchronized GameController getInstance() {
        if (instance == null) {
            instance = new GameController();
        }
        return instance;
    }

    /**
     * Costruttore privato (Singleton). Inizializza tutti i servizi.
     */
    private GameController() {
        this.mapGenerator = new MapGenerator();
        this.characterService = new CharacterService();
        this.playerService = new PlayerService();
        this.combatService = new CombatService(this.characterService, this.playerService);
        this.itemService = new ItemService(this.characterService);
        this.enemyAIService = new EnemyAIService(this.combatService, this.characterService);
        this.gameState = GameState.GAME_OVER; // Il gioco parte in attesa di una nuova partita.
    }

    // --- GETTERS PER LA VIEW ---
    public GameMap getGameMap() { return gameMap; }
    public GameState getGameState() { return gameState; }
    public double[][] getPlayerFov() { return playerFov; }
    public boolean isAlive(Character character) { return characterService.isAlive(character);}
    public void setGameUpdateListener(GameUpdateListener listener) {
        this.gameUpdateListener = listener;
    }

    /**
     * Inizializza una nuova partita, generando mappa e stato del FOV.
     */
    public void startNewGame(int width, int height, int enemyCount, int itemCount) {
        this.gameMap = mapGenerator.generateMap(width, height, enemyCount, itemCount);
        this.player = gameMap.getPlayer();
        this.gameState = GameState.PLAYING;
        // Pre-calcola la mappa di resistenza per il FOV.
        this.fov = new FOV();
        this.playerFov = new double[width][height];
        this.resistanceMap = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                resistanceMap[x][y] = gameMap.getTile(x, y).isWalkable() ? 0.0 : 1.0;
            }
        }
        calculateFov();
        if (gameUpdateListener != null) {
            gameUpdateListener.onGameUpdated();
        }
    }


    /**
     * Gestisce un intero turno di gioco a partire dall'azione del giocatore.
     * @param direction L'input di movimento del giocatore.
     */
    public void handlePlayerTurn(Direction direction) {
        if (gameState != GameState.PLAYING) return;

        handlePlayerAction(direction);
        processEnemyTurns();
        checkEndGameConditions();
        if (gameUpdateListener != null) {
            gameUpdateListener.onGameUpdated();
        }
    }

    /**
     * Esegue l'azione del giocatore e aggiorna il FOV.
     */
    private void handlePlayerAction(Direction direction) {
        movePlayer(direction);
        if (characterService.isAlive(player)) {
            calculateFov();
        }
    }

    /**
     * Esegue il turno per ogni nemico vivo, usando una mappa Dijkstra per il pathfinding.
     */
    private void processEnemyTurns() {
        // Crea una mappa di costo per Dijkstra, dove l'obiettivo è il giocatore.
        DijkstraMap dijkstraMapToPlayer = enemyAIService.getDijkstraMap(gameMap);
        dijkstraMapToPlayer.setGoal(player.getX(), player.getY());
        dijkstraMapToPlayer.scan(null);

        // Fa agire ogni nemico e raccoglie i morti per la rimozione.
        List<Enemy> enemiesToRemove = new ArrayList<>();
        for (Enemy enemy : new ArrayList<>(gameMap.getEnemies())) { // Itera su una copia per evitare ConcurrentModificationException
            if (characterService.isAlive(enemy)) {
                enemyAIService.performTurn(enemy, gameMap, player, dijkstraMapToPlayer);
            } else {
                enemiesToRemove.add(enemy);
            }
        }
        gameMap.getEnemies().removeAll(enemiesToRemove);
    }

    /**
     * Gestisce il movimento del giocatore e le interazioni (attacco, raccolta oggetti).
     */
    private void movePlayer(Direction direction) {
        int targetX = player.getX();
        int targetY = player.getY();

        switch (direction) {
            case UP:    targetY--; break;
            case DOWN:  targetY++; break;
            case LEFT:  targetX--; break;
            case RIGHT: targetX++; break;
        }

        // Controlla se la mossa è valida.
        if (targetX < 0 || targetX >= gameMap.getWidth() || targetY < 0 || targetY >= gameMap.getHeight() || !gameMap.getTile(targetX, targetY).isWalkable()) {
            return; // Urta un muro o esce dalla mappa.
        }

        // Controlla se c'è un nemico.
        Enemy targetEnemy = getEnemyAt(targetX, targetY);
        if (targetEnemy != null) {
            combatService.performAttack(player, targetEnemy);
            return; // Attacca invece di muoversi.
        }

        // Controlla se c'è un oggetto.
        Item targetItem = getItemAt(targetX, targetY);
        if (targetItem != null) {
            itemService.onPickup(targetItem, player);
            gameMap.getItems().remove(targetItem);
        }

        // Se la cella è libera, si muove.
        player.setPosition(targetX, targetY);
    }

    /**
     * Cerca un nemico vivo in una data coordinata.
     */
    private Enemy getEnemyAt(int x, int y) {
        for (Enemy enemy : gameMap.getEnemies()) {
            if (characterService.isAlive(enemy) && enemy.getX() == x && enemy.getY() == y) {
                return enemy;
            }
        }
        return null;
    }

    public void useItemFromInventory(Item item) {
        Player player = getGameMap().getPlayer();

        // Passiamo anche l'istanza di itemService al metodo dell'oggetto
        boolean success = itemService.useItem(player, item);

        if (success) {
            processEnemyTurns();
            checkEndGameConditions();
        }

        // NUOVA LOGICA: Notifica il listener che lo stato è cambiato.
        if (gameUpdateListener != null) {
            gameUpdateListener.onGameUpdated();
        }
    }


    /**
     * Cerca un oggetto in una data coordinata.
     */
    private Item getItemAt(int x, int y) {
        for (Item item : gameMap.getItems()) {
            if (item.getX() == x && item.getY() == y) {
                return item;
            }
        }
        return null;
    }

    /**
     * Controlla lo stato di fine partita.
     */
    private void checkEndGameConditions() {
        if (!characterService.isAlive(player)) {
            gameState = GameState.GAME_OVER;
            System.out.println("Game Over! Sei stato sconfitto.");
        }
        // TODO: Aggiungere condizione di vittoria (es. nemici sconfitti, oggetto trovato, etc.)
    }

    /**
     * Ricalcola il campo visivo del giocatore.
     */
    private void calculateFov() {
        FOV.reuseFOV(resistanceMap, playerFov, player.getX(), player.getY(), PLAYER_VISION_RADIUS);
    }

    /**
     * Gestisce la richiesta di aprire l'inventario.
     * È responsabilità del controller decidere se mettere in pausa il gioco.
     */
    public void requestOpenInventory() {
        // Il controller ha l'autorità di cambiare il proprio stato.
        setGameState(GameState.PAUSED);
        // In futuro, potremmo anche fare altro qui, come fermare i timer, ecc.
    }

    /**
     * Gestisce la richiesta di chiudere l'inventario, spesso dopo un'azione.
     */
    public void requestCloseInventory() {
        setGameState(GameState.PLAYING);
        // Potremmo anche forzare un aggiornamento della UI qui, se necessario.
        if (gameUpdateListener != null) {
            gameUpdateListener.onGameUpdated();
        }
    }

    private void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

}
