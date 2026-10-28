package com.github.simplerougelike;

import static org.junit.Assert.*;

import com.github.matteoCarda.simplerougelike.model.GameMap;
import com.github.matteoCarda.simplerougelike.model.entity.Enemy;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.service.CharacterService;
import com.github.matteoCarda.simplerougelike.service.CombatService;
import com.github.matteoCarda.simplerougelike.service.EnemyAIService;
import com.github.matteoCarda.simplerougelike.service.PlayerService;
import com.github.matteoCarda.simplerougelike.util.MapGenerator;
import org.junit.Assert;
import org.junit.Test;
import squidpony.squidai.DijkstraMap;

public class GameIntegrationTest {

    @Test
    public void enemyChasesAndAttacksPlayer_IntegrationTest() {
        // Arrange: Creiamo le istanze REALI di tutti i servizi
        CharacterService characterService = new CharacterService();
        PlayerService playerService = new PlayerService();
        CombatService combatService =
                new CombatService(characterService, playerService); // Inietta il vero servizio
        EnemyAIService enemyAIService = new EnemyAIService(combatService, characterService);

        // Creiamo una mappa con un giocatore e un nemico vicini
        MapGenerator mapGenerator = new MapGenerator();
        GameMap gameMap = mapGenerator.generateMap(10, 10, 1, 0);
        Player player = gameMap.getPlayer();
        Enemy enemy = gameMap.getEnemies().get(0);

        player.setPosition(5, 5);
        enemy.setPosition(5, 6); // Posiziona il nemico accanto al giocatore

        double playerInitialHealth = player.getHealth();

        // Act: Simuliamo il turno dell'IA
        DijkstraMap dijkstraMap =
                enemyAIService.getDijkstraMap(gameMap); // Calcoliamo la mappa dei percorsi
        dijkstraMap.setGoal(player.getX(), player.getY());
        enemyAIService.performTurn(enemy, gameMap, player, dijkstraMap);

        // Assert: Verifichiamo il risultato finale
        // Ci aspettiamo che la salute del giocatore sia diminuita
        assertTrue(
                "La salute del giocatore dovrebbe essere diminuita dopo l'attacco",
                player.getHealth() < playerInitialHealth);
        Assert.assertEquals(
                playerInitialHealth - Math.round(enemy.getAttackPower()), player.getHealth(), 0.0);
    }
}
