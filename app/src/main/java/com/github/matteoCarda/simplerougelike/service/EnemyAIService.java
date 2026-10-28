package com.github.matteoCarda.simplerougelike.service;

import com.github.matteoCarda.simplerougelike.model.GameMap;
import com.github.matteoCarda.simplerougelike.model.entity.Enemy;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import java.util.ArrayList;
import java.util.Random;
import squidpony.squidai.DijkstraMap;
import squidpony.squidmath.Coord;

/**
 * Gestisce l'Intelligenza Artificiale (IA) dei nemici. Determina le azioni dei nemici durante il
 * loro turno.
 */
public class EnemyAIService {
    private final CombatService combatService;
    private final CharacterService characterService;
    private final Random random = new Random();

    /**
     * Costruttore del servizio di IA.
     *
     * @param combatService Servizio per la gestione del combattimento.
     * @param characterService Servizio per la gestione dello stato dei personaggi.
     */
    public EnemyAIService(CombatService combatService, CharacterService characterService) {
        this.combatService = combatService;
        this.characterService = characterService;
    }

    /**
     * Esegue il turno di un nemico, determinando l'azione da compiere. La logica è la seguente: 1.
     * Se adiacente al giocatore, attacca. 2. Se nel raggio di inseguimento, si muove verso il
     * giocatore usando Dijkstra. 3. Altrimenti, si muove casualmente.
     *
     * @param enemy Il nemico che deve agire.
     * @param gameMap La mappa di gioco attuale.
     * @param player Il giocatore, target principale.
     * @param dijkstraMap La mappa di Dijkstra pre-calcolata con il giocatore come target.
     */
    public void performTurn(Enemy enemy, GameMap gameMap, Player player, DijkstraMap dijkstraMap) {
        // Distanza di Manhattan, un calcolo veloce per la prossimità.
        int distance =
                Math.abs(enemy.getX() - player.getX()) + Math.abs(enemy.getY() - player.getY());

        // 1. Attacco in mischia
        if (distance <= 1) {
            combatService.performAttack(enemy, player);
            return; // Azione del turno completata.
        }

        // 2. Inseguimento
        if (distance <= 8) { // Raggio di "aggro"
            // Calcola il prossimo passo verso il giocatore.
            ArrayList<Coord> path =
                    dijkstraMap.findPath(1, null, null, Coord.get(enemy.getX(), enemy.getY()));
            if (!path.isEmpty()) {
                Coord nextStep = path.get(0);
                if (isMoveValid(nextStep.x, nextStep.y, gameMap, player)) {
                    enemy.setPosition(nextStep.x, nextStep.y);
                    return; // Azione del turno completata.
                }
            }
        }

        // 3. Movimento casuale (fallback)
        int dirX = random.nextInt(3) - 1; // da -1 a 1
        int dirY = random.nextInt(3) - 1; // da -1 a 1
        int targetX = enemy.getX() + dirX;
        int targetY = enemy.getY() + dirY;

        if (isMoveValid(targetX, targetY, gameMap, player)) {
            enemy.setPosition(targetX, targetY);
        } // Altrimenti, il nemico sta fermo.
    }

    /**
     * Genera una mappa di costi per l'algoritmo di Dijkstra a partire dalla mappa di gioco.
     *
     * @param gameMap La mappa di gioco.
     * @return una DijkstraMap pronta per il pathfinding.
     */
    public DijkstraMap getDijkstraMap(GameMap gameMap) {
        // I muri ('#') hanno costo infinito, il pavimento ('.') ha costo base.
        char[][] costMap = new char[gameMap.getWidth()][gameMap.getHeight()];
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                costMap[x][y] = gameMap.getTile(x, y).isWalkable() ? '.' : '#';
            }
        }
        // 'M' abilita il movimento diagonale (misura di distanza di Chebyshev).
        return new DijkstraMap(costMap, 'M');
    }

    /**
     * Controlla se una mossa verso (x,y) è valida. Una mossa non è valida se esce dalla mappa,
     * finisce su un muro, o su una cella già occupata dal giocatore o da un altro nemico.
     */
    private boolean isMoveValid(int x, int y, GameMap gameMap, Player player) {
        if (x < 0 || x >= gameMap.getWidth() || y < 0 || y >= gameMap.getHeight()) return false;
        if (!gameMap.getTile(x, y).isWalkable()) return false;
        if (player.getX() == x && player.getY() == y) return false;

        for (Enemy otherEnemy : gameMap.getEnemies()) {
            if (characterService.isAlive(otherEnemy)
                    && otherEnemy.getX() == x
                    && otherEnemy.getY() == y) {
                return false; // Cella occupata da un altro nemico.
            }
        }
        return true;
    }
}
