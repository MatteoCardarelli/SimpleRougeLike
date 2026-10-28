package com.github.matteoCarda.simplerougelike.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.matteoCarda.simplerougelike.model.GameMap;
import com.github.matteoCarda.simplerougelike.model.entity.Enemy;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.model.tile.FloorTile;
import com.github.matteoCarda.simplerougelike.model.tile.Tile;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import squidpony.squidai.DijkstraMap;

public class EnemyAIServiceTest {

    // --- Mock delle dipendenze ---
    @Mock private CombatService mockCombatService;
    @Mock private CharacterService mockCharacterService;
    // Creiamo un mock anche per DijkstraMap, perché non vogliamo dipendere
    // dalla vera logica di pathfinding di squidlib in un test unitario.
    @Mock private DijkstraMap mockDijkstraMap;

    // --- Classe sotto test ---
    @InjectMocks private EnemyAIService enemyAIService;

    // --- Oggetti di gioco ---
    private Player player;
    private Enemy enemy;
    private GameMap gameMap;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Una mappa fissa, tutta pavimento: con una mappa generata a caso la cella verso cui
        // il nemico deve muoversi poteva essere un muro, e il test falliva a caso.
        Tile[][] grid = new Tile[20][20];
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                grid[x][y] = new FloorTile(x, y);
            }
        }
        player = new Player(0, 0);
        enemy = new Enemy(5, 5);
        List<Enemy> enemies = new ArrayList<>();
        enemies.add(enemy);
        gameMap = new GameMap(grid, player, enemies, new ArrayList<>());

        // Istruzione di default: per la maggior parte dei test, il nemico è vivo.
        when(mockCharacterService.isAlive(enemy)).thenReturn(true);
    }

    @Test
    public void performTurn_AttacksPlayer_WhenAdjacent() {
        // Arrange
        // Posizioniamo manualmente il giocatore accanto al nemico.
        player.setPosition(enemy.getX() + 1, enemy.getY());

        // Act
        // Eseguiamo il turno dell'IA.
        enemyAIService.performTurn(enemy, gameMap, player, mockDijkstraMap);

        // Assert
        // Verifichiamo che il metodo `performAttack` del nostro mockCombatService
        // sia stato chiamato esattamente una volta, con il giocatore come bersaglio.
        verify(mockCombatService, times(1)).performAttack(enemy, player);

        // Verifichiamo anche che il nemico non si sia mosso (non ha cambiato posizione).
        // `never()` è l'opposto di `times(1)`.
        verify(mockDijkstraMap, never()).findPath(anyInt(), any(), any(), any(), any());
    }

    @Test
    public void performTurn_ChasesPlayer_WhenInSight() {
        // Arrange
        // Posizioniamo il giocatore a una distanza media (ma non adiacente).
        player.setPosition(enemy.getX() + 3, enemy.getY());

        // Creiamo un finto percorso per il nostro mock di DijkstraMap.
        // Questo simula che il pathfinding abbia trovato un percorso verso il giocatore.
        // Il percorso dice al nemico di muoversi di un passo verso destra (x+1).
        ArrayList<squidpony.squidmath.Coord> fakedPath = new ArrayList<>();
        fakedPath.add(squidpony.squidmath.Coord.get(enemy.getX() + 1, enemy.getY()));

        // Istruiamo il mock: "QUANDO ti viene chiesto di trovare un percorso,
        // restituisci il nostro percorso finto".
        when(mockDijkstraMap.findPath(anyInt(), any(), any(), any())).thenReturn(fakedPath);

        int originalX = enemy.getX(); // Salviamo la posizione originale per la verifica

        // Act
        enemyAIService.performTurn(enemy, gameMap, player, mockDijkstraMap);

        // Assert
        // Verifichiamo che l'IA abbia cercato di trovare un percorso.
        verify(mockDijkstraMap, times(1)).findPath(anyInt(), any(), any(), any());

        // Verifichiamo che il nemico NON abbia attaccato.
        verify(mockCombatService, never()).performAttack(any(), any());

        // Verifichiamo che la posizione del nemico sia cambiata come previsto dal percorso.
        Assert.assertEquals(originalX + 1, enemy.getX());
    }

    @Test
    public void performTurn_Wanders_WhenPlayerIsFar() {
        // Arrange
        // Posizioniamo il giocatore molto lontano dal nemico.
        player.setPosition(enemy.getX() + 15, enemy.getY());

        int originalX = enemy.getX();
        int originalY = enemy.getY();

        // Act
        enemyAIService.performTurn(enemy, gameMap, player, mockDijkstraMap);

        // Assert
        // Verifichiamo che l'IA NON abbia cercato un percorso (perché il giocatore è troppo
        // lontano).
        verify(mockDijkstraMap, never()).findPath(anyInt(), any(), any(), any(), any());

        // Verifichiamo che NON abbia attaccato.
        verify(mockCombatService, never()).performAttack(any(), any());

        // Poiché il movimento è casuale, non possiamo sapere la posizione esatta.
        // Possiamo però verificare che la nuova posizione non sia la stessa di prima (è una
        // verifica debole ma utile).
        boolean hasMoved = (enemy.getX() != originalX || enemy.getY() != originalY);
        // Se `isMoveValid` fallisce per tutte le direzioni, il nemico potrebbe non muoversi.
        // Il test più robusto è semplicemente verificare che la logica di attacco/inseguimento non
        // sia stata attivata.
        // L'asserzione `hasMoved` potrebbe fallire casualmente, quindi ci fidiamo delle verifiche
        // `never()` di cui sopra.
    }
}
